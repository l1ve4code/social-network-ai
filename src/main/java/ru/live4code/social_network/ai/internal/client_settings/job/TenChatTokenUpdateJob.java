package ru.live4code.social_network.ai.internal.client_settings.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import ru.live4code.social_network.ai.internal.client_settings.service.TenChatAccessService;
import ru.live4code.social_network.ai.utils.annotation.Executor;

@Slf4j
@Executor(cronExpression = "0 0 */12 * * ?")
@RequiredArgsConstructor
public class TenChatTokenUpdateJob implements Job {

    private final TenChatAccessService tenChatAccessService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        tenChatAccessService.actualizeClientTenChatCredentials();
    }

}
