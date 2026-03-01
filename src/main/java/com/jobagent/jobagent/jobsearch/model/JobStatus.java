package com.jobagent.jobagent.jobsearch.model;

/**
 * Sprint 5.1 — Status of a job listing.
 */
public enum JobStatus {
    /** Job is currently accepting applications */
    ACTIVE,
    /** Job listing has expired */
    EXPIRED,
    /** User has applied to this job */
    APPLIED,
    /** Job has been filled */
    FILLED,
    /** Job was saved/bookmarked by user */
    SAVED
}
