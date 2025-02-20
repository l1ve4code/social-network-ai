package ru.live4code.social_network.ai.external.tenchat.model;

import jakarta.validation.constraints.NotNull;

public record TenChatAccountInfo(@NotNull String sessionId, @NotNull String username, long subscribersCount,
                                 long subscriptionCount) {
}
