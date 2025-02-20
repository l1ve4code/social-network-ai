package ru.live4code.social_network.ai.internal.direction.dao;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.generated.model.PublicationsDirection;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.time.LocalDate;

@Dao
@RequiredArgsConstructor
public class DirectionDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertClientDirection(long clientId, long clientTariffId, String directionText) {
        namedParameterJdbcTemplate.update("""
                insert into social_network.client_directions
                    (client_id, client_tariff_id, text)
                values
                    (:clientId, :clientTariffId, :directionText)
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("directionText", directionText)
        );
    }

    @Nullable
    public PublicationsDirection getClientDirection(long clientId) {
        return namedParameterJdbcTemplate.query("""
                select text
                from social_network.client_tariffs ct
                join social_network.client_directions cd
                    on ct.client_id = cd.client_id
                        and ct.id = cd.client_tariff_id
                where ct.client_id = :clientId
                    and ct.start_date <= :currentDate
                    and ct.end_date >= :currentDate
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("currentDate", LocalDate.now()),
                (rs, rn) -> new PublicationsDirection(rs.getString("text"))
        ).stream().findFirst().orElse(null);
    }

    public boolean isClientDirectionExists(long clientId, long clientTariffId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_directions
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                )
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                Boolean.class
        ));
    }

}
