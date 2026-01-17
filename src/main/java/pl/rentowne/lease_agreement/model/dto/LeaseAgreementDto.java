package pl.rentowne.lease_agreement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.rentowne.lease_agreement.model.LeaseAgreement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaseAgreementDto {
    private Long id;
    private LocalDateTime startContractDate; //od kiedy najem
    private LocalDateTime endContractDate; //do kiedy najem
    private BigDecimal deposit; //kaucja
    private BigDecimal depositPaid; //wpłacona kaucja
    private int paymentDueDayOfMonth; //termin płatności
    private BigDecimal rentAmount; //kwota czynszu
    private BigDecimal compensationAmount; //kwota odstępnego
    private BigDecimal internetFee; //kwota za internet
    private BigDecimal gasDeposit; //opłata za gaz
    private BigDecimal initialEnergyMeterReading; //początkowy odczyt energii
    private BigDecimal initialWaterMeterReading; //początkowy odczyt wody
    private BigDecimal initialGasMeterReading; //początkowy odczyt gazu
    private LocalDateTime depositReturnDate; //data zwrotu kaucji
    private BigDecimal returnedDepositAmount; //kwota zwróconej kaucji
    private LocalDateTime contractSigningDate; //Data podpisania umowy
    private boolean contractActive; //czy aktywna umowa
    private String chargeDescription; //opis obciążenia przy zakończeniu umowy
    private BigDecimal chargeAmount; //kwota obciążenia przy zakończeniu umowy
    private BigDecimal gasChargeAmount; //kwota obciążenia za gaz przy zakończeniu umowy

    public LeaseAgreementDto(BigDecimal rentAmount, BigDecimal compensationAmount, BigDecimal internetFee, BigDecimal gasDeposit) {
        this.rentAmount = rentAmount;
        this.compensationAmount = compensationAmount;
        this.internetFee = internetFee;
        this.gasDeposit = gasDeposit;
    }

    public static LeaseAgreement asEntity(LeaseAgreementDto dto) {
        return LeaseAgreement.builder()
                .id(dto.id)
                .startContractDate(dto.startContractDate)
                .endContractDate(dto.endContractDate)
                .deposit(dto.deposit)
                .depositPaid(dto.depositPaid)
                .paymentDueDayOfMonth(dto.paymentDueDayOfMonth)
                .rentAmount(dto.rentAmount)
                .compensationAmount(dto.compensationAmount)
                .internetFee(dto.internetFee)
                .gasDeposit(dto.gasDeposit)
                .initialEnergyMeterReading(dto.initialEnergyMeterReading)
                .initialWaterMeterReading(dto.initialWaterMeterReading)
                .initialGasMeterReading(dto.initialGasMeterReading)
                .depositReturnDate(dto.depositReturnDate)
                .returnedDepositAmount(dto.returnedDepositAmount)
                .isContractActive(dto.contractActive)
                .contractSigningDate(dto.contractSigningDate)
                .chargeDescription(dto.chargeDescription)
                .chargeAmount(dto.chargeAmount)
                .gasChargeAmount(dto.gasChargeAmount)
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
