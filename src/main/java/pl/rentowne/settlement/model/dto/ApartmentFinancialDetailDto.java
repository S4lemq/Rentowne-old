package pl.rentowne.settlement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentFinancialDetailDto {

    private Long apartmentId;
    private String apartmentName;
    private BigDecimal administrativeFee;
    private BigDecimal internet;
    private BigDecimal electricity;
    private BigDecimal gas;
    private BigDecimal tax;
    private BigDecimal revenue;
    private BigDecimal costs;
    private BigDecimal netIncome;
}

