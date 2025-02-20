package ru.live4code.social_network.ai.external.tenchat.model;

import jakarta.validation.constraints.NotNull;

public record TenChatToken(@NotNull String accessToken, @NotNull String refreshToken) {
}
