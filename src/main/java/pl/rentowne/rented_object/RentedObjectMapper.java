package pl.rentowne.rented_object;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import pl.rentowne.settlement.Settlement;
import pl.rentowne.tenant.model.Tenant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static pl.rentowne.util.DateUtils.formatPolishDate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class RentedObjectMapper {

    static RentedObjectDto toDto(@NonNull final RentedObject rentedObject) {
        return new RentedObjectDto(
                rentedObject.getId(),
                rentedObject.getRentedObjectName(),
                List.of()
        );
    }

    static RentedObjectDto toDtoWithMeters(@NonNull final RentedObject rentedObject) {
        final List<RentedObjectDto.MeterDto> meterDtos = rentedObject.getMeters().stream()
                .map(meter -> new RentedObjectDto.MeterDto(
                        meter.getId(),
                        meter.getName(),
                        meter.getMeterNumber(),
                        meter.getMeterType()
                ))
                .collect(Collectors.toList());

        return new RentedObjectDto(
                rentedObject.getId(),
                rentedObject.getRentedObjectName(),
                meterDtos
        );
    }

    static RentedObjectDetailsDto toDetailsDto(@NonNull final Tenant tenant, final Settlement latestSettlement) {
        final Optional<Settlement> settlement = Optional.ofNullable(latestSettlement);

        final Long tenantId = tenant.getId();
        final Long rentedObjectId = tenant.getRentedObject().getId();
        final String tenantFullName = tenant.getFirstname() + " " + tenant.getLastname();
        final String rentedObjectName = tenant.getRentedObject().getRentedObjectName();

        final BigDecimal rentAmount = settlement.map(Settlement::getRentAmount).orElse(null);
        final BigDecimal internetFee = settlement.map(Settlement::getInternetFee).orElse(null);
        final BigDecimal gasDeposit = settlement.map(Settlement::getGasDeposit).orElse(null);
        final BigDecimal electricityAmount = settlement.map(Settlement::getElectricityAmount).orElse(null);
        final BigDecimal waterAmount = settlement.map(Settlement::getWaterAmount).orElse(null);
        final BigDecimal compensationAmount = settlement.map(Settlement::getCompensationAmount).orElse(null);
        final LocalDateTime lastSettlementDate = settlement.map(Settlement::getSettlementDate).orElse(null);

        final BigDecimal totalAmount = sumAmounts(electricityAmount, waterAmount, compensationAmount, internetFee, rentAmount, gasDeposit);
        final BigDecimal sum = sumAmounts(rentAmount, internetFee, gasDeposit, electricityAmount, waterAmount);
        final BigDecimal profit = calculateProfit(compensationAmount);
        final boolean hasCurrentMonthSettlement = settlement.map(RentedObjectMapper::checkCurrentMonthSettlement).orElse(false);

        return RentedObjectDetailsDto.builder()
                .id(rentedObjectId)
                .objectName(rentedObjectName)
                .tenantFullName(tenantFullName)
                .tenantId(tenantId)
                .lastSettlementDate(formatPolishDate(lastSettlementDate))
                .totalAmount(totalAmount.compareTo(BigDecimal.ZERO) > 0 ? totalAmount : null)
                .compensationAmount(compensationAmount)
                .sum(sum.compareTo(BigDecimal.ZERO) > 0 ? sum : null)
                .rentAmount(rentAmount)
                .internetFee(internetFee)
                .electricityAmount(electricityAmount)
                .gasDeposit(gasDeposit)
                .waterAmount(waterAmount)
                .profit(profit)
                .hasCurrentMonthSettlement(hasCurrentMonthSettlement)
                .build();
    }

    private static BigDecimal sumAmounts(final BigDecimal... amounts) {
        return Arrays.stream(amounts)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal calculateProfit(final BigDecimal compensationAmount) {
        if (compensationAmount == null) {
            return null;
        }
        return compensationAmount.multiply(new BigDecimal("0.915")).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean checkCurrentMonthSettlement(@NonNull final Settlement settlement) {
        final LocalDateTime settlementDate = settlement.getSettlementDate();
        if (settlementDate == null) {
            return false;
        }
        final LocalDateTime now = LocalDateTime.now();
        return settlementDate.getYear() == now.getYear()
                && settlementDate.getMonthValue() == now.getMonthValue();
    }
}
