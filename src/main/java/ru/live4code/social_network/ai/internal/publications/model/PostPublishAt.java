package ru.live4code.social_network.ai.internal.publications.model;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record PostPublishAt(long postId, @NotNull OffsetDateTime publishAt) {
}
