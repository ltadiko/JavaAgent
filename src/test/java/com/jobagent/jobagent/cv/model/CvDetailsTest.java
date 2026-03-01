package com.jobagent.jobagent.cv.model;

import com.jobagent.jobagent.auth.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 3.2 — Unit tests for CvDetails entity.
 */
@DisplayName("CvDetails Entity Tests")
class CvDetailsTest {

    @Test
    @DisplayName("Builder sets default status to UPLOADED")
    void builder_defaultStatus_isUploaded() {
        CvDetails cv = CvDetails.builder()
                .fileName("test.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .s3Key("cv/tenant/user/file.pdf")
                .build();

        assertThat(cv.getStatus()).isEqualTo(CvStatus.UPLOADED);
    }

    @Test
    @DisplayName("Builder sets default active to true")
    void builder_defaultActive_isTrue() {
        CvDetails cv = CvDetails.builder()
                .fileName("test.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .s3Key("cv/tenant/user/file.pdf")
                .build();

        assertThat(cv.getActive()).isTrue();
    }

    @Test
    @DisplayName("All fields are set correctly")
    void builder_allFields_setCorrectly() {
        User user = User.builder()
                .email("test@example.com")
                .emailHash("hash")
                .fullName("Test User")
                .country("US")
                .region("US")
                .build();

        CvDetails cv = CvDetails.builder()
                .user(user)
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .fileSize(2048L)
                .s3Key("cv/tenant123/user456/abc.pdf")
                .status(CvStatus.PARSED)
                .active(false)
                .parsedJson("{\"skills\":[]}")
                .errorMessage(null)
                .build();

        assertThat(cv.getUser()).isEqualTo(user);
        assertThat(cv.getFileName()).isEqualTo("resume.pdf");
        assertThat(cv.getContentType()).isEqualTo("application/pdf");
        assertThat(cv.getFileSize()).isEqualTo(2048L);
        assertThat(cv.getS3Key()).isEqualTo("cv/tenant123/user456/abc.pdf");
        assertThat(cv.getStatus()).isEqualTo(CvStatus.PARSED);
        assertThat(cv.getActive()).isFalse();
        assertThat(cv.getParsedJson()).isEqualTo("{\"skills\":[]}");
    }

    @Test
    @DisplayName("CvStatus enum has expected values")
    void cvStatus_hasExpectedValues() {
        assertThat(CvStatus.values()).containsExactly(
                CvStatus.UPLOADED,
                CvStatus.PARSING,
                CvStatus.PARSED,
                CvStatus.FAILED
        );
    }
}
