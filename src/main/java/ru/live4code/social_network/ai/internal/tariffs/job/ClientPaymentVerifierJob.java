package ru.live4code.social_network.ai.internal.tariffs.job;

import lombok.RequiredArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import ru.live4code.social_network.ai.internal.tariffs.service.ClientPaymentVerifierService;
import ru.live4code.social_network.ai.utils.annotation.Executor;

@Executor(cronExpression = "0 * * * * ?")
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ClientPaymentVerifierJob implements Job {

    private final ClientPaymentVerifierService clientPaymentVerifierService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        clientPaymentVerifierService.verifyClientPayments();
    }

}
