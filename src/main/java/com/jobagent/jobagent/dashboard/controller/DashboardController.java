package com.jobagent.jobagent.dashboard.controller;

import com.jobagent.jobagent.dashboard.dto.DashboardSummary;
import com.jobagent.jobagent.dashboard.dto.RecentActivity;
import com.jobagent.jobagent.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Sprint 8.3 — REST controller for dashboard endpoints.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Aggregated dashboard summary and activity feed for the authenticated user")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get dashboard summary", description = "Returns a complete dashboard summary including user profile, CV, jobs, applications, and letters statistics")
    @GetMapping
    public ResponseEntity<DashboardSummary> getDashboard(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting dashboard summary for user {}", userId);

        DashboardSummary summary = dashboardService.getDashboardSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get recent activity", description = "Returns the most recent activity items for the user's activity feed")
    @GetMapping("/activity")
    public ResponseEntity<List<RecentActivity>> getRecentActivity(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Maximum number of activity items to return") @RequestParam(defaultValue = "10") int limit) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting recent activity for user {}, limit {}", userId, limit);

        List<RecentActivity> activities = dashboardService.getRecentActivity(userId, limit);
        return ResponseEntity.ok(activities);
    }
}
