package com.jobagent.jobagent.jobsearch.service;

import com.jobagent.jobagent.common.exception.ResourceNotFoundException;
import com.jobagent.jobagent.common.multitenancy.TenantContext;
import com.jobagent.jobagent.cv.dto.CvParsedData;
import com.jobagent.jobagent.cv.model.CvDetails;
import com.jobagent.jobagent.cv.model.CvStatus;
import com.jobagent.jobagent.cv.repository.CvDetailsRepository;
import com.jobagent.jobagent.jobsearch.dto.JobListingResponse;
import com.jobagent.jobagent.jobsearch.dto.JobMatchScore;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.jobsearch.model.JobStatus;
import com.jobagent.jobagent.jobsearch.repository.JobListingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Sprint 5.6 — Service for matching jobs to user's CV skills.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class JobMatchingService {

    private final JobListingRepository jobListingRepository;
    private final CvDetailsRepository cvDetailsRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get jobs matched to user's latest CV, sorted by match score.
     */
    public Page<JobMatchScore> getMatchedJobs(UUID userId, int minMatchPercentage, int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();

        // Get user's latest parsed CV
        Set<String> userSkills = getUserSkills(userId, tenantId);
        if (userSkills.isEmpty()) {
            log.info("No skills found for user {}, returning empty results", userId);
            return Page.empty();
        }

        log.debug("User {} has skills: {}", userId, userSkills);

        // Get all active jobs
        Pageable pageable = PageRequest.of(0, 1000); // Get all for matching
        Page<JobListing> activeJobs = jobListingRepository
                .findByTenantIdAndStatus(tenantId, JobStatus.ACTIVE, pageable);

        // Calculate match scores
        List<JobMatchScore> matchedJobs = activeJobs.getContent().stream()
                .map(job -> calculateMatchScore(job, userSkills))
                .filter(match -> match.matchPercentage() >= minMatchPercentage)
                .sorted(Comparator.comparingInt(JobMatchScore::matchPercentage).reversed())
                .toList();

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, matchedJobs.size());

        if (start >= matchedJobs.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), matchedJobs.size());
        }

        List<JobMatchScore> pageContent = matchedJobs.subList(start, end);
        return new PageImpl<>(pageContent, PageRequest.of(page, size), matchedJobs.size());
    }

    /**
     * Calculate match score for a specific job against user's CV.
     */
    public JobMatchScore calculateMatchForJob(UUID userId, UUID jobId) {
        UUID tenantId = TenantContext.requireTenantId();

        JobListing job = jobListingRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        Set<String> userSkills = getUserSkills(userId, tenantId);
        return calculateMatchScore(job, userSkills);
    }

    /**
     * Get top N job matches for a user.
     */
    public List<JobMatchScore> getTopMatches(UUID userId, int topN) {
        UUID tenantId = TenantContext.requireTenantId();
        Set<String> userSkills = getUserSkills(userId, tenantId);

        if (userSkills.isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, 500);
        return jobListingRepository.findByTenantIdAndStatus(tenantId, JobStatus.ACTIVE, pageable)
                .getContent().stream()
                .map(job -> calculateMatchScore(job, userSkills))
                .filter(match -> match.matchPercentage() > 0)
                .sorted(Comparator.comparingInt(JobMatchScore::matchPercentage).reversed())
                .limit(topN)
                .toList();
    }

    /**
     * Calculate match score between a job and user skills.
     */
    private JobMatchScore calculateMatchScore(JobListing job, Set<String> userSkills) {
        List<String> jobSkills = job.getSkills();

        if (jobSkills == null || jobSkills.isEmpty()) {
            // No skills required = 100% match (or could return 0)
            return JobMatchScore.builder()
                    .jobId(job.getId())
                    .title(job.getTitle())
                    .company(job.getCompany())
                    .location(job.getLocation())
                    .matchPercentage(100)
                    .matchedSkills(List.of())
                    .missingSkills(List.of())
                    .job(JobListingResponse.from(job))
                    .build();
        }

        // Normalize job skills for comparison
        Set<String> normalizedJobSkills = jobSkills.stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());

        // Find matches
        List<String> matched = normalizedJobSkills.stream()
                .filter(skill -> containsSkill(userSkills, skill))
                .toList();

        List<String> missing = normalizedJobSkills.stream()
                .filter(skill -> !containsSkill(userSkills, skill))
                .toList();

        // Calculate percentage
        int percentage = (int) Math.round(
                (double) matched.size() / normalizedJobSkills.size() * 100
        );

        return JobMatchScore.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .matchPercentage(percentage)
                .matchedSkills(matched)
                .missingSkills(missing)
                .job(JobListingResponse.from(job))
                .build();
    }

    /**
     * Check if user has a skill (with fuzzy matching).
     */
    private boolean containsSkill(Set<String> userSkills, String requiredSkill) {
        String normalized = requiredSkill.toLowerCase().trim();

        // Exact match
        if (userSkills.contains(normalized)) {
            return true;
        }

        // Partial match (e.g., "java" matches "java 11", "spring" matches "spring boot")
        for (String userSkill : userSkills) {
            if (userSkill.contains(normalized) || normalized.contains(userSkill)) {
                return true;
            }
        }

        // Common aliases
        return matchesAlias(userSkills, normalized);
    }

    /**
     * Check for common skill aliases.
     */
    private boolean matchesAlias(Set<String> userSkills, String skill) {
        Map<String, List<String>> aliases = Map.of(
                "javascript", List.of("js", "ecmascript"),
                "typescript", List.of("ts"),
                "kubernetes", List.of("k8s"),
                "postgresql", List.of("postgres", "psql"),
                "mongodb", List.of("mongo"),
                "springboot", List.of("spring boot", "spring-boot"),
                "reactjs", List.of("react", "react.js"),
                "nodejs", List.of("node", "node.js"),
                "aws", List.of("amazon web services"),
                "gcp", List.of("google cloud", "google cloud platform")
        );

        // Check if skill is an alias
        for (Map.Entry<String, List<String>> entry : aliases.entrySet()) {
            String canonical = entry.getKey();
            List<String> aliasList = entry.getValue();

            boolean skillIsCanonical = skill.equals(canonical);
            boolean skillIsAlias = aliasList.stream().anyMatch(a -> a.equalsIgnoreCase(skill));

            if (skillIsCanonical || skillIsAlias) {
                // Check if user has canonical or any alias
                if (userSkills.contains(canonical)) return true;
                for (String alias : aliasList) {
                    if (userSkills.contains(alias.toLowerCase())) return true;
                }
            }
        }

        return false;
    }

    /**
     * Extract skills from user's latest parsed CV.
     */
    private Set<String> getUserSkills(UUID userId, UUID tenantId) {
        Optional<CvDetails> latestCv = cvDetailsRepository
                .findTopByUserIdAndTenantIdAndStatusOrderByCreatedAtDesc(
                        userId, tenantId, CvStatus.PARSED);

        if (latestCv.isEmpty()) {
            log.debug("No parsed CV found for user {}", userId);
            return Set.of();
        }

        CvDetails cv = latestCv.get();
        String parsedJson = cv.getParsedJson();

        if (parsedJson == null || parsedJson.isBlank()) {
            return Set.of();
        }

        try {
            CvParsedData parsed = objectMapper.readValue(parsedJson, CvParsedData.class);
            List<String> skills = parsed.skills();

            if (skills == null || skills.isEmpty()) {
                return Set.of();
            }

            return skills.stream()
                    .map(String::toLowerCase)
                    .map(String::trim)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to parse CV JSON for user {}: {}", userId, e.getMessage());
            return Set.of();
        }
    }
}
