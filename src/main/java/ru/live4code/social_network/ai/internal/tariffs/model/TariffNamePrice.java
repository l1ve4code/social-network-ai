package ru.live4code.social_network.ai.internal.tariffs.model;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public record TariffNamePrice(String name, long price) {
}
