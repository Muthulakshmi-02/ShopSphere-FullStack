package com.example.MyProject.Services;

import com.example.MyProject.Configuration.JwtUtil;
import com.example.MyProject.Enum.Role;
import com.example.MyProject.Models.User;
import com.example.MyProject.Repository.UserRepository;
import com.example.MyProject.User.Dto.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    // Compared against when the email does not exist, so a wrong email takes as long as a
    // wrong password. Otherwise response time reveals which emails are registered.
    private String dummyHash;

    @PostConstruct
    void init() {
        dummyHash = passwordEncoder.encode("not-a-real-password");
    }

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

    public ApiResponse<LoginResponseDTO> loginUser(LoginUserDto dto) {
        User user = userRepository.findFirstByEmailIgnoreCase(dto.getEmail().trim()).orElse(null);

        boolean ok;
        if (user == null) {
            passwordEncoder.matches(dto.getPassword(), dummyHash);   // equalise timing
            ok = false;
        } else {
            ok = passwordEncoder.matches(dto.getPassword(), user.getPassword());
        }

        if (!ok) {
            String errorMsg = messageSource.getMessage("messages.invalid.credentials", null, LocaleContextHolder.getLocale());
            return ApiResponse.<LoginResponseDTO>builder().success(false).message(errorMsg).build();
        }

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
}