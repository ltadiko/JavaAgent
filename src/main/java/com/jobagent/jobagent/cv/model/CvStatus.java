package com.jobagent.jobagent.cv.model;

/**
 * Sprint 3.1 — CV processing status.
 */
public enum CvStatus {
    /** File uploaded, not yet processed */
    UPLOADED,

    /** AI parsing in progress */
    PARSING,

    /** Successfully parsed and indexed */
    PARSED,

    /** Parsing failed */
    FAILED
}
