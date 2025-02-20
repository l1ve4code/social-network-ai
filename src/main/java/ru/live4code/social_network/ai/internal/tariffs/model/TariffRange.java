package ru.live4code.social_network.ai.internal.tariffs.model;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TariffRange(@NotNull LocalDate start, @NotNull LocalDate end) {
}
