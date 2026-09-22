package org.netra.features.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Asynchronous execution configuration for push notification delivery.
 * Configures bounded concurrency and thread pooling to isolate third-party
 * push delivery network calls from core domain transaction threads.
 */
@Configuration
@EnableAsync
public class NotificationAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationAsyncConfig.class);

    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(250);
        executor.setThreadNamePrefix("notif-exec-");
        executor.setRejectedExecutionHandler((r, exec) -> {
            log.warn("Notification task executor bounded queue full; falling back to caller runs policy.");
            new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(r, exec);
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
