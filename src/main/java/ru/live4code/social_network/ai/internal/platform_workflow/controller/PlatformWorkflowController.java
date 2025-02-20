package ru.live4code.social_network.ai.internal.platform_workflow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.live4code.social_network.ai.generated.api.PlatformWorkflowApi;
import ru.live4code.social_network.ai.generated.model.PlatformWorkflowItem;
import ru.live4code.social_network.ai.internal.platform_workflow.service.PlatformWorkflowService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlatformWorkflowController implements PlatformWorkflowApi {

    private final PlatformWorkflowService platformWorkflowService;

    @Override
    public ResponseEntity<List<PlatformWorkflowItem>> getPlatformWorkflow() {
        return platformWorkflowService.getPlatformWorkflow();
    }

}
