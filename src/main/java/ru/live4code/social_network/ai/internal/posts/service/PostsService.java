package ru.live4code.social_network.ai.internal.posts.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;
import ru.live4code.social_network.ai.generated.model.GenerationIdResponse;
import ru.live4code.social_network.ai.generated.model.ImageInfo;
import ru.live4code.social_network.ai.generated.model.Post;
import ru.live4code.social_network.ai.generated.model.PostsResponse;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;
import ru.live4code.social_network.ai.internal.posts.dao.PostImagesDao;
import ru.live4code.social_network.ai.internal.posts.dao.PostsDao;
import ru.live4code.social_network.ai.internal.queue.dao.PostRawProcessingQueueDao;
import ru.live4code.social_network.ai.internal.tariffs.dao.TariffDao;
import ru.live4code.social_network.ai.internal.themes.dao.ThemesDao;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostsService {

    private final PostsDao postsDao;
    private final TariffDao tariffDao;
    private final ThemesDao themesDao;
    private final PostImagesDao postImagesDao;
    private final PostRawProcessingQueueDao postRawProcessingQueueDao;
    private final TransactionTemplate transactionTemplate;
    private final ClientInfoService clientInfoService;

    public ResponseEntity<PostsResponse> getClientLastGeneratedPosts() {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (!postsDao.isClientPostsExists(clientId, clientTariffId)) {
            return ResponseEntity.notFound().build();
        }

        List<Post> lastGeneratedPosts = postsDao.getGeneratedPostsForClient(clientId, clientTariffId);
        if (lastGeneratedPosts.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Long> postIds = lastGeneratedPosts.stream().map(Post::getId).toList();
        Map<Long, List<ImageInfo>> imagesForPosts = postImagesDao.getClientPostImages(clientId, clientTariffId, postIds);

        lastGeneratedPosts.forEach(post -> post.setImages(imagesForPosts.getOrDefault(post.getId(), List.of())));

        var response = new PostsResponse(lastGeneratedPosts);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<GenerationIdResponse> generatePostsForClient() {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (!themesDao.isClientApprovedThemesExists(clientId, clientTariffId)) {
            return ResponseEntity.notFound().build();
        }

        if (postsDao.isClientApprovedPostsExists(clientId, clientTariffId)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        if (postRawProcessingQueueDao.isGenerationExists(clientId, clientTariffId)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        postRawProcessingQueueDao.insertToQueue(clientId, clientTariffId);
        @Nullable Long transactionId = postRawProcessingQueueDao.getLastAddedTransactionId(clientId, clientTariffId);
        if (transactionId == null) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        return ResponseEntity.ok(new GenerationIdResponse(transactionId));
    }

    public ResponseEntity<GenerationIdResponse> getLastGenerationIdForPosts() {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        @Nullable Long transactionId = postRawProcessingQueueDao.getLastAddedTransactionId(clientId, clientTariffId);
        if (transactionId == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new GenerationIdResponse(transactionId));
    }

    public ResponseEntity<Void> approveGeneratedPostsForClient() {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (!postsDao.isClientPostsExists(clientId, clientTariffId)) {
            return ResponseEntity.notFound().build();
        }

        if (postsDao.isClientApprovedPostsExists(clientId, clientTariffId)) {
            return ResponseEntity.badRequest().build();
        }

        if (postImagesDao.isImagesNotPreparedForClientPosts(clientId, clientTariffId)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        postsDao.makeClientPostsApproved(clientId, clientTariffId);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> getClientPostsGenerationStatus(long generationId) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (!postRawProcessingQueueDao.isTransactionExists(clientId, clientTariffId, generationId)) {
            return ResponseEntity.notFound().build();
        }

        boolean isGenerating = postRawProcessingQueueDao.isGenerating(clientId, clientTariffId, generationId);

        return isGenerating ? new ResponseEntity<>(HttpStatus.ACCEPTED) : ResponseEntity.ok().build();
    }

    public ResponseEntity<Post> getPostInfo(long postId) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        @Nullable Post clientPost = postsDao.getGeneratedPostForClient(clientId, clientTariffId, postId);
        if (clientPost == null) {
            return ResponseEntity.notFound().build();
        }

        List<ImageInfo> postImages = postImagesDao.getClientPostImages(clientId, clientTariffId, postId);
        clientPost.setImages(postImages);

        return ResponseEntity.ok(clientPost);
    }

    public ResponseEntity<Void> editClientPost(long postId, String text, String imageId) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (postsDao.isClientApprovedPostsExists(clientId, clientTariffId)) {
            return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
        }

        if (text.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        if (!postsDao.isClientPostExists(clientId, clientTariffId, postId)) {
            return ResponseEntity.notFound().build();
        }

        if (!postImagesDao.isClientPostImage(clientId, imageId)) {
            return ResponseEntity.notFound().build();
        }

        if (!postImagesDao.isCurrentPostImage(clientId, clientTariffId, postId, imageId)) {
            return ResponseEntity.badRequest().build();
        }

        transactionTemplate.executeWithoutResult(__ -> {
            postsDao.updateClientPost(clientId, clientTariffId, postId, text);
            postImagesDao.makeClientImageForUse(clientId, clientTariffId, imageId);
        });

        return ResponseEntity.ok().build();
    }

}
