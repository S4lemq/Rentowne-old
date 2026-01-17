package pl.rentowne.rented_object;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

record RentedObjectSettlementRowDto(
        Long rentedObjectId,
        String apartment,
        String rentedObject,
        String tenantName,
        String tenantSurname,
        LocalDateTime lastSettlementDate,
        BigDecimal lastSettlementTotalAmount,
        BigDecimal profit
) {
    private static final BigDecimal TAX_RATE = new BigDecimal("0.085");
    private static final BigDecimal NET_MULTIPLIER = BigDecimal.ONE.subtract(TAX_RATE);

    public RentedObjectSettlementRowDto {
        profit = computeNetProfit(profit);
    }

    private static BigDecimal computeNetProfit(BigDecimal profit) {
        if (profit == null) {
            return null;
        }
        return profit
                .multiply(NET_MULTIPLIER)
                .setScale(2, RoundingMode.HALF_UP);
    }
}