package ru.live4code.social_network.ai.external.minio.model;

import jakarta.validation.constraints.NotNull;

public record FilenameBytearray(@NotNull String filename, byte[] imageBytes) {
}
