package com.jobagent.jobagent.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.jobagent.application.model.ApplicationStatus;
import com.jobagent.jobagent.application.model.JobApplication;
import com.jobagent.jobagent.application.repository.JobApplicationRepository;
import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.repository.UserRepository;
import com.jobagent.jobagent.common.exception.ResourceNotFoundException;
import com.jobagent.jobagent.common.multitenancy.TenantContext;
import com.jobagent.jobagent.cv.dto.CvParsedData;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.model.CvStatus;
import com.jobagent.jobagent.cv.repository.CvDetailsRepository;
import com.jobagent.jobagent.dashboard.dto.*;
import com.jobagent.jobagent.motivation.repository.MotivationLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Sprint 8.1 — Dashboard service aggregating data from all modules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;
    private final CvDetailsRepository cvRepository;
    private final JobApplicationRepository applicationRepository;
    private final MotivationLetterRepository letterRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get dashboard summary for a user.
     */
    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();
        log.debug("Building dashboard summary for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        CvSummary cvSummary;
        try {
            cvSummary = buildCvSummary(userId, tenantId);
        } catch (Exception e) {
            log.warn("Failed to build CV summary: {}", e.getMessage());
            cvSummary = CvSummary.builder().count(0).skillsCount(0).topSkills(List.of()).build();
        }

        ApplicationsSummary appsSummary;
        try {
            appsSummary = buildApplicationsSummary(userId, tenantId);
        } catch (Exception e) {
            log.warn("Failed to build applications summary: {}", e.getMessage());
            appsSummary = ApplicationsSummary.builder().build();
        }

        LettersSummary lettersSummary;
        try {
            lettersSummary = buildLettersSummary(userId, tenantId);
        } catch (Exception e) {
            log.warn("Failed to build letters summary: {}", e.getMessage());
            lettersSummary = LettersSummary.builder().count(0).build();
        }

        return DashboardSummary.builder()
                .user(buildUserSummary(user))
                .cv(cvSummary)
                .jobs(buildJobsSummary(userId, tenantId))
                .applications(appsSummary)
                .letters(lettersSummary)
                .build();
    }

    /**
     * Get recent activity for a user.
     * Not @Transactional to allow graceful handling of individual query failures.
     */
    public List<RecentActivity> getRecentActivity(UUID userId, int limit) {
        UUID tenantId = TenantContext.requireTenantId();
        List<RecentActivity> activities = new ArrayList<>();

        try {
            // Get recent applications
            var recentApps = applicationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                    userId, tenantId, PageRequest.of(0, limit));

            for (JobApplication app : recentApps) {
                activities.add(RecentActivity.builder()
                        .id(UUID.randomUUID())
                        .type(mapApplicationStatus(app.getStatus()))
                        .title(getApplicationActivityTitle(app))
                        .description(app.getJob().getCompany() + " - " + app.getJob().getTitle())
                        .entityType("APPLICATION")
                        .entityId(app.getId())
                        .timestamp(app.getUpdatedAt())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to load recent applications: {}", e.getMessage());
        }

        // Sort by timestamp descending
        activities.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));

        return activities.stream().limit(limit).toList();
    }

    private UserSummary buildUserSummary(User user) {
        return UserSummary.builder()
                .name(user.getFullName())
                .email(user.getEmail())
                .memberSince(user.getCreatedAt())
                .region(user.getRegion())
                .build();
    }

    private CvSummary buildCvSummary(UUID userId, UUID tenantId) {
        var cvs = cvRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(
                userId, tenantId, PageRequest.of(0, 10));

        int count = (int) cvs.getTotalElements();
        Instant latestParsedAt = null;
        int skillsCount = 0;
        List<String> topSkills = new ArrayList<>();

        // Find latest parsed CV
        Optional<CvDetails> latestParsed = cvRepository.findTopByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
                userId, tenantId, CvStatus.PARSED);

        if (latestParsed.isPresent()) {
            CvDetails cv = latestParsed.get();
            latestParsedAt = cv.getUpdatedAt();

            // Parse skills from JSON
            if (cv.getParsedJson() != null) {
                try {
                    CvParsedData data = objectMapper.readValue(cv.getParsedJson(), CvParsedData.class);
                    if (data.skills() != null) {
                        skillsCount = data.skills().size();
                        topSkills = data.skills().stream().limit(5).toList();
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse CV JSON for skills: {}", e.getMessage());
                }
            }
        }

        return CvSummary.builder()
                .count(count)
                .latestParsedAt(latestParsedAt)
                .skillsCount(skillsCount)
                .topSkills(topSkills)
                .build();
    }

    private JobsSummary buildJobsSummary(UUID userId, UUID tenantId) {
        // For now, return placeholder values
        // In a full implementation, this would query job matches
        return JobsSummary.builder()
                .matchesCount(0)
                .topMatchScore(0)
                .newJobsToday(0)
                .savedJobs(0)
                .build();
    }

    private ApplicationsSummary buildApplicationsSummary(UUID userId, UUID tenantId) {
        List<Object[]> statusCounts = applicationRepository.getStatusCounts(userId, tenantId);
        Map<ApplicationStatus, Long> countMap = new HashMap<>();

        for (Object[] row : statusCounts) {
            countMap.put((ApplicationStatus) row[0], (Long) row[1]);
        }

        long total = countMap.values().stream().mapToLong(Long::longValue).sum();

        return ApplicationsSummary.builder()
                .total(total)
                .drafts(countMap.getOrDefault(ApplicationStatus.DRAFT, 0L))
                .pending(countMap.getOrDefault(ApplicationStatus.PENDING, 0L))
                .sent(countMap.getOrDefault(ApplicationStatus.SENT, 0L))
                .interviews(countMap.getOrDefault(ApplicationStatus.INTERVIEW, 0L))
                .offers(countMap.getOrDefault(ApplicationStatus.OFFERED, 0L))
                .rejected(countMap.getOrDefault(ApplicationStatus.REJECTED, 0L))
                .withdrawn(countMap.getOrDefault(ApplicationStatus.WITHDRAWN, 0L))
                .build();
    }

    private LettersSummary buildLettersSummary(UUID userId, UUID tenantId) {
        long count = letterRepository.countByUserIdAndTenantId(userId, tenantId);

        var letters = letterRepository.findByUserIdAndTenantIdOrderByUpdatedAtDesc(
                userId, tenantId, PageRequest.of(0, 1));

        Instant latestAt = letters.hasContent() ? letters.getContent().get(0).getUpdatedAt() : null;

        return LettersSummary.builder()
                .count(count)
                .latestAt(latestAt)
                .build();
    }

    private RecentActivity.ActivityType mapApplicationStatus(ApplicationStatus status) {
        return switch (status) {
            case DRAFT -> RecentActivity.ActivityType.APPLICATION_CREATED;
            case PENDING, PROCESSING -> RecentActivity.ActivityType.APPLICATION_SUBMITTED;
            case SENT -> RecentActivity.ActivityType.APPLICATION_SENT;
            case VIEWED -> RecentActivity.ActivityType.APPLICATION_VIEWED;
            default -> RecentActivity.ActivityType.APPLICATION_RESPONSE;
        };
    }

    private String getApplicationActivityTitle(JobApplication app) {
        return switch (app.getStatus()) {
            case DRAFT -> "Application draft created";
            case PENDING -> "Application submitted";
            case PROCESSING -> "Application being processed";
            case SENT -> "Application sent";
            case VIEWED -> "Application viewed by employer";
            case INTERVIEW -> "Interview scheduled";
            case OFFERED -> "Offer received";
            case ACCEPTED -> "Offer accepted";
            case REJECTED -> "Application rejected";
            case WITHDRAWN -> "Application withdrawn";
            case FAILED -> "Application failed to send";
        };
    }
}
