package pl.rentowne.tenant.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.rentowne.settlement.model.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TenantSettlementRowDto {
    private Long id;
    private LocalDateTime settlementDate;
    private BigDecimal totalAmount;
    private SettlementStatus settlementStatus;
}
