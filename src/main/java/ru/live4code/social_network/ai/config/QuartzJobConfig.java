package ru.live4code.social_network.ai.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.context.annotation.Configuration;
import ru.live4code.social_network.ai.utils.annotation.Executor;

import java.util.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuartzJobConfig {

    private static final String JOB_TAG = "DEFAULT";

    private final Scheduler scheduler;

    @Resource
    private final List<Job> availableJobs;

    @PostConstruct
    public void setup() {
        deleteNonExistsJobs();
        initializeJobs();
    }

    private void initializeJobs() {
        for (Job job : availableJobs) {
            var executorAnnotation = job.getClass().getAnnotation(Executor.class);
            if (executorAnnotation == null) {
                throw new IllegalArgumentException("Annotation @Executor wasn't added!");
            }

            String name = job.getClass().getSimpleName();
            String cron = executorAnnotation.cronExpression();

            JobDetail jobDetail = JobBuilder.newJob(job.getClass())
                    .withIdentity(name)
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(name)
                    .startNow()
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                    .build();

            try {
                if (scheduler.checkExists(new JobKey(name))) {
                    TriggerKey existsTriggerKey = new TriggerKey(name);
                    CronTrigger existsTrigger = (CronTrigger) scheduler.getTrigger(existsTriggerKey);
                    String existsCronExpression = existsTrigger.getCronExpression();
                    if (!existsCronExpression.equals(cron)) {
                        scheduler.rescheduleJob(existsTriggerKey, trigger);
                        log.warn("Job: {} was rescheduled from: {}, to: {}!", name, existsCronExpression, cron);
                    }
                    continue;
                }
                scheduler.scheduleJob(jobDetail, trigger);
                log.info("{} job was created!", name);
            } catch (SchedulerException exception) {
                throw new IllegalArgumentException(exception.getMessage());
            }
        }
        log.info("{} job(s) were created!", availableJobs.size());
    }

    private void deleteNonExistsJobs() {
        Set<JobKey> inDatabaseJobs = getDatabaseJobs();
        List<JobKey> inCodeJobs = availableJobs.stream()
                .filter(item -> Objects.nonNull(item.getClass().getAnnotation(Executor.class)))
                .map(item -> new JobKey(item.getClass().getSimpleName(), JOB_TAG))
                .toList();

        var jobsToDelete = new ArrayList<>(inDatabaseJobs);
        jobsToDelete.removeAll(inCodeJobs);

        try {
            scheduler.deleteJobs(jobsToDelete);
            log.info("{} non exists job(s) were deleted!", jobsToDelete.size());
        } catch (SchedulerException exception) {
            throw new IllegalArgumentException("Can't delete passed job(s)!");
        }
    }

    private Set<JobKey> getDatabaseJobs() {
        try {
            return scheduler.getJobKeys(GroupMatcher.jobGroupEquals(JOB_TAG));
        } catch (SchedulerException exception) {
            throw new NoSuchElementException("Can't find actual job(s)!");
        }
    }

}
