package com.example.MyProject.Controller;

import com.example.MyProject.Analytics.dto.AnalyticsResponse;
import com.example.MyProject.Services.AnalyticsService;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// @CrossOrigin removed: CORS is configured once in SecurityConfig.
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /** Revenue over time + best sellers from real PAID orders. `days` is capped at 365 in the service. */
    @GetMapping
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        AnalyticsResponse response = analyticsService.getAnalytics(days);
        return ResponseEntity.ok(ApiResponse.<AnalyticsResponse>builder()
                .success(true)
                .data(response)
                .build());
    }
}