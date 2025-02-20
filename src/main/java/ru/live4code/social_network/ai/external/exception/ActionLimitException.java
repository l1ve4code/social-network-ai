package ru.live4code.social_network.ai.external.exception;

public class ActionLimitException extends RuntimeException {
    public ActionLimitException(String message) {
        super(message);
    }
}
