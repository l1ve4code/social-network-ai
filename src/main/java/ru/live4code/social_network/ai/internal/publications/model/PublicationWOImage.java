package ru.live4code.social_network.ai.internal.publications.model;

import jakarta.validation.constraints.NotNull;

public record PublicationWOImage(long id, long clientId, @NotNull String title, @NotNull String description,
                                 @NotNull String imageId) {
}
