package ru.live4code.social_network.ai.internal.posts.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.live4code.social_network.ai.generated.api.PostApi;
import ru.live4code.social_network.ai.generated.model.GenerationIdResponse;
import ru.live4code.social_network.ai.generated.model.Post;
import ru.live4code.social_network.ai.generated.model.PostEditRequest;
import ru.live4code.social_network.ai.internal.posts.service.PostImagesService;
import ru.live4code.social_network.ai.internal.posts.service.PostsService;

@RestController
@RequiredArgsConstructor
public class PostController implements PostApi {

    private final PostsService postsService;
    private final PostImagesService postImagesService;

    @Override
    public ResponseEntity<Void> editClientPost(Long postId, PostEditRequest postEditRequest) {
        String text = postEditRequest.getText();
        String imageId = postEditRequest.getImageId();
        return postsService.editClientPost(postId, text, imageId);
    }

    @Override
    public ResponseEntity<Post> getPostInfo(Long postId) {
        return postsService.getPostInfo(postId);
    }

    @Override
    public ResponseEntity<Resource> getClientPostImage(String imageId) {
        return postImagesService.getClientPostImage(imageId);
    }

    @Override
    public ResponseEntity<Void> getClientPostImageGenerationStatus(Long generationId) {
        return postImagesService.getClientPostImageGenerationStatus(generationId);
    }

    @Override
    public ResponseEntity<GenerationIdResponse> generateImageForSpecificPost(Long postId) {
        return postImagesService.generateImageForSpecificPost(postId);
    }

    @Override
    public ResponseEntity<GenerationIdResponse> getLastGenerationIdForPostImage(Long postId) {
        return postImagesService.getLastGenerationIdForPostImage(postId);
    }

}
