package com.example.MyProject.Services;

import com.example.MyProject.Address.dto.AddressResponse;
import com.example.MyProject.Configuration.JwtUtil;
import com.example.MyProject.Enum.Role;
import com.example.MyProject.Exception.ResourceNotFoundException;
import com.example.MyProject.Exception.TooManyRequestsException;
import com.example.MyProject.Models.Address;
import com.example.MyProject.Models.User;
import com.example.MyProject.Repository.AddressRepository;
import com.example.MyProject.Repository.OrderRepository;
import com.example.MyProject.Repository.UserRepository;
import com.example.MyProject.Repository.WishlistRepository;
import com.example.MyProject.User.Dto.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.example.MyProject.Models.AttemptLimiter;
import java.time.Duration;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    // NEW: login attempt limits. Wrong passwords are counted per email AND per IP address,
    // inside a 15 minute window. After the limit, even the right password is refused until it ends.
    private static final int MAX_FAILURES_PER_EMAIL = 5;
    private static final int MAX_FAILURES_PER_IP = 20;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final AddressRepository addressRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;
    private final AttemptLimiter limiter;

    // Compared against when the email does not exist, so a wrong email takes as long as a
    // wrong password. Otherwise response time reveals which emails are registered.
    private String dummyHash;

    @PostConstruct
    void init() {
        dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    // CHANGED: NOT_SUPPORTED. With the class-level @Transactional, a duplicate-email error inside
    // saveAndFlush marked the whole transaction as failed; the catch below then returned the friendly
    // message, but the commit that followed failed and the customer saw a generic 500 instead.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApiResponse<UserResponseDTO> registerUser(RegisterUserDto dto) {
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        String userName = dto.getUserName().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return failure("messages.email.exists");
        }
        if (userRepository.existsByUserName(userName)) {
            return failure("messages.username.exists");
        }

        // Never trust a client-supplied role: self-registration is always USER.
        User user = User.builder()
                .userName(userName)
                .email(email)
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.USER)
                .build();

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two simultaneous registrations with the same email/username.
            return failure("messages.email.exists");
        }

        String successMsg = messageSource.getMessage("messages.user.register.success", null, LocaleContextHolder.getLocale());
        return ApiResponse.<UserResponseDTO>builder()
                .success(true)
                .data(mapToResponse(user))
                .message(successMsg)
                .build();
    }

    // CHANGED: takes the caller's IP address (for the limit).
    public ApiResponse<LoginResponseDTO> loginUser(LoginUserDto dto, String clientIp) {
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        String emailKey = "login:email:" + email;
        String ipKey = "login:ip:" + (clientIp == null ? "unknown" : clientIp);

        // Checked BEFORE the password, so a blocked caller learns nothing about whether it was right.
        if (limiter.isLimited(emailKey, MAX_FAILURES_PER_EMAIL) || limiter.isLimited(ipKey, MAX_FAILURES_PER_IP)) {
            long seconds = Math.max(limiter.secondsLeft(emailKey), limiter.secondsLeft(ipKey));
            long minutes = Math.max(1, (seconds + 59) / 60);
            throw new TooManyRequestsException(
                    "Too many failed sign-in attempts. Please try again in " + minutes
                            + (minutes == 1 ? " minute" : " minutes") + ", or reset your password.");
        }

        User user = userRepository.findFirstByEmailIgnoreCase(dto.getEmail().trim()).orElse(null);

        boolean ok;
        if (user == null) {
            passwordEncoder.matches(dto.getPassword(), dummyHash);   // equalise timing
            ok = false;
        } else {
            ok = passwordEncoder.matches(dto.getPassword(), user.getPassword());
        }

        if (!ok) {
            limiter.hit(emailKey, LOGIN_WINDOW);
            limiter.hit(ipKey, LOGIN_WINDOW);
            String errorMsg = messageSource.getMessage("messages.invalid.credentials", null, LocaleContextHolder.getLocale());
            return ApiResponse.<LoginResponseDTO>builder().success(false).message(errorMsg).build();
        }

        // A correct login wipes the failure count for this email.
        limiter.reset(emailKey);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        LoginResponseDTO response = LoginResponseDTO.builder()
                .token(token)
                .userName(user.getUserName())
                .role(user.getRole().name())
                .build();

        String successMsg = messageSource.getMessage("messages.user.login.success", null, LocaleContextHolder.getLocale());
        return ApiResponse.<LoginResponseDTO>builder()
                .success(true)
                .data(response)
                .message(successMsg)
                .build();
    }

    private ApiResponse<UserResponseDTO> failure(String messageKey) {
        String msg = messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
        return ApiResponse.<UserResponseDTO>builder().success(false).message(msg).build();
    }

    private UserResponseDTO mapToResponse(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        String email = (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getUserProfile() {
        User user = getAuthenticatedUser();
        long orderCount = orderRepository.countByUser_UserId(user.getUserId());
        long wishlistCount = wishlistRepository.countByUser_UserId(user.getUserId());
        long addressCount = addressRepository.countByUser_UserId(user.getUserId());

        AddressResponse defaultAddrResponse = addressRepository.findByUser_UserIdAndIsDefaultTrue(user.getUserId())
                .map(this::mapAddressToResponse)
                .orElse(null);

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole())
                .totalOrders(orderCount)
                .totalWishlist(wishlistCount)
                .totalAddresses((int) addressCount)
                .defaultAddress(defaultAddrResponse)
                .build();

        return ApiResponse.<UserProfileResponse>builder()
                .success(true)
                .data(response)
                .message("Profile fetched successfully")
                .build();
    }

    @Transactional
    public ApiResponse<UserProfileResponse> updateUserProfile(UpdateProfileRequest request) {
        User user = getAuthenticatedUser();
        String newName = request.getUserName().trim();
        if (!newName.equalsIgnoreCase(user.getUserName())) {
            if (userRepository.existsByUserName(newName)) {
                return ApiResponse.<UserProfileResponse>builder()
                        .success(false)
                        .message("Username is already taken by another account.")
                        .build();
            }
            user.setUserName(newName);
            user = userRepository.save(user);
        }
        return getUserProfile();
    }

    @Transactional
    public ApiResponse<Void> changePassword(ChangePasswordRequest request) {
        User user = getAuthenticatedUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("Current password does not match our records.")
                    .build();
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("New password must be different from your current password.")
                    .build();
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.<Void>builder()
                .success(true)
                .message("Password changed successfully.")
                .build();
    }

    private AddressResponse mapAddressToResponse(Address address) {
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .label(address.getLabel())
                .shippingAddress(address.getShippingAddress())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .phoneNumber(address.getPhoneNumber())
                .isDefault(address.isDefault())
                .build();
    }
}