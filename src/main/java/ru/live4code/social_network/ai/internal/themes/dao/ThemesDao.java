package ru.live4code.social_network.ai.internal.themes.dao;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.generated.model.Theme;
import ru.live4code.social_network.ai.internal.themes.model.GeneratedClientTheme;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.util.List;

@Dao
@RequiredArgsConstructor
public class ThemesDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertGeneratedThemes(List<GeneratedClientTheme> themes) {
        namedParameterJdbcTemplate.batchUpdate("""
                insert into social_network.client_themes
                    (generation_id, client_id, client_tariff_id, direction_id, text)
                values
                    (:generationId, :clientId, :clientTariffId, :directionId, :text)
                """,
                themes.stream().map(theme -> new MapSqlParameterSource()
                        .addValue("generationId", theme.generationId())
                        .addValue("clientId", theme.clientId())
                        .addValue("clientTariffId", theme.clientTariffId())
                        .addValue("directionId", theme.directionId())
                        .addValue("text", theme.text())
                ).toArray(MapSqlParameterSource[]::new)
        );
    }

    @Nullable
    public Long getClientLastGenerationId(long clientId, long clientTariffId) {
        return namedParameterJdbcTemplate.queryForObject("""
                select max(generation_id)
                from social_network.client_themes
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                Long.class
        );
    }

    public void makeClientThemesApproved(long clientId, long clientTariffId, long generationId) {
        namedParameterJdbcTemplate.update("""
                update social_network.client_themes
                set approved = true
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                    and generation_id = :generationId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("generationId", generationId)
        );
    }

    public boolean isClientApprovedThemesExists(long clientId, long clientTariffId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_themes
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                        and approved is true
                )
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                Boolean.class
        ));
    }

    public List<Theme> getGeneratedThemesForClientByGenerationId(
            long clientId,
            long clientTariffId,
            long generationId
    ) {
        return namedParameterJdbcTemplate.query("""
                select text
                from social_network.client_themes
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                    and generation_id = :generationId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("generationId", generationId),
                (rs, rn) -> new Theme(rs.getString("text"))
        );
    }

}
