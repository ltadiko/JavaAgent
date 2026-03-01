package com.jobagent.jobagent.application.model;

/**
 * Sprint 7.1 — Status of a job application.
 */
public enum ApplicationStatus {
    /** Initial draft, not submitted */
    DRAFT,
    /** Submitted by user, pending send */
    PENDING,
    /** Being processed for sending */
    PROCESSING,
    /** Successfully sent to employer */
    SENT,
    /** Application was viewed by employer */
    VIEWED,
    /** Application was rejected */
    REJECTED,
    /** Invited for interview */
    INTERVIEW,
    /** Offer received */
    OFFERED,
    /** Application accepted/hired */
    ACCEPTED,
    /** Application withdrawn by user */
    WITHDRAWN,
    /** Sending failed */
    FAILED
}
