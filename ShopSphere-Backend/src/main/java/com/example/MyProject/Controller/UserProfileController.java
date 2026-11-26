package com.example.MyProject.Controller;

import com.example.MyProject.Services.UserService;
import com.example.MyProject.User.Dto.ApiResponse;
import com.example.MyProject.User.Dto.ChangePasswordRequest;
import com.example.MyProject.User.Dto.UpdateProfileRequest;
import com.example.MyProject.User.Dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        ApiResponse<UserProfileResponse> response = userService.getUserProfile();
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        ApiResponse<UserProfileResponse> response = userService.updateUserProfile(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        ApiResponse<Void> response = userService.changePassword(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
