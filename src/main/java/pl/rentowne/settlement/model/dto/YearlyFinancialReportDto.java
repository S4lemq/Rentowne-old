package pl.rentowne.settlement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearlyFinancialReportDto {

    private List<MonthlyFinancialReportDto> months; // lista miesięcznych raportów
    private BigDecimal yearlyTotalRevenue; // suma przychodów za rok/okres
    private BigDecimal yearlyTotalCosts; // suma kosztów za rok/okres
}

