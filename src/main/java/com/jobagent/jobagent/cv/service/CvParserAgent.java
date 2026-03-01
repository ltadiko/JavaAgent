package com.jobagent.jobagent.cv.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.jobagent.cv.dto.CvParsedData;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sprint 4.3 — AI agent for parsing CV text into structured data.
 *
 * <p>Uses Spring AI ChatClient with Ollama backend to extract
 * structured information from CV text.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvParserAgent {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("classpath:prompts/cv-parse.st")
    private Resource cvParsePrompt;

    /**
     * Parse CV text into structured data using AI.
     *
     * @param cvText raw text extracted from CV
     * @return parsed structured data
     */
    @CircuitBreaker(name = "cvParser", fallbackMethod = "parseFallback")
    @Retry(name = "cvParser")
    public CvParsedData parse(String cvText) {
        log.debug("Parsing CV text ({} chars) with AI", cvText.length());

        try {
            PromptTemplate promptTemplate = new PromptTemplate(cvParsePrompt);
            Prompt prompt = promptTemplate.create(Map.of("cv_text", truncateIfNeeded(cvText)));

            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            log.debug("AI response received ({} chars)", response != null ? response.length() : 0);

            return parseJsonResponse(response);

        } catch (Exception e) {
            log.error("Failed to parse CV with AI: {}", e.getMessage());
            throw new CvTextExtractor.CvParsingException("AI parsing failed", e);
        }
    }

    /**
     * Fallback method when AI parsing fails.
     */
    public CvParsedData parseFallback(String cvText, Throwable t) {
        log.warn("CV parsing fallback triggered: {}", t.getMessage());
        return CvParsedData.empty();
    }

    /**
     * Parse the JSON response from AI into CvParsedData.
     */
    private CvParsedData parseJsonResponse(String response) {
        if (response == null || response.isBlank()) {
            log.warn("Empty AI response, returning empty parsed data");
            return CvParsedData.empty();
        }

        // Extract JSON from response (AI might include markdown code blocks)
        String json = extractJson(response);

        try {
            return objectMapper.readValue(json, CvParsedData.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse AI response as JSON: {}", e.getMessage());
            return CvParsedData.empty();
        }
    }

    /**
     * Extract JSON from AI response (handles markdown code blocks).
     */
    private String extractJson(String response) {
        String trimmed = response.trim();

        // Handle ```json ... ``` blocks
        if (trimmed.startsWith("```json")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }

        // Handle ``` ... ``` blocks
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }

        return trimmed;
    }

    /**
     * Truncate CV text if too long for AI context.
     */
    private String truncateIfNeeded(String text) {
        int maxLength = 15000; // ~4K tokens for most models
        if (text.length() > maxLength) {
            log.warn("CV text truncated from {} to {} chars", text.length(), maxLength);
            return text.substring(0, maxLength) + "\n\n[... truncated ...]";
        }
        return text;
    }
}
