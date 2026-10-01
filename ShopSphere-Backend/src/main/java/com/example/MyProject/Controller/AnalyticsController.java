package com.example.MyProject.Controller;

import com.example.MyProject.Analytics.dto.AnalyticsResponse;
import com.example.MyProject.Services.AnalyticsService;
import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:4500", allowCredentials = "true")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Revenue over time + best-selling products, computed from real PAID
     * orders. `days` controls the reporting window (defaults to 30, capped
     * at 365 in the service to keep this from being asked to aggregate an
     * unbounded amount of history in one request).
     */
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
