package ru.live4code.social_network.ai.external.chat_gpt.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatGPTChoiceResponse {

    @JsonProperty("message")
    private ChatGPTMessageResponse message;

}
