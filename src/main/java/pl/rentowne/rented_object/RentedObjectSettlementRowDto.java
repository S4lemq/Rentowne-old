package pl.rentowne.rented_object;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
public class RentedObjectSettlementRowDto {
    private Long rentedObjectId;
    private String apartment;
    private String rentedObject;
    private String tenantName;
    private String tenantSurname;
    private LocalDateTime lastSettlementDate;
    private BigDecimal lastSettlementTotalAmount;
    private BigDecimal profit;

    public RentedObjectSettlementRowDto(Long rentedObjectId, String apartment, String rentedObject, String tenantName, String tenantSurname, LocalDateTime lastSettlementDate, BigDecimal lastSettlementTotalAmount, BigDecimal profit) {
        this.rentedObjectId = rentedObjectId;
        this.apartment = apartment;
        this.rentedObject = rentedObject;
        this.tenantName = tenantName;
        this.tenantSurname = tenantSurname;
        this.lastSettlementDate = lastSettlementDate;
        this.lastSettlementTotalAmount = lastSettlementTotalAmount;
        if (profit != null) {
            BigDecimal taxRate = new BigDecimal("0.085");
            BigDecimal netMultiplier = BigDecimal.ONE.subtract(taxRate);
            this.profit = profit.multiply(netMultiplier).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.profit = null;
        }
    }
}