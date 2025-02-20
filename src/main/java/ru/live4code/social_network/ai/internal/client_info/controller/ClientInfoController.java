package ru.live4code.social_network.ai.internal.client_info.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.live4code.social_network.ai.generated.api.ClientApi;
import ru.live4code.social_network.ai.generated.model.ClientInfo;
import ru.live4code.social_network.ai.generated.model.ClientInfoAllResponse;
import ru.live4code.social_network.ai.generated.model.StatusesInfo;
import ru.live4code.social_network.ai.generated.model.TariffInfo;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;

@RestController
@RequiredArgsConstructor
public class ClientInfoController implements ClientApi {

    private final ClientInfoService clientInfoService;

    @Override
    public ResponseEntity<ClientInfoAllResponse> getAllClientInfo() {
        return clientInfoService.getClientAllInfo();
    }

    @Override
    public ResponseEntity<ClientInfo> getClientInfo() {
        return clientInfoService.getClientInfo();
    }

    @Override
    public ResponseEntity<StatusesInfo> getClientStatusInfo() {
        return clientInfoService.getClientStatusInfo();
    }

    @Override
    public ResponseEntity<TariffInfo> getClientTariffInfo() {
        return clientInfoService.getClientTariffInfo();
    }

}
