package pl.rentowne.rental_document;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(DocumentProperties.class)
class DocumentConfiguration {
    @Bean
    Clock documentClock(DocumentProperties properties) {
        return Clock.system(properties.zone());
    }

    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
        // Unqualified @Scheduled jobs must not share the SMTP worker's single thread.
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("application-scheduler-");
        return scheduler;
    }

    @Bean
    ThreadPoolTaskScheduler documentScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("document-delivery-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(15);
        return scheduler;
    }
}
