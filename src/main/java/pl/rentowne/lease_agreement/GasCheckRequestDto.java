package pl.rentowne.lease_agreement;

import lombok.Builder;

import java.time.LocalDate;

/**
 * DTO dla żądania kontroli rozliczenia gazu
 */
@Builder
record GasCheckRequestDto(
        LocalDate dateFrom,
        LocalDate dateTo
) {}
