package ru.live4code.social_network.ai.internal.themes.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.live4code.social_network.ai.generated.model.GenerationIdResponse;
import ru.live4code.social_network.ai.generated.model.Theme;
import ru.live4code.social_network.ai.generated.model.ThemesResponse;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;
import ru.live4code.social_network.ai.internal.direction.dao.DirectionDao;
import ru.live4code.social_network.ai.internal.queue.dao.ThemeRawProcessingQueueDao;
import ru.live4code.social_network.ai.internal.tariffs.dao.TariffDao;
import ru.live4code.social_network.ai.internal.themes.dao.ThemesDao;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThemesService {

    private static final long MAX_REGENERATIONS = 3L;

    private final TariffDao tariffDao;
    private final ThemesDao themesDao;
    private final DirectionDao directionDao;
    private final ThemeRawProcessingQueueDao themeRawProcessingQueueDao;
    private final ClientInfoService clientInfoService;

    public ResponseEntity<Void> approveGeneratedThemesForClient() {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        @Nullable Long clientLastGenerationId = themesDao.getClientLastGenerationId(clientId, clientTariffId);
        if (clientLastGenerationId == null) {
            return ResponseEntity.notFound().build();
        }

        if (themesDao.isClientApprovedThemesExists(clientId, clientTariffId)) {
            return ResponseEntity.badRequest().build();
        }

        themesDao.makeClientThemesApproved(clientId, clientTariffId, clientLastGenerationId);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<GenerationIdResponse> generateThemesForClient() {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (!directionDao.isClientDirectionExists(clientId, clientTariffId)) {
            return ResponseEntity.notFound().build();
        }

        if (themesDao.isClientApprovedThemesExists(clientId, clientTariffId)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        if (themeRawProcessingQueueDao.isGenerationStarted(clientId, clientTariffId)) {
            return new ResponseEntity<>(HttpStatus.TOO_EARLY);
        }

        long alreadyGeneratedCount = themeRawProcessingQueueDao.getGenerationCount(clientId, clientTariffId);
        if (alreadyGeneratedCount >= MAX_REGENERATIONS) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        themeRawProcessingQueueDao.insertToQueue(clientId, clientTariffId, alreadyGeneratedCount + 1L);
        @Nullable Long transactionId = themeRawProcessingQueueDao.getLastAddedTransactionId(clientId, clientTariffId);
        if (transactionId == null) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }

        return ResponseEntity.ok(new GenerationIdResponse(transactionId));
    }

    public ResponseEntity<GenerationIdResponse> getLastGenerationIdForThemes() {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        @Nullable Long transactionId = themeRawProcessingQueueDao.getLastAddedTransactionId(clientId, clientTariffId);
        if (transactionId == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new GenerationIdResponse(transactionId));
    }

    public ResponseEntity<ThemesResponse> getClientLastGeneratedThemes() {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        @Nullable Long clientLastGenerationId = themesDao.getClientLastGenerationId(clientId, clientTariffId);
        if (clientLastGenerationId == null) {
            return ResponseEntity.notFound().build();
        }

        List<Theme> lastGeneratedThemes = themesDao.getGeneratedThemesForClientByGenerationId(
                clientId,
                clientTariffId,
                clientLastGenerationId
        );
        if (lastGeneratedThemes.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var response = new ThemesResponse(lastGeneratedThemes);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Void> getClientThemesGenerationStatus(long generationId) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long clientTariffId = tariffDao.getClientActualTariffId(clientId);
        if (clientTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (!themeRawProcessingQueueDao.isTransactionExists(clientId, clientTariffId, generationId)) {
            return ResponseEntity.notFound().build();
        }

        boolean isGenerating = themeRawProcessingQueueDao.isGenerating(clientId, clientTariffId, generationId);

        return isGenerating ? new ResponseEntity<>(HttpStatus.ACCEPTED) : ResponseEntity.ok().build();
    }

}
