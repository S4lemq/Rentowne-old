package pl.rentowne.lease_agreement;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO dla odpowiedzi kontroli rozliczenia gazu
 */
@Builder
record GasCheckResponseDto(
        /**
         * Suma z faktur za dany okres
         */
        BigDecimal invoicesTotal,

        /**
         * Suma pobranych zaliczek za dany okres
         */
        BigDecimal depositsTotal,

        /**
         * Saldo w formacie: "kwota zaliczki – kwota faktur = +/-saldo"
         */
        String balance
) {}
