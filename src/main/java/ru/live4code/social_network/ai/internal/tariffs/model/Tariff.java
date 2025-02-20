package ru.live4code.social_network.ai.internal.tariffs.model;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public record Tariff(long id, String name, long discountPrice, long price, int publicationAmount, int subscribesPerDay,
                     int unsubscribesPerDay, boolean isPromo) {
}
