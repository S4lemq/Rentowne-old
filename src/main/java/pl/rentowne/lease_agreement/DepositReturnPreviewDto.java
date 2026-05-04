package pl.rentowne.lease_agreement;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO dla podglądu zwrotu kaucji
 */
@Builder
record DepositReturnPreviewDto(
    /**
     * Kwota zwrotu kaucji w formacie czytelnym dla użytkownika
     * Np: "+ 200 zł (Musisz zwrócić tę kwotę najemcy)"
     * lub: "- 460 zł (Najemca musi dopłacić)"
     */
    String depositReturnMessage,

    /**
     * Kwota zwrotu kaucji (wartość liczbowa)
     */
    BigDecimal depositReturnAmount,

    /**
     * Wpłacona kaucja
     */
    BigDecimal depositPaid,

    /**
     * Kwota za wodę
     */
    BigDecimal waterAmount,

    /**
     * Kwota za prąd
     */
    BigDecimal electricityAmount,

    /**
     * Kwota obciążenia
     */
    BigDecimal chargeAmount,

    /**
     * Kwota obciążenia za gaz
     */
    BigDecimal gasChargeAmount
) {}
