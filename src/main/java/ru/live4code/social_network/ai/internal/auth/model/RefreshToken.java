package ru.live4code.social_network.ai.internal.auth.model;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RefreshToken(@NotNull String token, long clientId, @NotNull LocalDateTime expiredAt) {
}