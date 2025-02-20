package ru.live4code.social_network.ai.internal.posts.model;

import jakarta.validation.constraints.NotNull;
import ru.live4code.social_network.ai.external.minio.model.FilenameBytearray;

public record GeneratedClientPostImage(long transactionId, long generationId, long clientId, long clientTariffId,
                                       long themeId, @NotNull String imageId, byte[] imageBytes) {
    public FilenameBytearray toFilenameBytearray() {
        return new FilenameBytearray(imageId, imageBytes);
    }
    public GeneratedClientImage toGeneratedClientImage() {
        return new GeneratedClientImage(imageId, generationId, clientId, clientTariffId, themeId);
    }
}
