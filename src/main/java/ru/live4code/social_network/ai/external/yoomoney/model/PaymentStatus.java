package ru.live4code.social_network.ai.external.yoomoney.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    PENDING("pending"),
    SUCCEEDED("succeeded"),
    ERROR("error");

    private final String status;

    public static PaymentStatus findStatus(String text) {
        for (var value : PaymentStatus.values()) {
            if (value.status.equals(text)) {
                return value;
            }
        }
        return ERROR;
    }

}
