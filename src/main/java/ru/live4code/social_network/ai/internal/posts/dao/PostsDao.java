package ru.live4code.social_network.ai.internal.posts.dao;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.generated.model.Post;
import ru.live4code.social_network.ai.internal.posts.model.GeneratedClientPost;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.util.List;

@Dao
@RequiredArgsConstructor
public class PostsDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void insertGeneratedPosts(List<GeneratedClientPost> posts) {
        namedParameterJdbcTemplate.batchUpdate("""
                insert into social_network.client_posts
                    (client_id, client_tariff_id, theme_id, text)
                values
                    (:clientId, :clientTariffId, :themeId, :text)
                """,
                posts.stream().map(post -> new MapSqlParameterSource()
                        .addValue("clientId", post.clientId())
                        .addValue("clientTariffId", post.clientTariffId())
                        .addValue("themeId", post.themeId())
                        .addValue("text", post.text())
                ).toArray(MapSqlParameterSource[]::new)
        );
    }

    public void updateClientPost(long clientId, long clientTariffId, long postId, String text) {
        namedParameterJdbcTemplate.update("""
                update social_network.client_posts
                set text = :text
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                    and id = :postId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("postId", postId)
                        .addValue("text", text)
        );
    }

    public void makeClientPostsApproved(long clientId, long clientTariffId) {
        namedParameterJdbcTemplate.update("""
                update social_network.client_posts
                set approved = true
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
        );
    }

    public List<Long> getApprovedPostIdsForClient(long clientId, long clientTariffId) {
        return namedParameterJdbcTemplate.query("""
                select id
                from social_network.client_posts
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                    and approved is true
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                (rs, rn) -> rs.getLong("id")
        );
    }

    public List<Post> getGeneratedPostsForClient(long clientId, long clientTariffId) {
        return namedParameterJdbcTemplate.query("""
                select
                    cp.id,
                    ct.text as name,
                    cp.text as description,
                    cp.approved as for_publish
                from social_network.client_posts cp
                join social_network.client_themes ct
                    on cp.theme_id = ct.id
                        and cp.client_id = ct.client_id
                        and cp.client_tariff_id = ct.client_tariff_id
                where cp.client_id = :clientId
                    and cp.client_tariff_id = :clientTariffId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                (rs, rn) -> {
                    var post = new Post();
                    post.setId(rs.getLong("id"));
                    post.setName(rs.getString("name"));
                    post.setDescription(rs.getString("description"));
                    post.setForPublish(rs.getBoolean("for_publish"));
                    return post;
                }
        );
    }

    @Nullable
    public Post getGeneratedPostForClient(long clientId, long clientTariffId, long postId) {
        return namedParameterJdbcTemplate.query("""
                select
                    cp.id,
                    ct.text as name,
                    cp.text as description,
                    cp.approved as for_publish
                from social_network.client_posts cp
                join social_network.client_themes ct
                    on cp.theme_id = ct.id
                        and cp.client_id = ct.client_id
                        and cp.client_tariff_id = ct.client_tariff_id
                where cp.client_id = :clientId
                    and cp.client_tariff_id = :clientTariffId
                    and cp.id = :postId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("postId", postId),
                (rs, rn) -> {
                    var post = new Post();
                    post.setId(rs.getLong("id"));
                    post.setName(rs.getString("name"));
                    post.setDescription(rs.getString("description"));
                    post.setForPublish(rs.getBoolean("for_publish"));
                    return post;
                }
        ).stream().findFirst().orElse(null);
    }

    @Nullable
    public Long getClientPostThemeId(long clientId, long clientTariffId, long postId) {
        return namedParameterJdbcTemplate.query("""
                select theme_id
                from social_network.client_posts
                where client_id = :clientId
                    and client_tariff_id = :clientTariffId
                    and id = :postId
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("postId", postId),
                (rs, rn) -> rs.getLong("theme_id")
        ).stream().findFirst().orElse(null);
    }

    public boolean isClientPostExists(long clientId, long clientTariffId, long postId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_posts
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                        and id = :postId
                )
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId)
                        .addValue("postId", postId),
                Boolean.class
        ));
    }

    public boolean isClientPostsExists(long clientId, long clientTariffId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_posts
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

    public boolean isClientApprovedPostsExists(long clientId, long clientTariffId) {
        return Boolean.TRUE.equals(namedParameterJdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from social_network.client_posts
                    where client_id = :clientId
                        and client_tariff_id = :clientTariffId
                        and approved is true
                )
                """,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("clientTariffId", clientTariffId),
                Boolean.class
        ));
    }

}
