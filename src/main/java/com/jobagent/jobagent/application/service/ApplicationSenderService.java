package com.jobagent.jobagent.application.service;

import com.jobagent.jobagent.application.model.ApplicationStatus;
import com.jobagent.jobagent.application.model.JobApplication;
import com.jobagent.jobagent.application.repository.JobApplicationRepository;
import com.jobagent.jobagent.common.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Sprint 7.6 — Service for sending job applications.
 *
 * <p>For MVP, this simulates sending by logging. In production, this would:
 * - Send email with CV and cover letter
 * - Submit to job portal APIs
 * - Track delivery status
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationSenderService {

    private final JobApplicationRepository applicationRepository;

    /**
     * Send application asynchronously.
     */
    @Async("applicationSenderExecutor")
    public void sendAsync(UUID applicationId) {
        log.info("Starting async send for application: {}", applicationId);

        try {
            // Small delay to simulate processing
            Thread.sleep(1000);
            send(applicationId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Application sending interrupted: {}", applicationId);
        }
    }

    /**
     * Send an application.
     */
    @Transactional
    public void send(UUID applicationId) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElse(null);

        if (application == null) {
            log.warn("Application not found for sending: {}", applicationId);
            return;
        }

        // Set tenant context for the operation
        TenantContext.setTenantId(application.getTenantId());

        try {
            if (application.getStatus() != ApplicationStatus.PENDING) {
                log.warn("Application {} not in PENDING status, skipping send", applicationId);
                return;
            }

            application.setStatus(ApplicationStatus.PROCESSING);
            applicationRepository.save(application);

            // Simulate sending process
            boolean success = simulateSend(application);

            if (success) {
                String confirmationRef = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                application.markSent(confirmationRef, "EMAIL");
                log.info("Successfully sent application {} with reference {}",
                        applicationId, confirmationRef);
            } else {
                application.markFailed("Simulated failure for testing");
                log.warn("Failed to send application {}", applicationId);
            }

            applicationRepository.save(application);

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Simulate sending an application.
     *
     * <p>In production, this would:
     * - Compose email with CV attachment and cover letter
     * - Use SMTP or email service (SendGrid, SES, etc.)
     * - Or submit to job portal API
     *
     * @return true if "sent" successfully
     */
    private boolean simulateSend(JobApplication application) {
        log.info("=== SIMULATED APPLICATION SEND ===");
        log.info("To: {} ({})", application.getJob().getCompany(), application.getJob().getTitle());
        log.info("From: {} ({})",
                application.getUser().getFullName(),
                application.getUser().getEmail());
        log.info("CV: {}", application.getCv().getFileName());
        if (application.getLetter() != null) {
            log.info("Cover Letter: {} words",
                    application.getLetter().getWordCount());
        }
        if (application.getAdditionalMessage() != null) {
            log.info("Additional Message: {}", application.getAdditionalMessage());
        }
        log.info("=================================");

        // Always succeed in simulation (can add random failures for testing)
        return true;
    }

    /**
     * Retry failed applications.
     */
    @Transactional
    public int retryFailed() {
        var failedApplications = applicationRepository.findByStatus(ApplicationStatus.FAILED);
        int retried = 0;

        for (JobApplication application : failedApplications) {
            application.setStatus(ApplicationStatus.PENDING);
            application.setFailureReason(null);
            applicationRepository.save(application);
            sendAsync(application.getId());
            retried++;
        }

        log.info("Retried {} failed applications", retried);
        return retried;
    }
}
