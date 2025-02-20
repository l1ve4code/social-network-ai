package ru.live4code.social_network.ai.internal.client_settings.model;

import jakarta.validation.constraints.NotNull;

public record RefreshCredentials(long clientId, @NotNull String refreshToken) {
}
