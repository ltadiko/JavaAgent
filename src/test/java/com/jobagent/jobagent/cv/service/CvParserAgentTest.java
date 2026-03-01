package com.jobagent.jobagent.cv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobagent.jobagent.cv.dto.CvParsedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Sprint 4.3 — Unit tests for CvParserAgent.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CvParserAgent Tests")
class CvParserAgentTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec responseSpec;

    private CvParserAgent parserAgent;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parserAgent = new CvParserAgent(chatClientBuilder, objectMapper);
    }

    @Test
    @DisplayName("Fallback returns empty parsed data")
    void parseFallback_returnsEmptyData() {
        CvParsedData result = parserAgent.parseFallback("some cv text", new RuntimeException("AI error"));

        assertThat(result).isNotNull();
        assertThat(result.fullName()).isNull();
        assertThat(result.skills()).isEmpty();
        assertThat(result.experience()).isEmpty();
    }

    @Test
    @DisplayName("CvParsedData empty factory works")
    void cvParsedData_empty_returnsDefaults() {
        CvParsedData empty = CvParsedData.empty();

        assertThat(empty.fullName()).isNull();
        assertThat(empty.email()).isNull();
        assertThat(empty.skills()).isEmpty();
        assertThat(empty.experience()).isEmpty();
        assertThat(empty.education()).isEmpty();
        assertThat(empty.languages()).isEmpty();
        assertThat(empty.certifications()).isEmpty();
    }

    @Test
    @DisplayName("CvParsedData records hold correct data")
    void cvParsedData_recordsData_correctly() {
        var experience = new CvParsedData.ExperienceEntry(
            "Acme Corp", "Developer", "NYC", "2020", "2023", "Built things"
        );
        var education = new CvParsedData.EducationEntry(
            "MIT", "BS", "CS", "2016", "2020"
        );

        assertThat(experience.company()).isEqualTo("Acme Corp");
        assertThat(experience.title()).isEqualTo("Developer");
        assertThat(education.institution()).isEqualTo("MIT");
        assertThat(education.degree()).isEqualTo("BS");
    }
}
