package pl.rentowne.settlement;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
record SettlementStats(List<String> label, List<BigDecimal> totalAmount, List<Long> tenant) {
}
