package pl.rentowne.lease_agreement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.housing_provider.model.ProviderType;
import pl.rentowne.invoice.repository.InvoiceRepository;
import pl.rentowne.lease_agreement.model.LeaseAgreement;
import pl.rentowne.lease_agreement.model.dto.GasCheckRequestDto;
import pl.rentowne.lease_agreement.model.dto.GasCheckResponseDto;
import pl.rentowne.lease_agreement.model.dto.LeaseTerminationInfoDto;
import pl.rentowne.lease_agreement.model.dto.TerminateLeaseRequestDto;
import pl.rentowne.lease_agreement.repository.LeaseAgreementRepository;
import pl.rentowne.lease_agreement.service.LeaseAgreementService;
import pl.rentowne.meter.model.MeterType;
import pl.rentowne.meter_reading.repository.MeterReadingRepository;
import pl.rentowne.rented_object.model.RentedObject;
import pl.rentowne.settlement.model.Settlement;
import pl.rentowne.settlement.model.SettlementStatus;
import pl.rentowne.settlement.model.dto.UtilityCostsDto;
import pl.rentowne.settlement.repository.SettlementRepository;
import pl.rentowne.settlement.service.SettlementService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseAgreementServiceImpl implements LeaseAgreementService {

    private final LeaseAgreementRepository leaseAgreementRepository;
    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional(readOnly = true)
    public LeaseTerminationInfoDto getLeaseTerminationInfo(Long leaseAgreementId) {
        LeaseAgreement leaseAgreement = leaseAgreementRepository.findById(leaseAgreementId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono umowy o id: " + leaseAgreementId));

        Long rentedObjectId = leaseAgreement.getTenant().getRentedObject().getId();

        // Oblicz koszty mediów bez zapisu
        UtilityCostsDto utilityCosts = settlementService.calculateUtilityCostsOnly(rentedObjectId);

        return LeaseTerminationInfoDto.builder()
                .waterConsumption(utilityCosts.getWaterConsumption())
                .waterAmount(utilityCosts.getWaterAmount())
                .electricityConsumption(utilityCosts.getElectricityConsumption())
                .electricityAmount(utilityCosts.getElectricityAmount())
                .depositPaid(leaseAgreement.getDepositPaid())
                .build();
    }

    @Override
    @Transactional
    public void terminateLease(Long leaseAgreementId, TerminateLeaseRequestDto request) {
        LeaseAgreement leaseAgreement = leaseAgreementRepository.findById(leaseAgreementId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono umowy o id: " + leaseAgreementId));

        if (!leaseAgreement.isContractActive()) {
            throw new IllegalStateException("Umowa o id: " + leaseAgreementId + " jest już zakończona");
        }

        Long rentedObjectId = leaseAgreement.getTenant().getRentedObject().getId();

        // Oblicz koszty mediów
        UtilityCostsDto utilityCosts = settlementService.calculateUtilityCostsOnly(rentedObjectId);

        BigDecimal electricityAmount = utilityCosts.getElectricityAmount();
        BigDecimal waterAmount = utilityCosts.getWaterAmount();

        // Dodaj kwoty obciążeń
        BigDecimal chargeAmount = request.getChargeAmount() != null ? request.getChargeAmount() : BigDecimal.ZERO;
        BigDecimal gasChargeAmount = request.getGasChargeAmount() != null ? request.getGasChargeAmount() : BigDecimal.ZERO;

        // Oblicz zwróconą kaucję
        BigDecimal returnedDeposit = leaseAgreement.getDepositPaid()
                .subtract(waterAmount)
                .subtract(electricityAmount)
                .subtract(chargeAmount)
                .subtract(gasChargeAmount);

        // Zapisz Settlement
        Settlement settlement = Settlement.builder()
                .electricityAmount(electricityAmount)
                .waterAmount(waterAmount)
                .gasAmount(BigDecimal.ZERO)
                .totalAmount(electricityAmount.add(waterAmount))
                .compensationAmount(BigDecimal.ZERO)
                .rentAmount(BigDecimal.ZERO)
                .internetFee(BigDecimal.ZERO)
                .gasDeposit(BigDecimal.ZERO)
                .settlementDate(LocalDateTime.now())
                .rentedObject(RentedObject.builder().id(rentedObjectId).build())
                .settlementStatus(SettlementStatus.TO_PAY)
                .build();

        Long settlementId = settlementRepository.saveAndFlush(settlement).getId();

        // Zaktualizuj odczyty liczników jako rozliczone
        List<Long> electricityReadingIds = meterReadingRepository.findUnsettledReadingIdsByRentedObjectAndType(
                rentedObjectId, MeterType.ELECTRIC);
        List<Long> waterReadingIds = meterReadingRepository.findUnsettledReadingIdsByRentedObjectAndType(
                rentedObjectId, MeterType.WATER_COLD);

        List<Long> allMeterReadingIds = new ArrayList<>(electricityReadingIds);
        allMeterReadingIds.addAll(waterReadingIds);

        if (!allMeterReadingIds.isEmpty()) {
            meterReadingRepository.updateSettled(allMeterReadingIds, settlementId);
        }

        // Zaktualizuj umowę
        leaseAgreement.setChargeDescription(request.getChargeDescription());
        leaseAgreement.setChargeAmount(chargeAmount);
        leaseAgreement.setGasChargeAmount(gasChargeAmount);
        leaseAgreement.setReturnedDepositAmount(returnedDeposit);
        leaseAgreement.setDepositReturnDate(LocalDateTime.now());
        leaseAgreement.setContractActive(false);

        leaseAgreementRepository.save(leaseAgreement);

        log.info("Zakończono umowę o id: {}. Zwrócona kaucja: {}", leaseAgreementId, returnedDeposit);
    }

    @Override
    @Transactional(readOnly = true)
    public GasCheckResponseDto checkGasBalance(Long leaseAgreementId, GasCheckRequestDto request) {
        // Pobierz umowę najmu
        LeaseAgreement leaseAgreement = leaseAgreementRepository.findById(leaseAgreementId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono umowy o id: " + leaseAgreementId));

        // Pobierz mieszkanie z umowy
        Long apartmentId = leaseAgreement.getTenant().getRentedObject().getApartment().getId();
        Long rentedObjectId = leaseAgreement.getTenant().getRentedObject().getId();

        // Pobierz sumę z faktur za gaz w danym okresie
        BigDecimal invoicesTotal = invoiceRepository.sumGasAmountByApartmentAndDateRange(
                apartmentId,
                ProviderType.GAS,
                request.getDateFrom(),
                request.getDateTo()
        );

        // Konwertuj daty na LocalDateTime (początek pierwszego dnia i koniec ostatniego dnia)
        LocalDateTime dateTimeFrom = request.getDateFrom().atStartOfDay();
        LocalDateTime dateTimeTo = request.getDateTo().atTime(23, 59, 59);

        // Pobierz sumę zaliczek za gaz z Settlement w danym okresie
        BigDecimal depositsTotal = settlementRepository.sumGasDepositByRentedObjectAndDateRange(
                rentedObjectId,
                dateTimeFrom,
                dateTimeTo
        );

        // Oblicz saldo: zaliczki - faktury
        BigDecimal balance = depositsTotal.subtract(invoicesTotal);

        // Sformatuj string salda w formacie: "kwota zaliczki – kwota faktur = +/-saldo"
        String balanceString = String.format("%s zł – %s zł = %s%s zł",
                formatAmount(depositsTotal),
                formatAmount(invoicesTotal),
                balance.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
                formatAmount(balance)
        );

        log.info("Sprawdzono rozliczenie gazu dla umowy {}: faktury={}, zaliczki={}, saldo={}",
                leaseAgreementId, invoicesTotal, depositsTotal, balance);

        return GasCheckResponseDto.builder()
                .invoicesTotal(invoicesTotal)
                .depositsTotal(depositsTotal)
                .balance(balanceString)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String previewDepositReturn(Long leaseAgreementId, TerminateLeaseRequestDto request) {
        // Pobierz umowę najmu
        LeaseAgreement leaseAgreement = leaseAgreementRepository.findById(leaseAgreementId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono umowy o id: " + leaseAgreementId));

        Long rentedObjectId = leaseAgreement.getTenant().getRentedObject().getId();

        // Oblicz koszty mediów (bez zapisu)
        UtilityCostsDto utilityCosts = settlementService.calculateUtilityCostsOnly(rentedObjectId);

        BigDecimal electricityAmount = utilityCosts.getElectricityAmount();
        BigDecimal waterAmount = utilityCosts.getWaterAmount();

        // Pobierz kwoty obciążeń z requestu
        BigDecimal chargeAmount = request.getChargeAmount() != null ? request.getChargeAmount() : BigDecimal.ZERO;
        BigDecimal gasChargeAmount = request.getGasChargeAmount() != null ? request.getGasChargeAmount() : BigDecimal.ZERO;

        // Oblicz kwotę zwrotu kaucji
        BigDecimal depositReturnAmount = leaseAgreement.getDepositPaid()
                .subtract(waterAmount)
                .subtract(electricityAmount)
                .subtract(chargeAmount)
                .subtract(gasChargeAmount);

        // Sformatuj komunikat w zależności od tego czy kwota jest dodatnia czy ujemna
        String message;
        if (depositReturnAmount.compareTo(BigDecimal.ZERO) > 0) {
            message = String.format("+ %s zł (Musisz zwrócić tę kwotę najemcy)", formatAmount(depositReturnAmount));
        } else if (depositReturnAmount.compareTo(BigDecimal.ZERO) < 0) {
            message = String.format("- %s zł (Najemca musi dopłacić)", formatAmount(depositReturnAmount.abs()));
        } else {
            message = "0 zł (Rozliczenie wychodzi na zero)";
        }

        log.info("Podgląd zwrotu kaucji dla umowy {}: wpłacona={}, woda={}, prąd={}, obciążenie={}, gaz={}, zwrot={}",
                leaseAgreementId, leaseAgreement.getDepositPaid(), waterAmount, electricityAmount,
                chargeAmount, gasChargeAmount, depositReturnAmount);

        return message;
    }

    /**
     * Formatuje kwotę do wyświetlenia (usuwa niepotrzebne zera po przecinku)
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.stripTrailingZeros().toPlainString();
    }
}
