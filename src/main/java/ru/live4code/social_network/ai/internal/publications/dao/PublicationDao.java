package ru.live4code.social_network.ai.internal.publications.dao;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.generated.model.Publication;
import ru.live4code.social_network.ai.generated.model.PublicationStatus;
import ru.live4code.social_network.ai.internal.publications.model.PostPublishAt;
import ru.live4code.social_network.ai.internal.publications.model.PublicationWOImage;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Dao
@RequiredArgsConstructor
public class PublicationDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertPublications(long clientId, long clientTariffId, List<PostPublishAt> publications) {
        namedParameterJdbcTemplate.batchUpdate("""
                insert into social_network.client_publications
                    (client_id, client_tariff_id, post_id, publish_at)
                values
                    (:clientId, :clientTariffId, :postId, :publishAt)
                """,
                publications.stream().map(publication -> new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("postId", publication.postId())
                        .addValue("publishAt", publication.publishAt())
                ).toArray(MapSqlParameterSource[]::new)
        );
    }

    public void changeClientPublicationPublishTime(
            long clientId,
            long clientTariffId,
            long publicationId,
            OffsetDateTime publishAt
    ) {
        namedParameterJdbcTemplate.update("""
                update social_network.client_publications
                set publish_at = :publishAt
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                    and id = :publicationId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("publicationId", publicationId)
                        .addValue("publishAt", publishAt)
        );
    }

    public List<PublicationWOImage> getPostsForPublish() {
        return namedParameterJdbcTemplate.query("""
                select
                    cp.id,
                    cp.client_id,
                    ct.text as title,
                    cps.text as description,
                    cpi.id as image_id
                from social_network.client_publications cp
                join social_network.tenchat_credentials tc using(client_id)
                join social_network.client_posts cps
                    on cp.client_id = cps.client_id
                        and cp.client_tariff_id = cps.client_tariff_id
                        and cp.post_id = cps.id
                join social_network.client_themes ct
                    on cp.client_id = ct.client_id
                        and cp.client_tariff_id = ct.client_tariff_id
                        and cps.theme_id = ct.id
                join social_network.client_post_images cpi
                    on cp.client_id = cpi.client_id
                        and cp.client_tariff_id = cpi.client_tariff_id
                        and cps.theme_id = cpi.theme_id
                        and cpi.is_used is true
                where cp.publish_at at time zone 'Europe/Moscow' <= :currentDate
                    and cp.published is false
                """,
                new MapSqlParameterSource()
                        .addValue("currentDate", LocalDateTime.now()),
                (rs, rn) -> new PublicationWOImage(
                        rs.getLong("id"),
                        rs.getLong("client_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("image_id")
                )
        );
    }

    public List<Publication> getClientPublications(long clientId, long clientTariffId) {
        return namedParameterJdbcTemplate.query("""
                select
                    cp.id,
                    (cp.publish_at at time zone 'Europe/Moscow')::date as date,
                    (cp.publish_at at time zone 'Europe/Moscow')::time as time,
                    ct.text,
                    cp.published
                from social_network.client_publications cp
                join social_network.client_posts cps
                    on cp.client_id = cps.client_id
                        and cp.client_tariff_id = cps.client_tariff_id
                        and cp.post_id = cps.id
                join social_network.client_themes ct
                    on cp.client_id = ct.client_id
                        and cp.client_tariff_id = ct.client_tariff_id
                        and cps.theme_id = ct.id
                where cp.client_id = :clientId
                    and cp.client_tariff_id = :clientTariffId
                """,
                new MapSqlParameterSource()
                        .addValue("currentDate", LocalDateTime.now())
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                (rs, rn) -> new Publication(
                        rs.getLong("id"),
                        rs.getString("date"),
                        rs.getString("time"),
                        rs.getString("text"),
                        rs.getBoolean("published") ?
                                PublicationStatus.PUBLISHED : PublicationStatus.SCHEDULED
                )
        );
    }

    @Nullable
    public Publication getClientPublication(long clientId, long clientTariffId, long publicationId) {
        return namedParameterJdbcTemplate.query("""
                select
                    cp.id,
                    (cp.publish_at at time zone 'Europe/Moscow')::date as date,
                    (cp.publish_at at time zone 'Europe/Moscow')::time as time,
                    ct.text,
                    cp.published
                from social_network.client_publications cp
                join social_network.client_posts cps
                    on cp.client_id = cps.client_id
                        and cp.client_tariff_id = cps.client_tariff_id
                        and cp.post_id = cps.id
                join social_network.client_themes ct
                    on cp.client_id = ct.client_id
                        and cp.client_tariff_id = ct.client_tariff_id
                        and cps.theme_id = ct.id
                where cp.client_id = :clientId
                    and cp.client_tariff_id = :clientTariffId
                    and cp.id = :publicationId
                """,
                new MapSqlParameterSource()
                        .addValue("currentDate", LocalDateTime.now())
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("publicationId", publicationId),
                (rs, rn) -> new Publication(
                        rs.getLong("id"),
                        rs.getString("date"),
                        rs.getString("time"),
                        rs.getString("text"),
                        rs.getBoolean("published") ?
                                PublicationStatus.PUBLISHED : PublicationStatus.SCHEDULED
                )
        ).stream().findFirst().orElse(null);
    }

    public void markPublished(Collection<Long> publicationIds) {
        namedParameterJdbcTemplate.update("""
                update social_network.client_publications
                set published = true
                where id in (select unnest(:publicationIds))
                """,
                new MapSqlParameterSource("publicationIds", publicationIds.toArray(Long[]::new))
        );
    }

    public boolean isPublished(long clientId, long clientTariffId, long publicationId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_publications
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                        and published is true
                        and id = :publicationId
                )
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("publicationId", publicationId),
                Boolean.class
        ));
    }

    public boolean isClientPublicationsExists(long clientId, long clientTariffId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_publications
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                )
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                Boolean.class
        ));
    }

    public boolean isClientPublicationExists(long clientId, long clientTariffId, long publicationId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_publications
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                        and id = :publicationId
                )
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("publicationId", publicationId),
                Boolean.class
        ));
    }

}
