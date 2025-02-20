package ru.live4code.social_network.ai.internal.client_info.dao;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.generated.model.StatusesInfo;
import ru.live4code.social_network.ai.internal.client_info.model.Client;
import ru.live4code.social_network.ai.internal.client_info.model.Role;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.time.LocalDate;

@Dao
@RequiredArgsConstructor
public class ClientDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertClient(String email, String password, boolean disableAccount) {
        namedParameterJdbcTemplate.update("""
                insert into social_network.client (email, password, enabled)
                values (:email, :password, :enabled)
                """,
                new MapSqlParameterSource()
                        .addValue("email", email)
                        .addValue("password", password)
                        .addValue("enabled", disableAccount)
        );
    }

    @Nullable
    public Long getClientIdByEmail(String email) {
        return namedParameterJdbcTemplate.query("""
                select id
                from social_network.client
                where email = :email
                """,
                new MapSqlParameterSource("email", email),
                (rs, rn) -> rs.getLong("id")
        ).stream().findFirst().orElse(null);
    }

    @Nullable
    public Client getClientById(long clientId) {
        return namedParameterJdbcTemplate.query("""
                select
                    id,
                    email,
                    name,
                    surname,
                    password,
                    enabled,
                    role,
                    created_at at time zone 'Europe/Moscow' as created_at
                from social_network.client
                where id = :clientId
                """,
                new MapSqlParameterSource("clientId", clientId),
                (rs, rn) -> new Client(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("password"),
                        rs.getBoolean("enabled"),
                        Role.valueOf(rs.getString("role")),
                        rs.getTimestamp("created_at").toLocalDateTime()
                )
        ).stream().findFirst().orElse(null);
    }

    @Nullable
    public Client getClientByEmail(String email) {
        return namedParameterJdbcTemplate.query("""
                select
                    id, 
                    email,
                    name,
                    surname, 
                    password, 
                    enabled, 
                    role, 
                    created_at at time zone 'Europe/Moscow' as created_at
                from social_network.client
                where email = :email
                """,
                new MapSqlParameterSource("email", email),
                (rs, rn) -> new Client(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("password"),
                        rs.getBoolean("enabled"),
                        Role.valueOf(rs.getString("role")),
                        rs.getTimestamp("created_at").toLocalDateTime()
                )
        ).stream().findFirst().orElse(null);
    }

    public StatusesInfo getClientStatuses(long clientId) {
        return namedParameterJdbcTemplate.query("""
                select distinct
                    (cd.id is not null) as is_direction_filled,
                    (cth.id is not null) as is_themes_filled,
                    (cp.id is not null) as is_publications_filled,
                    (tc.phone is not null) as is_tenchat_filled
                from social_network.client c
                left join social_network.tenchat_credentials tc
                    on c.id = tc.client_id
                left join social_network.client_tariffs ct
                    on c.id = ct.client_id and start_date <= :currentDate and end_date >= :currentDate
                left join social_network.client_directions cd
                    on c.id = cd.client_id and ct.id = cd.client_tariff_id
                left join social_network.client_themes cth
                    on c.id = cth.client_id and ct.id = cth.client_tariff_id and cth.approved is true
                left join social_network.client_posts cp
                    on c.id = cp.client_id and ct.id = cp.client_tariff_id and cp.approved is true
                where c.id = :clientId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("currentDate", LocalDate.now()),
                (rs, rn) -> new StatusesInfo(
                        rs.getBoolean("is_direction_filled"),
                        rs.getBoolean("is_themes_filled"),
                        rs.getBoolean("is_publications_filled"),
                        rs.getBoolean("is_tenchat_filled")
                )
        ).stream().findFirst().orElseThrow();
    }

    public void updateClientNameSurnameByEmail(String email, String name, String surname) {
        namedParameterJdbcTemplate.update("""
                update social_network.client
                set name = :name, surname = :surname
                where email = :email
                """,
                new MapSqlParameterSource()
                        .addValue("name", name)
                        .addValue("surname", surname)
                        .addValue("email", email)
        );
    }

    public void updateClientPasswordByEmail(String email, String password) {
        namedParameterJdbcTemplate.update("""
                update social_network.client
                set password = :password
                where email = :email
                """,
                new MapSqlParameterSource()
                        .addValue("password", password)
                        .addValue("email", email)
        );
    }

    public void updateClientPasswordById(long id, String password) {
        namedParameterJdbcTemplate.update("""
                update social_network.client
                set password = :password
                where id = :id
                """,
                new MapSqlParameterSource()
                        .addValue("password", password)
                        .addValue("id", id)
        );
    }

    public void enableClientById(long id) {
        namedParameterJdbcTemplate.update("""
                update social_network.client
                set enabled = true
                where id = :id
                """,
                new MapSqlParameterSource("id", id)
        );
    }

    public void insertConfirmationToken(String token, long clientId) {
        namedParameterJdbcTemplate.update("""
                insert into social_network.confirmation_token (value, client_id)
                values (:token, :clientId)
                """,
                new MapSqlParameterSource()
                        .addValue("token", token)
                        .addValue("clientId", clientId)
        );
    }

    public Long getClientIdByConfirmationToken(String token) {
        return namedParameterJdbcTemplate.query("""
                select client_id
                from social_network.confirmation_token
                where value = :token
                """,
                new MapSqlParameterSource("token", token),
                (rs, rn) -> rs.getLong("client_id")
        ).stream().findFirst().orElse(null);
    }

    public void deleteConfirmationToken(String token) {
        namedParameterJdbcTemplate.update("""
                delete from social_network.confirmation_token
                where value = :token
                """,
                new MapSqlParameterSource("token", token)
        );
    }

}
