package com.jobagent.jobagent.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {
    public static LoginResponse bearer(String accessToken) {
        return new LoginResponse(accessToken, "Bearer", 1800);
    }
}
