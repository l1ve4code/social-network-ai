package ru.live4code.social_network.ai.internal.queue.job;

import lombok.RequiredArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import ru.live4code.social_network.ai.internal.queue.service.PostRawProcessingQueueService;
import ru.live4code.social_network.ai.utils.annotation.Executor;

@Executor(cronExpression = "0 * * * * ?")
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class PostRawProcessingQueueJob implements Job {

    private final PostRawProcessingQueueService postRawProcessingQueueService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        postRawProcessingQueueService.processPostsInRawProcessingQueue();
    }

}
