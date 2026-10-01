package com.example.MyProject.Controller;

import com.example.MyProject.User.Dto.*;
import com.example.MyProject.Services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
// NOTE: CORS is already handled globally by SecurityConfig.corsConfigurationSource().
// The old @CrossOrigin(origins = "https://localhost:4500") here pointed at https
// while the app runs on http, which could break preflight requests for login/register.
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(@RequestBody RegisterUserDto dto) {
        ApiResponse<UserResponseDTO> response = userService.registerUser(dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@RequestBody LoginUserDto dto) {
        ApiResponse<LoginResponseDTO> response = userService.loginUser(dto);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
