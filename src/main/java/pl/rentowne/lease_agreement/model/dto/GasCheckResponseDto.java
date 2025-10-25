package pl.rentowne.lease_agreement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO dla odpowiedzi kontroli rozliczenia gazu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GasCheckResponseDto {

    /**
     * Suma z faktur za dany okres
     */
    private BigDecimal invoicesTotal;

    /**
     * Suma pobranych zaliczek za dany okres
     */
    private BigDecimal depositsTotal;

    /**
     * Saldo w formacie: "kwota zaliczki – kwota faktur = +/-saldo"
     */
    private String balance;
}

