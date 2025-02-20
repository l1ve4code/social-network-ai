package ru.live4code.social_network.ai.internal.posts.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.generated.model.ImageInfo;
import ru.live4code.social_network.ai.internal.posts.model.GeneratedClientImage;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.live4code.social_network.ai.internal.queue.dao.ImageRawProcessingQueueDao.DEFAULT_GENERATION_ID;

@Dao
@RequiredArgsConstructor
public class PostImagesDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertGeneratedImages(List<GeneratedClientImage> images) {
        namedParameterJdbcTemplate.batchUpdate("""
                insert into social_network.client_post_images
                    (id, generation_id, client_id, client_tariff_id, theme_id, is_used)
                values
                    (:imageId, :generationId, :clientId, :clientTariffId, :themeId, :isUsed)
                """,
                images.stream().map(image -> new MapSqlParameterSource()
                        .addValue("imageId", image.imageId())
                        .addValue("generationId", image.generationId())
                        .addValue("clientId", image.clientId())
                        .addValue("clientTariffId", image.clientTariffId())
                        .addValue("themeId", image.themeId())
                        .addValue("isUsed", image.generationId() == DEFAULT_GENERATION_ID)
                ).toArray(MapSqlParameterSource[]::new)
        );
    }

    public List<ImageInfo> getClientPostImages(long clientId, long clientTariffId, long postId) {
        return namedParameterJdbcTemplate.query("""
                select
                    cpi.id,
                    cpi.is_used
                from social_network.client_post_images cpi
                join social_network.client_posts cp using(client_id, client_tariff_id, theme_id)
                where cpi.client_id = :clientId
                    and cpi.client_tariff_id = :clientTariffId
                    and cp.id = :postId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("postId", postId),
                (rs, rn) -> new ImageInfo(rs.getString("id"), rs.getBoolean("is_used"))
        );
    }

    public Map<Long, List<ImageInfo>> getClientPostImages(long clientId, long clientTariffId, List<Long> postIds) {
        var result = new HashMap<Long, List<ImageInfo>>();
        namedParameterJdbcTemplate.query("""
                select
                    cp.id as post_id,
                    cpi.id as image_id,
                    cpi.is_used
                from social_network.client_post_images cpi
                join social_network.client_posts cp using(client_id, client_tariff_id, theme_id)
                where cpi.client_id = :clientId
                    and cpi.client_tariff_id = :clientTariffId
                    and cp.id in (select unnest(:postIds))
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("postIds", postIds.toArray(Long[]::new)),
                (rs, rn) -> result.computeIfAbsent(rs.getLong("post_id"), k -> new ArrayList<>())
                        .add(new ImageInfo(rs.getString("image_id"), rs.getBoolean("is_used")))
        );
        return result;
    }

    public boolean isCurrentPostImage(long clientId, long clientTariffId, long postId, String imageId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_post_images cpi
                    join social_network.client_posts cp using(client_id, client_tariff_id, theme_id)
                    where cpi.client_id = :clientId
                        and cpi.client_tariff_id = :clientTariffId
                        and cpi.id = :imageId
                        and cp.id = :postId
                )
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("postId", postId)
                        .addValue("imageId", imageId),
                Boolean.class
        ));
    }

    public void makeClientImageForUse(long clientId, long clientTariffId, String imageId) {
        namedParameterJdbcTemplate.update("""
                with image_info as (
                    select client_id, client_tariff_id, theme_id
                    from social_network.client_post_images
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                        and id = :imageId
                )
                update social_network.client_post_images cpi
                set is_used = (cpi.id = :imageId)
                from image_info ii
                where ii.client_id = cpi.client_id
                    and ii.client_tariff_id = cpi.client_tariff_id
                    and ii.theme_id = cpi.theme_id
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("imageId", imageId)
        );
    }

    public boolean isClientPostImage(long clientId, String imageId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_post_images
                    where client_id = :clientId
                        and id = :imageId
                )
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("imageId", imageId),
                Boolean.class
        ));
    }

    public boolean isImagesNotPreparedForClientPosts(long clientId, long clientTariffId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_posts cp
                    left join social_network.client_post_images cpi
                        on cp.client_id = cpi.client_id
                            and cp.client_tariff_id = cpi.client_tariff_id
                            and cp.theme_id = cpi.theme_id
                            and cpi.is_used is true
                    where cp.client_id = :clientId
                        and cp.client_tariff_id = :clientTariffId
                        and cpi.id is null
                )
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                Boolean.class
        ));
    }

}
