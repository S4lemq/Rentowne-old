package pl.rentowne.rented_object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dla odpowiedzi sprawdzenia proporcjonalności rozliczenia
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProrationCheckResponseDto {

    /**
     * Czy należy zasugerować rozliczenie niepełnego miesiąca
     */
    private boolean shouldSuggestProration;

    /**
     * Komunikat dla użytkownika (null jeśli shouldSuggestProration = false)
     */
    private String message;

    /**
     * Data rozpoczęcia umowy
     */
    private String contractStartDate;

    /**
     * Data rozliczenia wybrana przez użytkownika
     */
    private String settlementDate;
}

