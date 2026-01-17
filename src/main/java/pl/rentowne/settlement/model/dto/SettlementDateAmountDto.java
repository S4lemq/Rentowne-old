package pl.rentowne.settlement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SettlementDateAmountDto {
    private Long rentedObjectId;
    private LocalDateTime date;
    private BigDecimal totalAmount;
}
