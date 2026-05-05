package com.oggu.jms.rabbitmq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 *
 * Author : bhask
 * Created : 05-04-2026
 */
@Configuration
public class ExecutorConfig {

    @Bean(name = "rabbitExecutor")
    public Executor rabbitExecutor() {
        return new ThreadPoolExecutor(
                5,                      // core threads
                10,                     // max threads
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100), // queue for backpressure
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
