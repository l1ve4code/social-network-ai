package ru.live4code.social_network.ai.internal.client_info.service;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.live4code.social_network.ai.generated.model.*;
import ru.live4code.social_network.ai.internal.client_info.dao.ClientDao;
import ru.live4code.social_network.ai.internal.tariffs.dao.TariffDao;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.tariffs.model.TariffDaysLeft;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientInfoService {

    private final ClientDao clientDao;
    private final TariffDao tariffDao;

    public Client getClientByEmail(String email) {
        var client = clientDao.getClientByEmail(email);
        if (client == null) {
            throw new UsernameNotFoundException("Can't find client by email");
        }
        return client;
    }

    public Client getClientById(long clientId) {
        var client = clientDao.getClientById(clientId);
        if (client == null) {
            throw new UsernameNotFoundException("Can't find client by id");
        }
        return client;
    }

    public ResponseEntity<ClientInfo> getClientInfo() {
        Client client = getCurrentClient();
        return ResponseEntity.ok(client.toClientInfo());
    }

    public ResponseEntity<StatusesInfo> getClientStatusInfo() {
        Client client = getCurrentClient();
        StatusesInfo statusesInfo = clientDao.getClientStatuses(client.getId());
        return ResponseEntity.ok(statusesInfo);
    }

    public ResponseEntity<TariffInfo> getClientTariffInfo() {
        Client client = getCurrentClient();
        @Nullable TariffDaysLeft tariffDaysLeft = tariffDao.getClientTariffInfo(client.getId());
        var response = tariffDaysLeft != null ? toTariffInfo(tariffDaysLeft) : null;
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ClientInfoAllResponse> getClientAllInfo() {
        Client client = getCurrentClient();
        ClientInfo clientInfo = client.toClientInfo();
        StatusesInfo statusesInfo = clientDao.getClientStatuses(client.getId());
        @Nullable TariffDaysLeft tariffDaysLeft = tariffDao.getClientTariffInfo(client.getId());
        TariffInfo tariffResponse = tariffDaysLeft != null ? toTariffInfo(tariffDaysLeft) : null;
        var clientInfoAllResponse = new ClientInfoAllResponse()
                .client(clientInfo)
                .statuses(statusesInfo)
                .tariff(tariffResponse);
        return ResponseEntity.ok(clientInfoAllResponse);
    }

    @NonNull
    public Client getCurrentClient() {
        var email = SecurityContextHolder.getContext().getAuthentication().getName();
        return getClientByEmail(email);
    }


    @NonNull
    public String getCurrentClientEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public UserDetailsService getUserDetailsService() {
        return this::getClientByEmail;
    }

    private static TariffInfo toTariffInfo(TariffDaysLeft tariffDaysLeft) {
        TariffInfo tariffInfo = new TariffInfo();
        tariffInfo.name(tariffDaysLeft.name());
        tariffInfo.daysLeft(tariffDaysLeft.daysLeft());
        tariffInfo.abilities(makeTariffAbility(tariffDaysLeft));
        return tariffInfo;
    }

    private static List<Ability> makeTariffAbility(TariffDaysLeft tariff) {
        return List.of(
                new Ability("Кол-во постов", new AbilityValue(String.valueOf(tariff.publicationAmount()))),
                new Ability("Кол-во тем на выбор", new AbilityValue("до " + (tariff.publicationAmount() * 3))),
                new Ability(
                        "Кол-во автозамен",
                        new AbilityValue(String.valueOf(tariff.publicationAmount() * 3 + tariff.publicationAmount() * 2))
                )
        );
    }

}
