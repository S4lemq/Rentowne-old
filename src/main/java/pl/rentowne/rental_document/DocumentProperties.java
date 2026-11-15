package pl.rentowne.rental_document;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

@Validated
@ConfigurationProperties(prefix = "application.documents")
public record DocumentProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("data/rentalDocuments") @NotBlank String directory,
        @DefaultValue("Poznań") @NotBlank String issueCity,
        @DefaultValue("Europe/Warsaw") ZoneId zone,
        @DefaultValue("Rentowne <rentowneApp@gmail.com>") @NotBlank String from,
        @DefaultValue("rentowneApp@gmail.com") @NotBlank String replyTo
) {}
