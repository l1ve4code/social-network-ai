package ru.live4code.social_network.ai.internal.queue.model;

import ru.live4code.social_network.ai.internal.posts.model.GeneratedClientPost;

public record RawImageFromRawPost(long clientId, long clientTariffId, long themeId) {
    public static RawImageFromRawPost fromGeneratedClientPost(GeneratedClientPost posts) {
        return new RawImageFromRawPost(posts.clientId(), posts.clientTariffId(), posts.themeId());
    }
}
