package pl.rentowne.rental_document;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(DocumentProperties.class)
class DocumentConfiguration {
    @Bean
    Clock documentClock(DocumentProperties properties) {
        return Clock.system(properties.zone());
    }

}
