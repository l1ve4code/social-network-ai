package ru.live4code.social_network.ai.internal.platform_workflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import ru.live4code.social_network.ai.generated.model.PlatformWorkflowItem;
import ru.live4code.social_network.ai.internal.platform_workflow.dao.PlatformWorkflowDao;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformWorkflowService {

    private final PlatformWorkflowDao platformWorkflowDao;

    public ResponseEntity<List<PlatformWorkflowItem>> getPlatformWorkflow() {
        List<PlatformWorkflowItem> platformWorkflowItems = platformWorkflowDao.getPlatformWorkflows();
        if (platformWorkflowItems.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(platformWorkflowItems);
    }

}
