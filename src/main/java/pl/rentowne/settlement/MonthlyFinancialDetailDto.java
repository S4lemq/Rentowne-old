package pl.rentowne.settlement;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
record MonthlyFinancialDetailDto(
        LocalDate month,
        MonthlyFinancialSummaryDto summary,
        List<ApartmentFinancialDetailDto> apartments
) {
}
