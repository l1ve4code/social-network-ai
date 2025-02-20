package ru.live4code.social_network.ai.internal.audience.job;

import lombok.RequiredArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import ru.live4code.social_network.ai.internal.audience.service.AudienceService;
import ru.live4code.social_network.ai.utils.annotation.Executor;

@Executor(cronExpression = "0 */15 1-6 * * ?")
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class AudienceJob implements Job {

    private final AudienceService audienceService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        audienceService.doDailySubscribesAndUnsubscribes();
    }

}
