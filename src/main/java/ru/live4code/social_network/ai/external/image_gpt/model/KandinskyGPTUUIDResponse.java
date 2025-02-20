package ru.live4code.social_network.ai.external.image_gpt.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KandinskyGPTUUIDResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("uuid")
    private String uuid;

}
