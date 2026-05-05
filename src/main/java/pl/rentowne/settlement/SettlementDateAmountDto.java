package pl.rentowne.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record SettlementDateAmountDto(Long rentedObjectId, LocalDateTime date, BigDecimal totalAmount) {
}
