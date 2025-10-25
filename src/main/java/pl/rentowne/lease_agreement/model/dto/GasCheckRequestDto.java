package pl.rentowne.lease_agreement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO dla żądania kontroli rozliczenia gazu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GasCheckRequestDto {

    private LocalDate dateFrom;
    private LocalDate dateTo;
}

