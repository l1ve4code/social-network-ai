package ru.live4code.social_network.ai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Configuration
public class ApplicationConfig {

    @Value("${external.api.chat-gpt.url}")
    private String chatGPTLink;

    @Value("${external.api.image-gpt.url}")
    private String imageGPTLink;

    @Value("${external.api.ten-chat.url}")
    private String tenChatLink;

    @Bean
    private static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        var config = new PropertySourcesPlaceholderConfigurer();
        var resources = new Resource[]{
                new ClassPathResource("production.properties"),
                new ClassPathResource("local.properties"),
                new ClassPathResource("application.properties")
        };
        config.setLocations(resources);
        config.setIgnoreUnresolvablePlaceholders(true);
        config.setIgnoreResourceNotFound(true);

        return config;
    }

    @Bean
    @Qualifier("chatGPTLink")
    public String chatGPTLink(){
        return chatGPTLink;
    }

    @Bean
    @Qualifier("imageGPTLink")
    public String imageGPTLink(){
        return imageGPTLink;
    }

    @Bean
    @Qualifier("tenChatLink")
    public String tenChatLink(){
        return tenChatLink;
    }

}
