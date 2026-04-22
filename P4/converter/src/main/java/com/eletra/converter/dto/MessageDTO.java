package com.eletra.converter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MessageDTO(
        @JsonProperty("username") String username,
        @JsonProperty("createdAt") String createdAt,
        @JsonProperty("sentAt") String sentAt,
        @JsonProperty("message") String message
) {}