package pl.rentowne.settlement.mapper;

import org.springframework.stereotype.Component;
import pl.rentowne.settlement.model.Settlement;
import pl.rentowne.settlement.model.dto.SettlementListDto;
import pl.rentowne.util.DateUtils;

import java.math.BigDecimal;

/**
 * Mapper dla Settlement
 */
@Component
public class SettlementMapper {

    /**
     * Sumuje wartości BigDecimal pomijając null
     */
    private static BigDecimal sumNonNull(BigDecimal... values) {
        BigDecimal sum = BigDecimal.ZERO;
        if (values == null) return sum;
        for (BigDecimal v : values) {
            if (v != null) {
                sum = sum.add(v);
            }
        }
        return sum;
    }

    /**
     * Konwertuje encję Settlement na DTO listy
     *
     * @param settlement encja Settlement
     * @return DTO dla listy
     */
    public SettlementListDto toListDto(Settlement settlement) {
        if (settlement == null) {
            return null;
        }

        // Oblicz sumę czynszu i mediów
        BigDecimal rentAndUtilities = sumNonNull(
                settlement.getRentAmount(),
                settlement.getInternetFee(),
                settlement.getGasDeposit(),
                settlement.getElectricityAmount(),
                settlement.getWaterAmount()
        );

        return new SettlementListDto(
                settlement.getId(),
                DateUtils.formatPolishDate(settlement.getSettlementDate()),
                settlement.getCompensationAmount(),
                settlement.getRentAmount(),
                settlement.getInternetFee(),
                settlement.getGasDeposit(),
                settlement.getElectricityAmount(),
                settlement.getWaterAmount(),
                settlement.getGasAmount(),
                settlement.getTotalAmount(),
                rentAndUtilities
        );
    }
}
