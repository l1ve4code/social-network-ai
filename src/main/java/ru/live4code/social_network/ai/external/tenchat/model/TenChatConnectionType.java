package ru.live4code.social_network.ai.external.tenchat.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TenChatConnectionType {

    RECOMMENDED_AUTHORS("RECOMMENDED_AUTHORS"),
    INTERESTS("INTERESTS"),
    GOAL("GOAL"),
    BUSINESS("BUSINESS"),
    SHARED_FRIENDS("SHARED_FRIENDS"),
    COMPANY("COMPANY"),
    NEW_AUTHORS("NEW_AUTHORS"),
    KEY_SKILL("KEY_SKILL"),
    POSITION("POSITION"),
    EDUCATION("EDUCATION"),
    CONTACT("CONTACT");

    private final String value;

}
