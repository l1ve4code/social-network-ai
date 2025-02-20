package ru.live4code.social_network.ai.internal.posts.model;

import jakarta.validation.constraints.NotNull;

public record GeneratedClientPost(long clientId, long clientTariffId, long themeId, @NotNull String text) {
}
