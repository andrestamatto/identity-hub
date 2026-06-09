package br.dev.andrestamatto.identityhub.infrastructure.messaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean
    public AsyncTaskExecutor notificationTaskExecutor() {
        var executor = new SimpleAsyncTaskExecutor("identityhub-notification-");
        executor.setVirtualThreads(true);
        return executor;
    }

}
