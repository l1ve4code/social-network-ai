package ru.live4code.social_network.ai.external.yoomoney.model;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public record PaymentDetails(String id, String redirectionLink) {
}