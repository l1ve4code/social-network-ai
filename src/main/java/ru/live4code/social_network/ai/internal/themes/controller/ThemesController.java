package ru.live4code.social_network.ai.internal.themes.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.live4code.social_network.ai.generated.api.ThemesApi;
import ru.live4code.social_network.ai.generated.model.GenerationIdResponse;
import ru.live4code.social_network.ai.generated.model.ThemesResponse;
import ru.live4code.social_network.ai.internal.themes.service.ThemesService;

@RestController
@RequiredArgsConstructor
public class ThemesController implements ThemesApi {

    private final ThemesService themesService;

    @Override
    public ResponseEntity<Void> approveGeneratedThemesForClient() {
        return themesService.approveGeneratedThemesForClient();
    }

    @Override
    public ResponseEntity<GenerationIdResponse> generateThemesForClient() {
        return themesService.generateThemesForClient();
    }

    @Override
    public ResponseEntity<GenerationIdResponse> getLastGenerationIdForThemes() {
        return themesService.getLastGenerationIdForThemes();
    }

    @Override
    public ResponseEntity<ThemesResponse> getClientLastGeneratedThemes() {
        return themesService.getClientLastGeneratedThemes();
    }

    @Override
    public ResponseEntity<Void> getClientThemesGenerationStatus(Long generationId) {
        return themesService.getClientThemesGenerationStatus(generationId);
    }

}
