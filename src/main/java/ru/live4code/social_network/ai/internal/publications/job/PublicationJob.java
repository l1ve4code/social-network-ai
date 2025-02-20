package ru.live4code.social_network.ai.internal.publications.job;

import lombok.RequiredArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import ru.live4code.social_network.ai.internal.publications.service.PublicationService;
import ru.live4code.social_network.ai.utils.annotation.Executor;

@Executor(cronExpression = "15 * * * * ?")
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class PublicationJob implements Job {

    private final PublicationService publicationService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        publicationService.publishClientPosts();
    }

}
