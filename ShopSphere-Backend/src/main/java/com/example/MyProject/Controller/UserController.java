package com.example.MyProject.Controller;

import com.example.MyProject.Services.UserService;
import com.example.MyProject.User.Dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
// CORS is handled globally by SecurityConfig.corsConfigurationSource().
public class UserController {

    private final UserService userService;

    // @Valid was missing on both: the validation annotations on the DTOs never ran,
    // so a 1-character password was accepted.
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(@Valid @RequestBody RegisterUserDto dto) {
        ApiResponse<UserResponseDTO> response = userService.registerUser(dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginUserDto dto) {
        ApiResponse<LoginResponseDTO> response = userService.loginUser(dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}