package ru.live4code.social_network.ai.internal.queue.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.internal.queue.model.ThemeRawProcessingQueue;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.util.List;

@Dao
public class ThemeRawProcessingQueueDao extends RawProcessingQueueAbstractDao {

    public ThemeRawProcessingQueueDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(namedParameterJdbcTemplate);
    }

    public List<ThemeRawProcessingQueue> getLatestTransactions(long batchSize) {
        return namedParameterJdbcTemplate.query("""
                with transactions_to_process as (
                    select
                        transaction_id,
                        generation_id,
                        client_id,
                        client_tariff_id
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
                    cd.id as direction_id,
                    cd.text,
                    t.publication_amount
                from transactions_to_process ttp
                join social_network.client_tariffs ct
                    on ttp.client_id = ct.client_id
                        and ttp.client_tariff_id = ct.id
                join social_network.tariff t
                    on t.id = ct.tariff_id
                join social_network.client_directions cd
                    on ttp.client_id = cd.client_id
                        and ttp.client_tariff_id = cd.client_tariff_id
                """.formatted(getTableName()),
                new MapSqlParameterSource("batchSize", batchSize),
                (rs, rn) -> new ThemeRawProcessingQueue(
                        rs.getLong("transaction_id"),
                        rs.getLong("generation_id"),
                        rs.getLong("client_id"),
                        rs.getLong("client_tariff_id"),
                        rs.getLong("direction_id"),
                        rs.getString("text"),
                        rs.getLong("publication_amount")
                )
        );
    }

    @Override
    protected String getTableName() {
        return "social_network.client_raw_themes_processing_queue";
    }

}
