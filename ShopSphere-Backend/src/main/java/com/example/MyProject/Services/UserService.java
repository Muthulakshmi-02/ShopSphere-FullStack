package com.example.MyProject.Services;

import com.example.MyProject.Configuration.JwtUtil;
import com.example.MyProject.Enum.Role;
import com.example.MyProject.Models.User;
import com.example.MyProject.Repository.UserRepository;
import com.example.MyProject.User.Dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    public ApiResponse<UserResponseDTO> registerUser(RegisterUserDto dto) {
        // Email validation with MessageSource
        if (userRepository.existsByEmail(dto.getEmail())) {
            String errorMsg = messageSource.getMessage("messages.email.exists", null, LocaleContextHolder.getLocale());
            return ApiResponse.<UserResponseDTO>builder()
                    .success(false)
                    .message(errorMsg)
                    .build();
        }

        // Username validation with MessageSource
        if (userRepository.existsByUserName(dto.getUserName())) {
            String errorMsg = messageSource.getMessage("messages.username.exists", null, LocaleContextHolder.getLocale());
            return ApiResponse.<UserResponseDTO>builder()
                    .success(false)
                    .message(errorMsg)
                    .build();
        }

        // SECURITY FIX: never trust a client-supplied role. Every public
        // self-registration is forced to USER; admin accounts must be created
        // through a separate, protected admin-only process.
        User user = User.builder()
                .userName(dto.getUserName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.USER)
                .build();

        user = userRepository.save(user);

        // Success message from properties
        String successMsg = messageSource.getMessage("messages.user.register.success", null, LocaleContextHolder.getLocale());

        return ApiResponse.<UserResponseDTO>builder()
                .success(true)
                .data(mapToResponse(user))
                .message(successMsg)
                .build();
    }

    public ApiResponse<LoginResponseDTO> loginUser(LoginUserDto dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            String errorMsg = messageSource.getMessage("messages.invalid.credentials", null, LocaleContextHolder.getLocale());
            return ApiResponse.<LoginResponseDTO>builder()
                    .success(false)
                    .message(errorMsg)
                    .build();
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        LoginResponseDTO response = LoginResponseDTO.builder()
                .token(token)
                .userName(user.getUserName())
                .role(user.getRole().name())
                .build();

        // Success message from properties
        String successMsg = messageSource.getMessage("messages.user.login.success", null, LocaleContextHolder.getLocale());

        return ApiResponse.<LoginResponseDTO>builder()
                .success(true)
                .data(response)
                .message(successMsg)
                .build();
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
