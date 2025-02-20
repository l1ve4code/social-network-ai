package ru.live4code.social_network.ai.utils.environment.dao;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.utils.annotation.Dao;

@Dao
@RequiredArgsConstructor
public class EnvironmentDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertEnvironment(String key, String value) {
        namedParameterJdbcTemplate.update("""
                insert into social_network.environment
                    (key, value)
                values
                    (:key, :value)
                """,
                new MapSqlParameterSource()
                        .addValue("key", key)
                        .addValue("value", value)
        );
    }

    @Nullable
    public String getEnvironmentValue(String key) {
        return namedParameterJdbcTemplate.query("""
                select value
                from social_network.environment
                where key = :key
                """,
                new MapSqlParameterSource("key", key),
                (rs, rn) -> rs.getString("value")
        ).stream().findFirst().orElse(null);
    }

    public void deleteEnvironment(String key) {
        namedParameterJdbcTemplate.update("""
                delete from social_network.environment
                where key = :key
                """,
                new MapSqlParameterSource("key", key)
        );
    }

}
