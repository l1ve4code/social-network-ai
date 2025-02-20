package ru.live4code.social_network.ai.internal.posts.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.live4code.social_network.ai.generated.api.PostsApi;
import ru.live4code.social_network.ai.generated.model.GenerationIdResponse;
import ru.live4code.social_network.ai.generated.model.PostsResponse;
import ru.live4code.social_network.ai.internal.posts.service.PostsService;

@RestController
@RequiredArgsConstructor
public class PostsController implements PostsApi {

    private final PostsService postsService;

    @Override
    public ResponseEntity<PostsResponse> getClientLastGeneratedPosts() {
        return postsService.getClientLastGeneratedPosts();
    }

    @Override
    public ResponseEntity<GenerationIdResponse> generatePostsForClient() {
        return postsService.generatePostsForClient();
    }

    @Override
    public ResponseEntity<GenerationIdResponse> getLastGenerationIdForPosts() {
        return postsService.getLastGenerationIdForPosts();
    }

    @Override
    public ResponseEntity<Void> approveGeneratedPostsForClient() {
        return postsService.approveGeneratedPostsForClient();
    }

    @Override
    public ResponseEntity<Void> getClientPostsGenerationStatus(Long generationId) {
        return postsService.getClientPostsGenerationStatus(generationId);
    }

}
