package com.jobagent.jobagent.dashboard.controller;

import com.jobagent.jobagent.dashboard.dto.DashboardSummary;
import com.jobagent.jobagent.dashboard.dto.RecentActivity;
import com.jobagent.jobagent.dashboard.service.DashboardService;
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
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get dashboard summary for authenticated user.
     */
    @GetMapping
    public ResponseEntity<DashboardSummary> getDashboard(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting dashboard summary for user {}", userId);

        DashboardSummary summary = dashboardService.getDashboardSummary(userId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get recent activity feed.
     */
    @GetMapping("/activity")
    public ResponseEntity<List<RecentActivity>> getRecentActivity(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "10") int limit) {

        UUID userId = UUID.fromString(jwt.getSubject());
        log.debug("Getting recent activity for user {}, limit {}", userId, limit);

        List<RecentActivity> activities = dashboardService.getRecentActivity(userId, limit);
        return ResponseEntity.ok(activities);
    }
}
