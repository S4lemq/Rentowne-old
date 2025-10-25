package pl.rentowne.lease_agreement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO dla podglądu zwrotu kaucji
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositReturnPreviewDto {

    /**
     * Kwota zwrotu kaucji w formacie czytelnym dla użytkownika
     * Np: "+ 200 zł (Musisz zwrócić tę kwotę najemcy)"
     * lub: "- 460 zł (Najemca musi dopłacić)"
     */
    private String depositReturnMessage;

    /**
     * Kwota zwrotu kaucji (wartość liczbowa)
     */
    private BigDecimal depositReturnAmount;

    /**
     * Wpłacona kaucja
     */
    private BigDecimal depositPaid;

    /**
     * Kwota za wodę
     */
    private BigDecimal waterAmount;

    /**
     * Kwota za prąd
     */
    private BigDecimal electricityAmount;

    /**
     * Kwota obciążenia
     */
    private BigDecimal chargeAmount;

    /**
     * Kwota obciążenia za gaz
     */
    private BigDecimal gasChargeAmount;
}

