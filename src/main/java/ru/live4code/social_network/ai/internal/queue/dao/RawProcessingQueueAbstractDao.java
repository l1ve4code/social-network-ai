package ru.live4code.social_network.ai.internal.queue.dao;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

@RequiredArgsConstructor
public abstract class RawProcessingQueueAbstractDao {

    protected final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertToQueue(long clientId, long clientTariffId, long generationId) {
        namedParameterJdbcTemplate.update("""
                insert into %s
                    (client_id, client_tariff_id, generation_id)
                values
                    (:clientId, :clientTariffId, :generationId)
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("generationId", generationId)
        );
    }

    public void markProcessed(List<Long> transactionIds) {
        namedParameterJdbcTemplate.update("""
                update %s
                set processed_time = current_timestamp
                where transaction_id in (select unnest(:transactionIds))
                """.formatted(getTableName()),
                new MapSqlParameterSource("transactionIds", transactionIds.toArray(Long[]::new))
        );
    }

    @Nullable
    public Long getLastAddedTransactionId(long clientId, long clientTariffId) {
        return namedParameterJdbcTemplate.queryForObject("""
                select max(transaction_id)
                from %s
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                Long.class
        );
    }

    public long getGenerationCount(long clientId, long clientTariffId) {
        @Nullable Long count = namedParameterJdbcTemplate.queryForObject("""
                select count(*)
                from %s
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                Long.class
        );
        return count != null ? count : 0L;
    }

    public boolean isGenerationExists(long clientId, long clientTariffId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from %s
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                )
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                Boolean.class
        ));
    }

    public boolean isGenerationStarted(long clientId, long clientTariffId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from %s
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                        and processed_time is null
                )
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                Boolean.class
        ));
    }

    public boolean isTransactionExists(long clientId, long clientTariffId, long transactionId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from %s
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                        and transaction_id = :transactionId
                )
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("transactionId", transactionId),
                Boolean.class
        ));
    }

    public boolean isGenerating(long clientId, long clientTariffId, long transactionId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from %s
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                        and transaction_id = :transactionId
                        and processed_time is null
                )
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("transactionId", transactionId),
                Boolean.class
        ));
    }

    protected abstract String getTableName();

}
