package ru.live4code.social_network.ai.internal.tariffs.model;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public record ClientTariffPayment(String paymentId, long clientId, long tariffId) {
}
