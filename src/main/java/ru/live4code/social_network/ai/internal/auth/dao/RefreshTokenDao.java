package ru.live4code.social_network.ai.internal.auth.dao;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.internal.auth.model.RefreshToken;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.time.OffsetDateTime;

@Dao
@RequiredArgsConstructor
public class RefreshTokenDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertRefreshToken(long clientId, String token, OffsetDateTime expiredAt) {
        namedParameterJdbcTemplate.update("""
                insert into social_network.refresh_token
                    (token, client_id, expired_at)
                values
                    (:token, :clientId, :expiredAt)
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("token", token)
                        .addValue("expiredAt", expiredAt)
        );
    }

    @Nullable
    public RefreshToken getRefreshToken(String token) {
        return namedParameterJdbcTemplate.query("""
                select
                    token,
                    client_id,
                    (expired_at at time zone 'Europe/Moscow') as expired_at
                from social_network.refresh_token
                where token = :token
                """,
                new MapSqlParameterSource("token", token),
                (rs, rn) -> new RefreshToken(
                        rs.getString("token"),
                        rs.getLong("client_id"),
                        rs.getTimestamp("expired_at").toLocalDateTime()
                )
        ).stream().findFirst().orElse(null);
    }

    public void deleteRefreshToken(String token) {
        namedParameterJdbcTemplate.update("""
                delete from social_network.refresh_token
                where token = :token
                """,
                new MapSqlParameterSource("token", token)
        );
    }

}
