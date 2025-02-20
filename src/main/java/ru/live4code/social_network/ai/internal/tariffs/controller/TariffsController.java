package ru.live4code.social_network.ai.internal.tariffs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.live4code.social_network.ai.generated.api.TariffsApi;
import ru.live4code.social_network.ai.generated.model.ActualTariffsResponse;
import ru.live4code.social_network.ai.generated.model.GetPaymentLinkResponse;
import ru.live4code.social_network.ai.internal.tariffs.service.TariffsService;

@RestController
@RequiredArgsConstructor
public class TariffsController implements TariffsApi {

    private final TariffsService tariffsService;

    @Override
    public ResponseEntity<ActualTariffsResponse> getActualTariffs() {
        return tariffsService.getActualTariffs();
    }

    @Override
    public ResponseEntity<GetPaymentLinkResponse> getPaymentLink(Long tariffId) {
        return tariffsService.getPaymentLink(tariffId);
    }

}
