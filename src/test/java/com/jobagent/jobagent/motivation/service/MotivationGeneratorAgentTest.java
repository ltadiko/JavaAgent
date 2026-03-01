package com.jobagent.jobagent.motivation.service;

import com.jobagent.jobagent.cv.dto.CvParsedData;
import com.jobagent.jobagent.jobsearch.model.JobListing;
import com.jobagent.jobagent.motivation.model.LetterTone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Sprint 6.10 — Unit tests for MotivationGeneratorAgent.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MotivationGeneratorAgent Tests")
class MotivationGeneratorAgentTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private MotivationGeneratorAgent agent;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        agent = new MotivationGeneratorAgent(
                chatClientBuilder,
                new ClassPathResource("prompts/motivation-letter.st")
        );
    }

    @Test
    @DisplayName("generateLetter() calls AI and returns content")
    void generateLetter_success_returnsContent() {
        // Given
        CvParsedData cvData = createTestCvData();
        JobListing job = createTestJob();
        String expectedLetter = "Dear Hiring Manager,\n\nI am excited to apply...";

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(expectedLetter);

        // When
        String result = agent.generateLetter(
                cvData, job, LetterTone.PROFESSIONAL, "en", null);

        // Then
        assertThat(result).isEqualTo(expectedLetter);
        verify(chatClient).prompt();
        verify(requestSpec).user(any(String.class));
        verify(requestSpec).call();
    }

    @Test
    @DisplayName("generateLetter() trims whitespace from response")
    void generateLetter_withWhitespace_trimmed() {
        // Given
        CvParsedData cvData = createTestCvData();
        JobListing job = createTestJob();

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("  Letter content  \n\n");

        // When
        String result = agent.generateLetter(
                cvData, job, LetterTone.PROFESSIONAL, "en", null);

        // Then
        assertThat(result).isEqualTo("Letter content");
    }

    @Test
    @DisplayName("generateLetter() handles null response gracefully")
    void generateLetter_nullResponse_returnsEmpty() {
        // Given
        CvParsedData cvData = createTestCvData();
        JobListing job = createTestJob();

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(null);

        // When
        String result = agent.generateLetter(
                cvData, job, LetterTone.PROFESSIONAL, "en", null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("generateLetter() throws exception on AI failure")
    void generateLetter_aiFailure_throwsException() {
        // Given
        CvParsedData cvData = createTestCvData();
        JobListing job = createTestJob();

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("AI service unavailable"));

        // When/Then
        assertThatThrownBy(() -> agent.generateLetter(
                cvData, job, LetterTone.PROFESSIONAL, "en", null))
                .isInstanceOf(MotivationGeneratorAgent.MotivationGenerationException.class)
                .hasMessageContaining("Failed to generate motivation letter");
    }

    @Test
    @DisplayName("generateLetter() includes additional instructions in prompt")
    void generateLetter_withInstructions_includesInPrompt() {
        // Given
        CvParsedData cvData = createTestCvData();
        JobListing job = createTestJob();
        String instructions = "Focus on leadership skills";

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Letter content");

        // When
        agent.generateLetter(cvData, job, LetterTone.CONFIDENT, "de", instructions);

        // Then
        verify(requestSpec).user(argThat((String prompt) ->
                prompt.contains("Focus on leadership skills") &&
                prompt.contains("confident") &&
                prompt.contains("German")
        ));
    }

    private CvParsedData createTestCvData() {
        return new CvParsedData(
                "John Doe",
                "john@example.com",
                "+1234567890",
                "Senior Software Engineer",
                "10+ years of experience in software development",
                List.of("Java", "Spring Boot", "PostgreSQL", "Docker"),
                List.of(new CvParsedData.ExperienceEntry(
                        "Tech Corp",
                        "Lead Developer",
                        "Berlin",
                        "2020",
                        "Present",
                        "Led team of 5 developers"
                )),
                List.of(),
                List.of("English", "German"),
                List.of("AWS Certified")
        );
    }

    private JobListing createTestJob() {
        JobListing job = new JobListing();
        job.setId(UUID.randomUUID());
        job.setTitle("Senior Java Developer");
        job.setCompany("Innovative Solutions GmbH");
        job.setDescription("We are looking for a Senior Java Developer...");
        job.setRequirements("5+ years Java experience, Spring Boot expertise");
        return job;
    }
}
