package ru.live4code.social_network.ai.internal.queue.dao;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.internal.queue.model.ImageRawProcessingQueue;
import ru.live4code.social_network.ai.internal.queue.model.RawImageFromRawPost;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.util.List;

@Dao
public class ImageRawProcessingQueueDao extends RawProcessingQueueAbstractDao {

    public static final Long DEFAULT_GENERATION_ID = 1L;

    public ImageRawProcessingQueueDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(namedParameterJdbcTemplate);
    }

    public void insertToQueueWithDefaultGenerationId(List<RawImageFromRawPost> rawProcessingImages) {
        namedParameterJdbcTemplate.batchUpdate("""
                insert into %s
                    (generation_id, client_id, client_tariff_id, theme_id)
                values
                    (:generationId, :clientId, :clientTariffId, :themeId)
                """.formatted(getTableName()),
                rawProcessingImages.stream().map(image -> new MapSqlParameterSource()
                        .addValue("generationId", DEFAULT_GENERATION_ID)
                        .addValue("clientId", image.clientId())
                        .addValue("clientTariffId", image.clientTariffId())
                        .addValue("themeId", image.themeId())
                ).toArray(MapSqlParameterSource[]::new)
        );
    }

    public void insertToQueue(long clientId, long clientTariffId, long generationId, long themeId) {
        namedParameterJdbcTemplate.update("""
                insert into %s
                    (client_id, client_tariff_id, generation_id, theme_id)
                values
                    (:clientId, :clientTariffId, :generationId, :themeId)
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("generationId", generationId)
                        .addValue("themeId", themeId)
        );
    }

    public List<ImageRawProcessingQueue> getLatestTransactions(long batchSize) {
        return namedParameterJdbcTemplate.query("""
                with transactions_to_process as (
                    select
                        transaction_id,
                        generation_id,
                        client_id,
                        client_tariff_id,
                        theme_id
                    from %s
                    where processed_time is null
                    order by transaction_id
                    limit :batchSize
                )
                select
                    ttp.transaction_id,
                    ttp.generation_id,
                    ttp.client_id,
                    ttp.client_tariff_id,
                    ttp.theme_id,
                    ct.text
                from transactions_to_process ttp
                join social_network.client_themes ct
                    on ttp.theme_id = ct.id
                        and ttp.client_id = ct.client_id
                        and ttp.client_tariff_id = ct.client_tariff_id
                where ct.approved is true
                """.formatted(getTableName()),
                new MapSqlParameterSource("batchSize", batchSize),
                (rs, rn) -> new ImageRawProcessingQueue(
                                rs.getLong("transaction_id"),
                                rs.getLong("generation_id"),
                                rs.getLong("client_id"),
                                rs.getLong("client_tariff_id"),
                                rs.getLong("theme_id"),
                                rs.getString("text")
                        )
        );
    }

    @Nullable
    public Long getLastAddedTransactionId(long clientId, long clientTariffId, long themeId) {
        return namedParameterJdbcTemplate.queryForObject("""
                select max(transaction_id)
                from %s
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                    and theme_id = :themeId
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("themeId", themeId),
                Long.class
        );
    }

    @Nullable
    public Long getMaxGenerationId(long clientId, long clientTariffId, long themeId) {
        return namedParameterJdbcTemplate.queryForObject("""
                select max(generation_id)
                from %s
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                    and theme_id = :themeId
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("themeId", themeId),
                Long.class
        );
    }

    public boolean isGenerationStarted(long clientId, long clientTariffId, long themeId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from %s
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                        and theme_id = :themeId
                        and processed_time is null
                )
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("themeId", themeId),
                Boolean.class
        ));
    }

    @Override
    @Deprecated
    public Long getLastAddedTransactionId(long clientId, long clientTariffId) {
        throw new NotImplementedException("This method is deprecated in images processing!");
    }

    @Override
    @Deprecated
    public void insertToQueue(long clientId, long clientTariffId, long generationId) {
        throw new NotImplementedException("This method is deprecated in images processing!");
    }

    @Override
    @Deprecated
    public boolean isGenerationStarted(long clientId, long clientTariffId) {
        throw new NotImplementedException("This method is deprecated in images processing!");
    }

    @Override
    protected String getTableName() {
        return "social_network.client_raw_images_processing_queue";
    }
}
