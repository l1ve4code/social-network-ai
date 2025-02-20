package ru.live4code.social_network.ai.internal.tariffs.dao;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.internal.tariffs.model.*;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Dao
@RequiredArgsConstructor
public class TariffDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Nullable
    public TariffNamePrice getTariffNameAndPrice(long tariffId) {
        return namedParameterJdbcTemplate.query("""
                select name, price
                from social_network.tariff
                where id = :tariffId
                """,
                new MapSqlParameterSource("tariffId", tariffId),
                (rs, rn) -> new TariffNamePrice(rs.getString("name"), rs.getLong("price"))
        ).stream().findFirst().orElse(null);
    }

    public List<Tariff> getActualTariffs() {
        return namedParameterJdbcTemplate.query("""
                select
                    id, 
                    name, 
                    discount_price, 
                    price, 
                    publication_amount, 
                    subscribes_per_day, 
                    unsubscribes_per_day,
                    is_promo
                from social_network.tariff
                order by id
                """,
                Map.of(),
                (rs, rn) -> new Tariff(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getLong("discount_price"),
                    rs.getLong("price"),
                    rs.getInt("publication_amount"),
                    rs.getInt("subscribes_per_day"),
                    rs.getInt("unsubscribes_per_day"),
                    rs.getBoolean("is_promo")
                )
        );
    }

    @Nullable
    public Long getClientActualTariffId(long clientId) {
        return namedParameterJdbcTemplate.query("""
                select id
                from social_network.client_tariffs
                where client_id = :clientId
                    and start_date <= :currentDate
                    and end_date >= :currentDate
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("currentDate", LocalDate.now()),
                (rs, rn) -> rs.getLong("id")
        ).stream().findFirst().orElse(null);
    }

    public TariffRange getClientActualTariffDateRangeByTariffId(long clientId, long clientTariffId) {
        return namedParameterJdbcTemplate.query("""
                select
                    start_date,
                    end_date
                from social_network.client_tariffs
                where client_id = :clientId
                    and id = :clientTariffId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                (rs, rn) -> new TariffRange(
                        rs.getTimestamp("start_date").toLocalDateTime().toLocalDate(),
                        rs.getTimestamp("end_date").toLocalDateTime().toLocalDate()
                )
        ).stream().findFirst().orElseThrow();
    }

    @Nullable
    public TariffDaysLeft getClientTariffInfo(long clientId) {
        return namedParameterJdbcTemplate.query("""
                select
                    t.name,
                    t.publication_amount,
                    t.subscribes_per_day,
                    t.unsubscribes_per_day,
                    (ct.end_date - :currentDate) as days_left
                from social_network.client_tariffs ct
                join social_network.tariff t on ct.tariff_id = t.id
                where ct.client_id = :clientId
                    and ct.start_date <= :currentDate
                    and ct.end_date >= :currentDate
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("currentDate", LocalDate.now()),
                (rs, rn) -> new TariffDaysLeft(
                        rs.getString("name"),
                        rs.getInt("publication_amount"),
                        rs.getInt("subscribes_per_day"),
                        rs.getInt("unsubscribes_per_day"),
                        rs.getInt("days_left")
                )
        ).stream().findFirst().orElse(null);
    }

    public void insertClientTariffs(Collection<ClientTariff> clientTariffs, LocalDate from, LocalDate to) {
        namedParameterJdbcTemplate.batchUpdate("""
                insert into social_network.client_tariffs
                    (client_id, tariff_id, start_date, end_date)
                values
                    (:clientId, :tariffId, :startDate, :endDate)
                """,
                clientTariffs.stream().map(clientTariff -> new MapSqlParameterSource()
                        .addValue("clientId", clientTariff.clientId())
                        .addValue("tariffId", clientTariff.tariffId())
                        .addValue("startDate", from)
                        .addValue("endDate", to)
                ).toArray(MapSqlParameterSource[]::new)
        );
    }

}
