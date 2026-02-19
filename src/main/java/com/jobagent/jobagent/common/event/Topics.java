package com.jobagent.jobagent.common.event;

/**
 * Kafka topic names used across the application.
 */
public final class Topics {

    private Topics() {}

    public static final String APPLICATION_SUBMITTED = "jobagent.application.submitted";
    public static final String APPLICATION_FAILED = "jobagent.application.failed";
    public static final String APPLICATION_STATUS_CHANGED = "jobagent.application.status-changed";
    public static final String CV_UPLOADED = "jobagent.cv.uploaded";
    public static final String CV_ANALYZED = "jobagent.cv.analyzed";
    public static final String USER_DATA_ERASED = "jobagent.user.data-erased";
}
