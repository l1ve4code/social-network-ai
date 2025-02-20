package ru.live4code.social_network.ai.internal.client_settings.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.external.tenchat.model.TenChatToken;
import ru.live4code.social_network.ai.internal.client_settings.model.RefreshCredentials;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.time.OffsetDateTime;
import java.util.*;

@Dao
@RequiredArgsConstructor
public class TenChatCredentialsDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertUserTokens(long id, String phone, String accessToken, String refreshToken) {
        namedParameterJdbcTemplate.update("""
                insert into social_network.tenchat_credentials (client_id, phone, access_token, refresh_token)
                values (:id, :phone, :accessToken, :refreshToken)
                on conflict (client_id) do update
                set phone = :phone, access_token = :accessToken, refresh_token = :refreshToken
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("phone", phone)
                        .addValue("accessToken", accessToken)
                        .addValue("refreshToken", refreshToken)
        );
    }

    public boolean isTokensExistsByClientId(long clientId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.tenchat_credentials
                    where client_id = :clientId
                )
                """,
                new MapSqlParameterSource("clientId", clientId),
                Boolean.class
        ));
    }

    public void updateCredentialsData(Map<Long, TenChatToken> credentialsData) {
        namedParameterJdbcTemplate.batchUpdate("""
                update social_network.tenchat_credentials
                set access_token = :accessToken, refresh_token = :refreshToken, updated_at = :currentDate
                where client_id = :clientId
                """,
                credentialsData.entrySet().stream().map(item -> {
                        long clientId = item.getKey();
                        var keys = item.getValue();
                        var accessToken = keys.accessToken();
                        var refreshToken = keys.refreshToken();
                        return new MapSqlParameterSource()
                                .addValue("clientId", clientId)
                                .addValue("accessToken", accessToken)
                                .addValue("refreshToken", refreshToken)
                                .addValue("currentDate", OffsetDateTime.now());
                    }
                ).toArray(MapSqlParameterSource[]::new)
        );
    }

    public Map<Long, String> getClientsAccessTokens(Collection<Long> clientIds) {
        var result = new HashMap<Long, String>();
        namedParameterJdbcTemplate.query("""
                select
                    client_id,
                    access_token
                from social_network.tenchat_credentials
                where client_id in (select unnest(:clientIds))
                """,
                new MapSqlParameterSource("clientIds", clientIds.toArray(Long[]::new)),
                (rs, rn) -> result.putIfAbsent(rs.getLong("client_id"), rs.getString("access_token"))
        );
        return result;
    }

    public List<RefreshCredentials> getClientCredentials() {
        return namedParameterJdbcTemplate.query("""
                select client_id, refresh_token
                from social_network.tenchat_credentials
                """,
                Map.of(),
                (rs, rn) -> new RefreshCredentials(
                        rs.getLong("client_id"),
                        rs.getString("refresh_token")
                )
        );
    }

}
