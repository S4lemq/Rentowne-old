package pl.rentowne.lease_agreement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.housing_provider.ProviderType;
import pl.rentowne.invoice.InvoiceRepository;
import pl.rentowne.meter.MeterType;
import pl.rentowne.meter_reading.MeterReadingRepository;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.settlement.Settlement;
import pl.rentowne.settlement.SettlementRepository;
import pl.rentowne.settlement.SettlementService;
import pl.rentowne.settlement.SettlementStatus;
import pl.rentowne.settlement.UtilityCostsDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class LeaseAgreementServiceImpl implements LeaseAgreementService {

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
        Long tenantId = leaseAgreement.getTenant().getId();

        // Pobierz ostatnie rozliczenie dla tego obiektu wynajmowanego i najemcy
        Settlement lastSettlement = settlementRepository.findLatestByRentedObjectId(rentedObjectId, tenantId);

        BigDecimal waterAmount;
        BigDecimal electricityAmount;
        BigDecimal waterConsumption;
        BigDecimal electricityConsumption;
        boolean hasPartialSettlement = false;

        // Sprawdź czy ostatnie rozliczenie było proporcjonalne (częściowe)
        if (lastSettlement != null && lastSettlement.getProrationDescription() != null) {
            // Było częściowe rozliczenie - pobierz dane z tego rozliczenia
            waterAmount = lastSettlement.getWaterAmount() != null ? lastSettlement.getWaterAmount() : BigDecimal.ZERO;
            electricityAmount = lastSettlement.getElectricityAmount() != null ? lastSettlement.getElectricityAmount() : BigDecimal.ZERO;
            waterConsumption = BigDecimal.ZERO;
            electricityConsumption = BigDecimal.ZERO;
            hasPartialSettlement = true;

            log.info("Znaleziono częściowe rozliczenie dla umowy {}: woda={}, prąd={}",
                    leaseAgreementId, waterAmount, electricityAmount);
        } else {
            // Nie było częściowego rozliczenia - przelicz koszty mediów
            UtilityCostsDto utilityCosts = settlementService.calculateUtilityCostsOnly(rentedObjectId);
            waterAmount = utilityCosts.waterAmount();
            electricityAmount = utilityCosts.electricityAmount();
            waterConsumption = utilityCosts.waterConsumption();
            electricityConsumption = utilityCosts.electricityConsumption();
        }

        return LeaseTerminationInfoDto.builder()
                .waterConsumption(waterConsumption)
                .waterAmount(waterAmount)
                .electricityConsumption(electricityConsumption)
                .electricityAmount(electricityAmount)
                .depositPaid(leaseAgreement.getDepositPaid())
                .hasPartialSettlement(hasPartialSettlement)
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
        Long tenantId = leaseAgreement.getTenant().getId();
        RentedObject rentedObject = leaseAgreement.getTenant().getRentedObject();
        Apartment apartment = rentedObject.getApartment();

        // Pobierz ostatnie rozliczenie dla tego obiektu wynajmowanego i najemcy
        Settlement lastSettlement = settlementRepository.findLatestByRentedObjectId(rentedObjectId, tenantId);

        BigDecimal electricityAmount;
        BigDecimal waterAmount;

        // Sprawdź czy ostatnie rozliczenie było proporcjonalne (częściowe)
        if (lastSettlement != null && lastSettlement.getProrationDescription() != null) {
            // Było częściowe rozliczenie - pobierz dane z tego rozliczenia
            waterAmount = lastSettlement.getWaterAmount() != null ? lastSettlement.getWaterAmount() : BigDecimal.ZERO;
            electricityAmount = lastSettlement.getElectricityAmount() != null ? lastSettlement.getElectricityAmount() : BigDecimal.ZERO;

            log.info("Zakończenie umowy {}: używam danych z częściowego rozliczenia - woda={}, prąd={}",
                    leaseAgreementId, waterAmount, electricityAmount);
        } else {
            // Nie było częściowego rozliczenia - przelicz koszty mediów
            UtilityCostsDto utilityCosts = settlementService.calculateUtilityCostsOnly(rentedObjectId);
            electricityAmount = utilityCosts.electricityAmount();
            waterAmount = utilityCosts.waterAmount();

            log.info("Zakończenie umowy {}: brak częściowego rozliczenia, przeliczono - woda={}, prąd={}",
                    leaseAgreementId, waterAmount, electricityAmount);
        }

        // Dodaj kwoty obciążeń
        BigDecimal chargeAmount = request.chargeAmount() != null ? request.chargeAmount() : BigDecimal.ZERO;
        BigDecimal gasChargeAmount = request.gasChargeAmount() != null ? request.gasChargeAmount() : BigDecimal.ZERO;

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
                .tenant(leaseAgreement.getTenant())
                .build();

        Long settlementId = settlementRepository.saveAndFlush(settlement).getId();

        // Zaktualizuj odczyty liczników jako rozliczone tylko jeśli nie było częściowego rozliczenia
        if (lastSettlement == null || lastSettlement.getProrationDescription() == null) {
            List<Long> electricityReadingIds = meterReadingRepository.findUnsettledReadingIdsByRentedObjectAndType(
                    rentedObjectId, MeterType.ELECTRIC);
            List<Long> waterReadingIds = meterReadingRepository.findUnsettledReadingIdsByRentedObjectAndType(
                    rentedObjectId, MeterType.WATER_COLD);

            List<Long> allMeterReadingIds = new ArrayList<>(electricityReadingIds);
            allMeterReadingIds.addAll(waterReadingIds);

            if (!allMeterReadingIds.isEmpty()) {
                meterReadingRepository.updateSettled(allMeterReadingIds, settlementId);
            }
        }

        // Zaktualizuj umowę
        leaseAgreement.setChargeDescription(request.chargeDescription());
        leaseAgreement.setChargeAmount(chargeAmount);
        leaseAgreement.setGasChargeAmount(gasChargeAmount);
        leaseAgreement.setReturnedDepositAmount(returnedDeposit);
        leaseAgreement.setContractActive(false);

        leaseAgreementRepository.save(leaseAgreement);

        // Ustaw active na false dla tenant
        leaseAgreement.getTenant().setActive(false);

        // Ustaw isRented na false w RentedObject i Apartment
        rentedObject.setRented(false);
        apartment.setRented(false);

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

        // Pobierz sumę z faktur za gaz w danym okresie
        BigDecimal invoicesTotal = invoiceRepository.sumGasAmountByApartmentAndDateRange(
                apartmentId,
                ProviderType.GAS,
                request.dateFrom(),
                request.dateTo()
        );

        // Konwertuj daty na LocalDateTime (początek pierwszego dnia i koniec ostatniego dnia)
        LocalDateTime dateTimeFrom = request.dateFrom().atStartOfDay();
        LocalDateTime dateTimeTo = request.dateTo().atTime(23, 59, 59);

        // Pobierz sumę zaliczek za gaz z Settlement w danym okresie dla całego mieszkania
        BigDecimal depositsTotal = settlementRepository.sumGasDepositByApartmentAndDateRange(
                apartmentId,
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
        Long tenantId = leaseAgreement.getTenant().getId();

        // Pobierz ostatnie rozliczenie dla tego obiektu wynajmowanego i najemcy
        Settlement lastSettlement = settlementRepository.findLatestByRentedObjectId(rentedObjectId, tenantId);

        BigDecimal electricityAmount;
        BigDecimal waterAmount;

        // Sprawdź czy ostatnie rozliczenie było proporcjonalne (częściowe)
        if (lastSettlement != null && lastSettlement.getProrationDescription() != null) {
            // Było częściowe rozliczenie - pobierz dane z tego rozliczenia
            waterAmount = lastSettlement.getWaterAmount() != null ? lastSettlement.getWaterAmount() : BigDecimal.ZERO;
            electricityAmount = lastSettlement.getElectricityAmount() != null ? lastSettlement.getElectricityAmount() : BigDecimal.ZERO;
        } else {
            // Nie było częściowego rozliczenia - przelicz koszty mediów (bez zapisu)
            UtilityCostsDto utilityCosts = settlementService.calculateUtilityCostsOnly(rentedObjectId);
            electricityAmount = utilityCosts.electricityAmount();
            waterAmount = utilityCosts.waterAmount();
        }

        // Pobierz kwoty obciążeń z requestu
        BigDecimal chargeAmount = request.chargeAmount() != null ? request.chargeAmount() : BigDecimal.ZERO;
        BigDecimal gasChargeAmount = request.gasChargeAmount() != null ? request.gasChargeAmount() : BigDecimal.ZERO;

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
