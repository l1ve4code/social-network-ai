package ru.live4code.social_network.ai.internal.queue.job;

import lombok.RequiredArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import ru.live4code.social_network.ai.internal.queue.service.ThemeRawProcessingQueueService;
import ru.live4code.social_network.ai.utils.annotation.Executor;

@Executor(cronExpression = "30 */1 * * * ?")
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ThemeRawProcessingQueueJob implements Job {

    private final ThemeRawProcessingQueueService themeRawProcessingQueueService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        themeRawProcessingQueueService.processThemesInRawProcessingQueue();
    }

}
