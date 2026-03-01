package com.jobagent.jobagent.cv.model;

import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.common.model.BaseEntity;
import com.jobagent.jobagent.common.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Sprint 3.2 — CV details entity for tracking uploaded CVs.
 *
 * <p>Each user can have multiple CV versions, but only one active at a time.
 * The actual file is stored in MinIO/S3, referenced by s3Key.
 */
@Entity
@Table(name = "cv_details")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvDetails extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "parsed_json", columnDefinition = "jsonb")
    private String parsedJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CvStatus status = CvStatus.UPLOADED;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "parsed_at")
    private Instant parsedAt;

    @Column(name = "error_message")
    private String errorMessage;
}
