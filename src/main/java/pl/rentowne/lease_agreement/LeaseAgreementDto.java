package pl.rentowne.lease_agreement;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record LeaseAgreementDto(
        Long id,
        LocalDateTime startContractDate, //od kiedy najem
        LocalDateTime endContractDate, //do kiedy najem
        BigDecimal deposit, //kaucja
        BigDecimal depositPaid, //wpłacona kaucja
        int paymentDueDayOfMonth, //termin płatności
        BigDecimal rentAmount, //kwota czynszu
        BigDecimal compensationAmount, //kwota odstępnego
        BigDecimal internetFee, //kwota za internet
        BigDecimal gasDeposit, //opłata za gaz
        BigDecimal initialEnergyMeterReading, //początkowy odczyt energii
        BigDecimal initialWaterMeterReading, //początkowy odczyt wody
        BigDecimal initialGasMeterReading, //początkowy odczyt gazu
        LocalDateTime depositReturnDate, //data zwrotu kaucji
        BigDecimal returnedDepositAmount, //kwota zwróconej kaucji
        LocalDateTime contractSigningDate, //Data podpisania umowy
        boolean contractActive, //czy aktywna umowa
        String chargeDescription, //opis obciążenia przy zakończeniu umowy
        BigDecimal chargeAmount, //kwota obciążenia przy zakończeniu umowy
        BigDecimal gasChargeAmount //kwota obciążenia za gaz przy zakończeniu umowy
) {

    public LeaseAgreementDto(BigDecimal rentAmount, BigDecimal compensationAmount, BigDecimal internetFee, BigDecimal gasDeposit) {
        this(
                null,
                null,
                null,
                null,
                null,
                0,
                rentAmount,
                compensationAmount,
                internetFee,
                gasDeposit,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null
        );
    }

    public static LeaseAgreement asEntity(LeaseAgreementDto dto) {
        return LeaseAgreement.builder()
                .id(dto.id())
                .startContractDate(dto.startContractDate())
                .endContractDate(dto.endContractDate())
                .deposit(dto.deposit())
                .depositPaid(dto.depositPaid())
                .paymentDueDayOfMonth(dto.paymentDueDayOfMonth())
                .rentAmount(dto.rentAmount())
                .compensationAmount(dto.compensationAmount())
                .internetFee(dto.internetFee())
                .gasDeposit(dto.gasDeposit())
                .initialEnergyMeterReading(dto.initialEnergyMeterReading())
                .initialWaterMeterReading(dto.initialWaterMeterReading())
                .initialGasMeterReading(dto.initialGasMeterReading())
                .depositReturnDate(dto.depositReturnDate())
                .returnedDepositAmount(dto.returnedDepositAmount())
                .isContractActive(dto.contractActive())
                .contractSigningDate(dto.contractSigningDate())
                .chargeDescription(dto.chargeDescription())
                .chargeAmount(dto.chargeAmount())
                .gasChargeAmount(dto.gasChargeAmount())
                .build();
    }

    public static LeaseAgreementDto asDto(LeaseAgreement entity) {
        return LeaseAgreementDto.builder()
                .id(entity.getId())
                .startContractDate(entity.getStartContractDate())
                .endContractDate(entity.getEndContractDate())
                .deposit(entity.getDeposit())
                .depositPaid(entity.getDepositPaid())
                .paymentDueDayOfMonth(entity.getPaymentDueDayOfMonth())
                .rentAmount(entity.getRentAmount())
                .compensationAmount(entity.getCompensationAmount())
                .internetFee(entity.getInternetFee())
                .gasDeposit(entity.getGasDeposit())
                .initialEnergyMeterReading(entity.getInitialEnergyMeterReading())
                .initialWaterMeterReading(entity.getInitialWaterMeterReading())
                .initialGasMeterReading(entity.getInitialGasMeterReading())
                .depositReturnDate(entity.getDepositReturnDate())
                .returnedDepositAmount(entity.getReturnedDepositAmount())
                .contractActive(entity.isContractActive())
                .contractSigningDate(entity.getContractSigningDate())
                .chargeDescription(entity.getChargeDescription())
                .chargeAmount(entity.getChargeAmount())
                .gasChargeAmount(entity.getGasChargeAmount())
                .build();
    }

}
