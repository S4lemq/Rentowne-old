package pl.rentowne.rental_document;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record DocumentSendRequest(
        @NotNull UUID requestId,
        @Min(0) long revision,
        @NotBlank String checksum,
        @NotBlank @Email @Size(max = 254) String to,
        @NotNull @Size(max = 20) List<@NotBlank @Email @Size(max = 254) String> cc,
        @AssertTrue boolean confirmed,
        boolean acknowledgeUnknown,
        boolean cancellation) {}
