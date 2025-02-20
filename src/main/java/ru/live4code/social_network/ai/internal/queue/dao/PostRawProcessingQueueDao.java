package ru.live4code.social_network.ai.internal.queue.dao;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.internal.queue.model.PostRawProcessingQueue;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dao
public class PostRawProcessingQueueDao extends RawProcessingQueueAbstractDao {

    public PostRawProcessingQueueDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        super(namedParameterJdbcTemplate);
    }

    public void insertToQueue(long clientId, long clientTariffId) {
        namedParameterJdbcTemplate.update("""
                insert into %s
                    (client_id, client_tariff_id)
                values
                    (:clientId, :clientTariffId)
                """.formatted(getTableName()),
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
        );
    }

    public Map<Long, List<PostRawProcessingQueue>> getLatestTransactions(long batchSize) {
        var result = new HashMap<Long, List<PostRawProcessingQueue>>();
        namedParameterJdbcTemplate.query("""
                with transactions_to_process as (
                    select
                        transaction_id,
                        client_id,
                        client_tariff_id
                    from %s
                    where processed_time is null
                    order by transaction_id
                    limit :batchSize
                )
                select
                    ttp.transaction_id,
                    ttp.client_id,
                    ttp.client_tariff_id,
                    ct.id as theme_id,
                    ct.text
                from transactions_to_process ttp
                join social_network.client_themes ct using(client_id, client_tariff_id)
                where ct.approved is true
                """.formatted(getTableName()),
                new MapSqlParameterSource("batchSize", batchSize),
                (rs, rn) -> result.computeIfAbsent(rs.getLong("transaction_id"), k -> new ArrayList<>())
                        .add(new PostRawProcessingQueue(
                                rs.getLong("transaction_id"),
                                rs.getLong("client_id"),
                                rs.getLong("client_tariff_id"),
                                rs.getLong("theme_id"),
                                rs.getString("text")
                        ))
        );
        return result;
    }

    @Override
    @Deprecated
    public void insertToQueue(long clientId, long clientTariffId, long generationId) {
        throw new NotImplementedException("This method is deprecated in posts processing!");
    }

    @Override
    protected String getTableName() {
        return "social_network.client_raw_posts_processing_queue";
    }

}
