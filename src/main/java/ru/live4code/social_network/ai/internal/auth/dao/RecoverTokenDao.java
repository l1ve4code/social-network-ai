package ru.live4code.social_network.ai.internal.auth.dao;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.time.OffsetDateTime;

@Dao
@RequiredArgsConstructor
public class RecoverTokenDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertRecoverToken(long clientId, String token, OffsetDateTime expiredAt) {
        namedParameterJdbcTemplate.update("""
                insert into social_network.recover_token
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
    public Long getClientIdByToken(String token) {
        return namedParameterJdbcTemplate.query("""
                select client_id
                from social_network.recover_token
                where token = :token
                    and expired_at >= current_timestamp
                """,
                new MapSqlParameterSource("token", token),
                (rs, rn) -> rs.getLong("client_id")
        ).stream().findFirst().orElse(null);
    }

    public void deleteToken(String token) {
        namedParameterJdbcTemplate.update("""
                delete from social_network.recover_token
                where token = :token
                """,
                new MapSqlParameterSource("token", token)
        );
    }

    public boolean isFreshRecoverTokenExists(long clientId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from social_network.recover_token
                    where client_id = :clientId
                        and expired_at >= current_timestamp
                )
                """,
                new MapSqlParameterSource("clientId", clientId),
                Boolean.class
        ));
    }

}
