package com.jobagent.jobagent.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after successful login containing JWT token")
public record LoginResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJSUzI1NiIs...")
        @JsonProperty("access_token") String accessToken,

        @Schema(description = "Token type (always Bearer)", example = "Bearer")
        @JsonProperty("token_type") String tokenType,

        @Schema(description = "Token validity duration in seconds", example = "1800", format = "int64")
        @JsonProperty("expires_in") long expiresIn
) {
    public static LoginResponse bearer(String accessToken) {
        return new LoginResponse(accessToken, "Bearer", 1800);
    }
}
