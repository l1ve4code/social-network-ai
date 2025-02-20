package ru.live4code.social_network.ai.internal.tariffs.model;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public record TariffDaysLeft(String name, int publicationAmount, int subscribesPerDay, int unsubscribesPerDay,
                             int daysLeft) {
}
