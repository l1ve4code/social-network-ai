package ru.live4code.social_network.ai.internal.tariffs.service;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import ru.live4code.social_network.ai.external.yoomoney.model.PaymentDetails;
import ru.live4code.social_network.ai.external.yoomoney.service.YoomoneyService;
import ru.live4code.social_network.ai.generated.model.*;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.client_info.service.ClientInfoService;
import ru.live4code.social_network.ai.internal.tariffs.dao.ClientPaymentDao;
import ru.live4code.social_network.ai.internal.tariffs.dao.TariffDao;
import ru.live4code.social_network.ai.internal.tariffs.model.Tariff;
import ru.live4code.social_network.ai.internal.tariffs.model.TariffNamePrice;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.util.List;

import static ru.live4code.social_network.ai.utils.color.Color.GREEN;
import static ru.live4code.social_network.ai.utils.color.Color.RED;

@Service
@RequiredArgsConstructor
public class TariffsService {

    private final TariffDao tariffDao;
    private final YoomoneyService yoomoneyService;
    private final ClientPaymentDao clientPaymentDao;
    private final ClientInfoService clientInfoService;

    public ResponseEntity<ActualTariffsResponse> getActualTariffs() {
        var response = new ActualTariffsResponse();
        var tariffs = tariffDao.getActualTariffs();
        if (tariffs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var mappedTariffs = tariffs.stream().map(tariff -> {
            var responseTariff = new ActualTariff();
            responseTariff.setId(tariff.id());
            responseTariff.setName(tariff.name());
            responseTariff.setPrice(tariff.price());
            responseTariff.setDiscountPrice(tariff.discountPrice());
            responseTariff.setAbilities(makeTariffAbility(tariff));
            responseTariff.setIsPromo(tariff.isPromo());
            return responseTariff;
        }).toList();

        response.setTariffs(mappedTariffs);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<GetPaymentLinkResponse> getPaymentLink(long tariffId) {
        Client client = clientInfoService.getCurrentClient();
        long clientId = client.getId();

        if (tariffDao.getClientActualTariffId(clientId) != null) {
            return ResponseEntity.badRequest().build();
        }

        @Nullable TariffNamePrice tariffNameAndPrice = tariffDao.getTariffNameAndPrice(tariffId);
        if (tariffNameAndPrice == null) {
            return ResponseEntity.notFound().build();
        }

        long tariffPrice = tariffNameAndPrice.price();
        PaymentDetails paymentDetails = yoomoneyService.createPaymentAndGetDetails(
                tariffNameAndPrice.name(),
                tariffPrice
        );

        clientPaymentDao.insertClientPayment(paymentDetails.id(), clientId, tariffId, tariffPrice);

        return ResponseEntity.ok(new GetPaymentLinkResponse(paymentDetails.redirectionLink()));
    }

    private static List<Ability> makeTariffAbility(Tariff tariff) {
        boolean publicationExists = tariff.publicationAmount() > 0;
        boolean subscriptionsExists = tariff.subscribesPerDay() > 0 && tariff.unsubscribesPerDay() > 0;
        return List.of(
                new Ability("Кол-во постов", new AbilityValue(String.valueOf(tariff.publicationAmount()))),
                new Ability("Кол-во тем на выбор", new AbilityValue("до " + (tariff.publicationAmount() * 3))),
                new Ability(
                        "Кол-во автозамен",
                        new AbilityValue(
                                String.valueOf(tariff.publicationAmount() * 3 + tariff.publicationAmount() * 2)
                        )
                ),
                new Ability(
                        "Автоматический постинг",
                        new AbilityValue(publicationExists ? "Да" : "Нет").color(publicationExists ? GREEN : RED)
                ),
                new Ability(
                        "Подписки/отписки на другие аккаунты",
                        new AbilityValue(subscriptionsExists ? "Да" : "Нет").color(subscriptionsExists ? GREEN : RED)
                )
        );
    }

}
