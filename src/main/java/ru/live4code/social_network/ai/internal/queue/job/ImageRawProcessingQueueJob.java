package ru.live4code.social_network.ai.internal.queue.job;

import lombok.RequiredArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import ru.live4code.social_network.ai.internal.queue.service.ImageRawProcessingQueueService;
import ru.live4code.social_network.ai.utils.annotation.Executor;

@Executor(cronExpression = "45 */1 * * * ?")
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ImageRawProcessingQueueJob implements Job {

    private final ImageRawProcessingQueueService imageRawProcessingQueueService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        imageRawProcessingQueueService.processImagesInRawProcessingQueue();
    }

}
