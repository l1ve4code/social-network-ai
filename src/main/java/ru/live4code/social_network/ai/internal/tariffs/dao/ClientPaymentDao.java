package ru.live4code.social_network.ai.internal.tariffs.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.internal.tariffs.model.ClientTariffPayment;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.util.Collection;
import java.util.List;

@Dao
@RequiredArgsConstructor
public class ClientPaymentDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertClientPayment(String paymentId, long clientId, long tariffId, long amount) {
        namedParameterJdbcTemplate.update("""
                insert into social_network.client_payment
                    (id, client_id, tariff_id, amount)
                values
                    (:id, :clientId, :tariffId, :amount)
                """,
                new MapSqlParameterSource()
                        .addValue("id", paymentId)
                        .addValue("clientId", clientId)
                        .addValue("tariffId", tariffId)
                        .addValue("amount", amount)
        );
    }

    public void markPaymentsSucceeded(Collection<String> paymentIds) {
        namedParameterJdbcTemplate.update("""
                update social_network.client_payment
                set status = 'SUCCEEDED'::social_network.client_payment_status
                where id in (select unnest(:paymentIds))
                """,
                new MapSqlParameterSource("paymentIds", paymentIds.toArray(String[]::new))
        );
    }

    public void markPaymentsError(Collection<String> paymentIds) {
        namedParameterJdbcTemplate.update("""
                update social_network.client_payment
                set status = 'ERROR'::social_network.client_payment_status
                where id in (select unnest(:paymentIds))
                """,
                new MapSqlParameterSource("paymentIds", paymentIds.toArray(String[]::new))
        );
    }

    public List<ClientTariffPayment> getClientPaymentsToApprove(long batchSize) {
        return namedParameterJdbcTemplate.query("""
                select
                    id,
                    client_id,
                    tariff_id
                from social_network.client_payment
                where status = 'PENDING'::social_network.client_payment_status
                limit :batchSize
                """,
                new MapSqlParameterSource("batchSize", batchSize),
                (rs, rn) -> new ClientTariffPayment(
                        rs.getString("id"),
                        rs.getLong("client_id"),
                        rs.getLong("tariff_id")
                )
        );
    }

}
