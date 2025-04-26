package com.bishop.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.RejectedExecutionException;

@Configuration
@EnableAsync
public class AsyncConfigs implements AsyncConfigurer {
    private static final Logger log = LoggerFactory.getLogger(AsyncConfigs.class);

    // Provide a custom ThreadPoolTaskExecutor for @Async methods
    @Override
    @Bean(name = "taskExecutor")
    @Primary
    public TaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(50); // Number of threads to keep in the pool
        executor.setMaxPoolSize(150); // Maximum number of allowed threads
        executor.setQueueCapacity(10000); // Size of the queue before rejecting tasks
        executor.setKeepAliveSeconds(60); // Time to keep idle threads alive
        executor.setThreadNamePrefix("AsyncExecutor-"); // Prefix for thread names

        // Custom handler for rejected tasks
        executor.setRejectedExecutionHandler((r, ex) -> {
            log.error("Task rejected from AsyncExecutor: {}", r.toString());
            throw new RejectedExecutionException("Task rejected due to overload: " + ex);
        });

        // Ensure graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    // Provide custom exception handling for uncaught async errors
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> log.error(
                "Uncaught async error in method '{}' with params {}: {}",
                method.getName(),
                Arrays.toString(params),
                throwable.getMessage(),
                throwable
        );
    }
}
