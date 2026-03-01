package com.jobagent.jobagent.motivation.service;

import com.jobagent.jobagent.cv.dto.CvParsedData;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.motivation.model.LetterTone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sprint 6.6 — AI agent for generating motivation letters using Spring AI + Ollama.
 */
@Service
@Slf4j
public class MotivationGeneratorAgent {

    private final ChatClient chatClient;
    private final Resource promptTemplate;

    public MotivationGeneratorAgent(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:prompts/motivation-letter.st") Resource promptTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate;
    }

    /**
     * Generate a motivation letter based on CV and job data.
     *
     * @param cvData     Parsed CV data
     * @param job        Target job listing
     * @param tone       Desired tone for the letter
     * @param language   Language code (en, de, nl, etc.)
     * @param additionalInstructions Extra guidance from user
     * @return Generated letter text
     */
    public String generateLetter(
            CvParsedData cvData,
            JobListing job,
            LetterTone tone,
            String language,
            String additionalInstructions) {

        log.info("Generating motivation letter for job: {} at {}", job.getTitle(), job.getCompany());

        String prompt = buildPrompt(cvData, job, tone, language, additionalInstructions);

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("Generated letter with {} characters", response != null ? response.length() : 0);
            return response != null ? response.trim() : "";

        } catch (Exception e) {
            log.error("Failed to generate motivation letter: {}", e.getMessage(), e);
            throw new MotivationGenerationException("Failed to generate motivation letter", e);
        }
    }

    /**
     * Build the prompt for AI generation.
     */
    private String buildPrompt(
            CvParsedData cvData,
            JobListing job,
            LetterTone tone,
            String language,
            String additionalInstructions) {

        PromptTemplate template = new PromptTemplate(promptTemplate);

        Map<String, Object> params = Map.ofEntries(
                Map.entry("candidateName", nvl(cvData.fullName(), "the candidate")),
                Map.entry("candidateTitle", nvl(cvData.currentTitle(), "")),
                Map.entry("candidateSummary", nvl(cvData.summary(), "")),
                Map.entry("candidateSkills", String.join(", ", cvData.skills() != null ? cvData.skills() : java.util.List.of())),
                Map.entry("candidateExperience", formatExperience(cvData)),
                Map.entry("jobTitle", nvl(job.getTitle(), "the position")),
                Map.entry("company", nvl(job.getCompany(), "the company")),
                Map.entry("jobDescription", nvl(job.getDescription(), "")),
                Map.entry("jobRequirements", nvl(job.getRequirements(), "")),
                Map.entry("tone", tone.name().toLowerCase()),
                Map.entry("language", getLanguageName(language)),
                Map.entry("additionalInstructions", nvl(additionalInstructions, ""))
        );

        return template.render(params);
    }

    /**
     * Format experience entries for prompt.
     */
    private String formatExperience(CvParsedData cvData) {
        if (cvData.experience() == null || cvData.experience().isEmpty()) {
            return "No specific experience listed";
        }

        StringBuilder sb = new StringBuilder();
        for (var exp : cvData.experience()) {
            sb.append("- ").append(exp.title())
              .append(" at ").append(exp.company());
            if (exp.description() != null && !exp.description().isBlank()) {
                sb.append(": ").append(exp.description());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Get language name from code.
     */
    private String getLanguageName(String code) {
        return switch (code.toLowerCase()) {
            case "en" -> "English";
            case "de" -> "German";
            case "nl" -> "Dutch";
            case "fr" -> "French";
            case "es" -> "Spanish";
            default -> "English";
        };
    }

    /**
     * Null-safe value getter.
     */
    private String nvl(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    /**
     * Exception for generation failures.
     */
    public static class MotivationGenerationException extends RuntimeException {
        public MotivationGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
