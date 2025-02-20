package ru.live4code.social_network.ai.internal.audience.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.internal.tariffs.model.SubscribesAndUnsubscribes;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Dao
@RequiredArgsConstructor
public class AudienceDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Map<Long, SubscribesAndUnsubscribes> getClientsAvailableSubscribesAndUnsubscribes(long batchSize) {
        var result = new HashMap<Long, SubscribesAndUnsubscribes>();
        namedParameterJdbcTemplate.query("""
                select
                    ct.client_id,
                    a.need_subscribes - a.done_subscribes as subscribes,
                    a.need_unsubscribes - a.done_unsubscribes as unsubscribes
                from social_network.client_tariffs ct
                join social_network.tariff t
                    on t.id = ct.tariff_id
                        and t.subscribes_per_day > 0
                        and t.unsubscribes_per_day > 0
                join social_network.tenchat_credentials tc using(client_id)
                join social_network.audience a using(client_id)
                where ct.start_date <= :currentDate
                    and ct.end_date >= :currentDate
                    and (a.need_subscribes - a.done_subscribes > 0 or a.need_unsubscribes - a.done_unsubscribes > 0)
                    and a.processed_at < :currentDate
                limit :batchSize
                """,
                new MapSqlParameterSource()
                        .addValue("currentDate", LocalDate.now())
                        .addValue("batchSize", batchSize),
                (rs, rn) -> result.putIfAbsent(
                        rs.getLong("client_id"),
                        new SubscribesAndUnsubscribes(rs.getInt("subscribes"), rs.getInt("unsubscribes"))
                )
        );
        return result;
    }

    public long getClientAllowedSubscribesPerDay(long clientId, long clientTariffId) {
        return namedParameterJdbcTemplate.query("""
                select t.subscribes_per_day
                from social_network.client_tariffs ct
                join social_network.tariff t on ct.tariff_id = t.id
                where ct.client_id = :clientId
                    and ct.id = :clientTariffId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                (rs, rn) -> rs.getLong("subscribes_per_day")
        ).stream().findFirst().orElse(0L);
    }

    public long getClientAllowedUnsubscribesPerDay(long clientId, long clientTariffId) {
        return namedParameterJdbcTemplate.query("""
                select t.unsubscribes_per_day
                from social_network.client_tariffs ct
                join social_network.tariff t on ct.tariff_id = t.id
                where ct.client_id = :clientId
                    and ct.id = :clientTariffId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                (rs, rn) -> rs.getLong("unsubscribes_per_day")
        ).stream().findFirst().orElse(0L);
    }

    public void setClientSubscribesPerDayForCurrentTariff(long clientId, long amount) {
        namedParameterJdbcTemplate.update("""
                insert into social_network.audience
                    (client_id, need_subscribes, processed_at)
                values
                    (:clientId, :needSubscribes, :processedAt)
                on conflict (client_id) do update
                set need_subscribes = :needSubscribes
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("needSubscribes", amount)
                        .addValue("processedAt", LocalDate.now())
        );
    }

    public void setClientUnsubscribesPerDayForCurrentTariff(long clientId, long amount) {
        namedParameterJdbcTemplate.update("""
                insert into social_network.audience
                    (client_id, need_unsubscribes, processed_at)
                values
                    (:clientId, :needUnsubscribes, :processedAt)
                on conflict (client_id) do update
                set need_unsubscribes = :needUnsubscribes
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("needUnsubscribes", amount)
                        .addValue("processedAt", LocalDate.now())
        );
    }

    public void insertDoneSubscribes(Map<Long, AtomicInteger> doneSubscribesByClient) {
        namedParameterJdbcTemplate.batchUpdate("""
                update social_network.audience
                set done_subscribes = done_subscribes + :doneSubscribes
                where client_id = :clientId
                """,
                doneSubscribesByClient.entrySet().stream().map(clientSubscribes -> new MapSqlParameterSource()
                        .addValue("clientId", clientSubscribes.getKey())
                        .addValue("doneSubscribes", clientSubscribes.getValue().get())
                ).toArray(MapSqlParameterSource[]::new)
        );
    }

    public void insertDoneUnsubscribes(Map<Long, AtomicInteger> doneUnsubscribesByClient) {
        namedParameterJdbcTemplate.batchUpdate("""
                update social_network.audience
                set done_unsubscribes = done_unsubscribes + :doneUnsubscribes
                where client_id = :clientId
                """,
                doneUnsubscribesByClient.entrySet().stream().map(clientUnsubscribes -> new MapSqlParameterSource()
                        .addValue("clientId", clientUnsubscribes.getKey())
                        .addValue("doneUnsubscribes", clientUnsubscribes.getValue().get())
                ).toArray(MapSqlParameterSource[]::new)
        );
    }

    public void markProcessedAt(Collection<Long> clientIds) {
        namedParameterJdbcTemplate.update("""
                update social_network.audience
                set processed_at = :currentDate,
                    done_subscribes = 0,
                    done_unsubscribes = 0
                where client_id in (select unnest(:clientIds))
                """,
                new MapSqlParameterSource()
                        .addValue("clientIds", clientIds.toArray(Long[]::new))
                        .addValue("currentDate", LocalDate.now())
        );
    }

}
