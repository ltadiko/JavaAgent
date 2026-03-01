package com.jobagent.jobagent.application.service;

import com.jobagent.jobagent.application.dto.*;
import com.jobagent.jobagent.application.model.ApplicationEvent;
import com.jobagent.jobagent.application.model.ApplicationStatus;
import com.jobagent.jobagent.application.model.JobApplication;
import com.jobagent.jobagent.application.repository.ApplicationEventRepository;
import com.jobagent.jobagent.application.repository.JobApplicationRepository;
import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.repository.UserRepository;
import com.jobagent.jobagent.common.exception.DuplicateResourceException;
import com.jobagent.jobagent.common.exception.ResourceNotFoundException;
import com.jobagent.jobagent.common.multitenancy.TenantContext;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.repository.CvDetailsRepository;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.jobsearch.repository.JobListingRepository;
import com.jobagent.jobagent.motivation.model.MotivationLetter;
import com.jobagent.jobagent.motivation.repository.MotivationLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sprint 7.5 — Service for job application operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final ApplicationEventRepository eventRepository;
    private final UserRepository userRepository;
    private final JobListingRepository jobRepository;
    private final CvDetailsRepository cvRepository;
    private final MotivationLetterRepository letterRepository;
    private final ApplicationSenderService senderService;

    /**
     * Create a new job application (draft).
     */
    @Transactional
    public JobApplicationResponse createApplication(UUID userId, SubmitApplicationRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        log.info("Creating application for user {} to job {}", userId, request.jobId());

        // Check for duplicate
        if (applicationRepository.existsByUserIdAndJobIdAndTenantId(userId, request.jobId(), tenantId)) {
            throw new DuplicateResourceException("You have already applied to this job");
        }

        // Load entities
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        JobListing job = jobRepository.findByIdAndTenantId(request.jobId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + request.jobId()));

        CvDetails cv = cvRepository.findById(request.cvId())
                .orElseThrow(() -> new ResourceNotFoundException("CV not found: " + request.cvId()));

        MotivationLetter letter = null;
        if (request.letterId() != null) {
            letter = letterRepository.findByIdAndTenantId(request.letterId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Letter not found: " + request.letterId()));
        }

        // Create application
        JobApplication application = JobApplication.builder()
                .tenantId(tenantId)
                .user(user)
                .job(job)
                .cv(cv)
                .letter(letter)
                .additionalMessage(request.additionalMessage())
                .status(ApplicationStatus.DRAFT)
                .build();

        application = applicationRepository.save(application);
        eventRepository.save(ApplicationEvent.created(application));
        log.info("Created application {} for job {}", application.getId(), job.getTitle());

        return JobApplicationResponse.from(application);
    }

    /**
     * Submit an application for sending.
     */
    @Transactional
    public JobApplicationResponse submitApplication(UUID applicationId, UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        JobApplication application = applicationRepository.findByIdAndUserIdAndTenantId(
                        applicationId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        if (!application.canSubmit()) {
            throw new IllegalStateException("Cannot submit application in status: " + application.getStatus());
        }

        ApplicationStatus oldStatus = application.getStatus();
        application.submit();
        application = applicationRepository.save(application);
        eventRepository.save(ApplicationEvent.statusChange(
                application, oldStatus, application.getStatus(), "Application submitted"));

        log.info("Submitted application {} for sending", applicationId);

        // Trigger async sending
        senderService.sendAsync(application.getId());

        return JobApplicationResponse.from(application);
    }

    /**
     * Get all applications for a user.
     */
    @Transactional(readOnly = true)
    public Page<JobApplicationResponse> getApplications(UUID userId, int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, size);

        return applicationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId, pageable)
                .map(JobApplicationResponse::from);
    }

    /**
     * Get applications by status.
     */
    @Transactional(readOnly = true)
    public Page<JobApplicationResponse> getApplicationsByStatus(
            UUID userId, ApplicationStatus status, int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, size);

        return applicationRepository.findByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
                        userId, tenantId, status, pageable)
                .map(JobApplicationResponse::from);
    }

    /**
     * Get a specific application.
     */
    @Transactional(readOnly = true)
    public JobApplicationResponse getApplication(UUID applicationId, UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        JobApplication application = applicationRepository.findByIdAndUserIdAndTenantId(
                        applicationId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        return JobApplicationResponse.from(application);
    }

    /**
     * Update application status.
     */
    @Transactional
    public JobApplicationResponse updateStatus(UUID applicationId, UUID userId, ApplicationStatusUpdate update) {
        UUID tenantId = TenantContext.requireTenantId();

        JobApplication application = applicationRepository.findByIdAndUserIdAndTenantId(
                        applicationId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(update.status());
        application = applicationRepository.save(application);
        eventRepository.save(ApplicationEvent.statusChange(
                application, oldStatus, update.status(), update.notes()));

        log.info("Updated application {} status to {}", applicationId, update.status());

        return JobApplicationResponse.from(application);
    }

    /**
     * Delete a draft application.
     */
    @Transactional
    public void deleteApplication(UUID applicationId, UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        JobApplication application = applicationRepository.findByIdAndUserIdAndTenantId(
                        applicationId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        if (!application.canModify()) {
            throw new IllegalStateException("Cannot delete application in status: " + application.getStatus());
        }

        applicationRepository.delete(application);
        log.info("Deleted application {}", applicationId);
    }

    /**
     * Withdraw an application.
     */
    @Transactional
    public JobApplicationResponse withdrawApplication(UUID applicationId, UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        JobApplication application = applicationRepository.findByIdAndUserIdAndTenantId(
                        applicationId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        ApplicationStatus oldStatus = application.getStatus();
        application.withdraw();
        application = applicationRepository.save(application);
        eventRepository.save(ApplicationEvent.statusChange(
                application, oldStatus, ApplicationStatus.WITHDRAWN, "Application withdrawn by user"));

        log.info("Withdrew application {}", applicationId);

        return JobApplicationResponse.from(application);
    }

    /**
     * Get application statistics for a user.
     */
    @Transactional(readOnly = true)
    public ApplicationStatsResponse getStats(UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        List<Object[]> statusCounts = applicationRepository.getStatusCounts(userId, tenantId);
        Map<String, Long> byStatus = new HashMap<>();
        long total = 0;

        for (Object[] row : statusCounts) {
            ApplicationStatus status = (ApplicationStatus) row[0];
            Long count = (Long) row[1];
            byStatus.put(status.name(), count);
            total += count;
        }

        return new ApplicationStatsResponse(
                total,
                byStatus.getOrDefault(ApplicationStatus.DRAFT.name(), 0L),
                byStatus.getOrDefault(ApplicationStatus.PENDING.name(), 0L),
                byStatus.getOrDefault(ApplicationStatus.SENT.name(), 0L),
                byStatus.getOrDefault(ApplicationStatus.INTERVIEW.name(), 0L),
                byStatus.getOrDefault(ApplicationStatus.OFFERED.name(), 0L),
                byStatus.getOrDefault(ApplicationStatus.REJECTED.name(), 0L),
                byStatus
        );
    }

    /**
     * Get timeline of events for an application.
     */
    @Transactional(readOnly = true)
    public List<ApplicationTimeline> getTimeline(UUID applicationId, UUID userId) {
        UUID tenantId = TenantContext.requireTenantId();

        // Verify the user owns this application
        applicationRepository.findByIdAndUserIdAndTenantId(applicationId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));

        return eventRepository.findByApplicationIdAndTenantIdOrderByCreatedAtDesc(applicationId, tenantId)
                .stream()
                .map(ApplicationTimeline::from)
                .toList();
    }

    /**
     * Check if user has applied to a job.
     */
    @Transactional(readOnly = true)
    public boolean hasApplied(UUID userId, UUID jobId) {
        UUID tenantId = TenantContext.requireTenantId();
        return applicationRepository.existsByUserIdAndJobIdAndTenantId(userId, jobId, tenantId);
    }
}
