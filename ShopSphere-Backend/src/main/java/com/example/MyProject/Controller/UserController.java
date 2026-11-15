package com.example.MyProject.Controller;

import com.example.MyProject.Services.PasswordResetService;
import com.example.MyProject.Services.UserService;
import com.example.MyProject.User.Dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
// CORS is handled globally by SecurityConfig.corsConfigurationSource().
// Every URL under /api/auth/** is public in SecurityConfig, which is what the sign-in, register and
// password-reset pages need.
public class UserController {

    // The same answer whether or not the email has an account, so this endpoint cannot be used to
    // find out which emails are registered.
    private static final String FORGOT_PASSWORD_MESSAGE =
            "If an account exists for that email, we've sent a link to reset the password. "
                    + "It works for 30 minutes.";

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(@Valid @RequestBody RegisterUserDto dto) {
        ApiResponse<UserResponseDTO> response = userService.registerUser(dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    // CHANGED: passes the caller's address to the service, which limits repeated wrong passwords.
    // Behind a reverse proxy (nginx, a cloud load balancer) add
    //     server.forward-headers-strategy=framework
    // to application.properties, or every visitor will look like they come from the proxy's address.
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginUserDto dto, HttpServletRequest request) {
        ApiResponse<LoginResponseDTO> response = userService.loginUser(dto, request.getRemoteAddr());
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /** Step 1 of "forgot password": emails a reset link (when the email has an account). */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest dto, HttpServletRequest request) {
        passwordResetService.requestReset(dto.getEmail(), request.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message(FORGOT_PASSWORD_MESSAGE)
                .build());
    }

    /** Step 2: the customer opened the emailed link and chose a new password. */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest dto, HttpServletRequest request) {
        passwordResetService.resetPassword(dto.getToken(), dto.getNewPassword(), request.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Your password has been updated. You can sign in now.")
                .build());
    }
}