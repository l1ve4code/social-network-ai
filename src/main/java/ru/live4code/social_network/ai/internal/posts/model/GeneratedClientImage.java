package ru.live4code.social_network.ai.internal.posts.model;

import jakarta.validation.constraints.NotNull;

public record GeneratedClientImage(@NotNull String imageId, long generationId, long clientId, long clientTariffId,
                                   long themeId) {
}
