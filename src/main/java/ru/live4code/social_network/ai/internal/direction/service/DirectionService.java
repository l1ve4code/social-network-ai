package ru.live4code.social_network.ai.internal.direction.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.live4code.social_network.ai.generated.model.PublicationsDirection;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;
import ru.live4code.social_network.ai.internal.direction.dao.DirectionDao;
import ru.live4code.social_network.ai.internal.direction.exception.error.AlreadySavedException;
import ru.live4code.social_network.ai.internal.direction.exception.error.TooLowWordsException;
import ru.live4code.social_network.ai.internal.tariffs.dao.TariffDao;
import ru.live4code.social_network.ai.utils.annotation.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectionService {

    private final TariffDao tariffDao;
    private final DirectionDao directionDao;
    private final ClientInfoService clientInfoService;

    public ResponseEntity<PublicationsDirection> getClientDirection() {
        Client client = clientInfoService.getCurrentClient();
        @Nullable PublicationsDirection publicationsDirection = directionDao.getClientDirection(client.getId());
        if (publicationsDirection == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(publicationsDirection);
    }

    public ResponseEntity<Void> setClientDirection(String text) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        @Nullable Long actualTariffId = tariffDao.getClientActualTariffId(clientId);
        if (actualTariffId == null) {
            return new ResponseEntity<>(HttpStatus.PAYMENT_REQUIRED);
        }

        if (directionDao.isClientDirectionExists(clientId, actualTariffId)) {
            throw new AlreadySavedException();
        }

        if (text.length() < 10) {
            throw new TooLowWordsException();
        }

        directionDao.insertClientDirection(clientId, actualTariffId, text);
        log.info("Direction was saved for client_id: {}.", clientId);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
