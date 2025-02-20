package ru.live4code.social_network.ai.internal.platform_workflow.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.live4code.social_network.ai.generated.model.ActionItem;
import ru.live4code.social_network.ai.generated.model.PlatformWorkflowItem;
import ru.live4code.social_network.ai.utils.annotation.Dao;

import java.util.Arrays;
import java.util.List;

@Dao
@RequiredArgsConstructor
public class PlatformWorkflowDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<PlatformWorkflowItem> getPlatformWorkflows() {
        return namedParameterJdbcTemplate.query("""
                select
                    title,
                    title_color,
                    array_to_string(description, ';') as description,
                    link
                from social_network.platform_workflow
                order by id
                """,
                new MapSqlParameterSource(),
                (rs, rn) -> new PlatformWorkflowItem(
                        rs.getString("title"),
                        rs.getString("title_color"),
                        toActionItemList(rs.getString("description")),
                        rs.getString("link")
                )
        );
    }

    private static List<ActionItem> toActionItemList(String arrayedString) {
        return Arrays.stream(arrayedString.split(";")).map(ActionItem::new).toList();
    }

}
