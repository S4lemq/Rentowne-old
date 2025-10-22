package pl.rentowne.settlement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.apartment.service.ApartmentService;
import pl.rentowne.housing_provider.model.HousingProvider;
import pl.rentowne.housing_provider.model.ProviderType;
import pl.rentowne.housing_provider.repository.HousingProviderRepository;
import pl.rentowne.lease_agreement.model.dto.LeaseAgreementDto;
import pl.rentowne.lease_agreement.repository.LeaseAgreementRepository;
import pl.rentowne.meter.model.MeterType;
import pl.rentowne.meter.repository.MeterRepository;
import pl.rentowne.meter_reading.model.dto.MeterReadingDto;
import pl.rentowne.meter_reading.repository.MeterReadingRepository;
import pl.rentowne.provider_field.model.BillingMethod;
import pl.rentowne.provider_field.model.ProviderField;
import pl.rentowne.rented_object.model.RentedObject;
import pl.rentowne.rented_object.model.dto.CalculateRequestDto;
import pl.rentowne.rented_object.repository.RentedObjectRepository;
import pl.rentowne.settlement.mapper.SettlementMapper;
import pl.rentowne.settlement.model.Settlement;
import pl.rentowne.settlement.model.SettlementStatus;
import pl.rentowne.settlement.model.dto.SettlementListDto;
import pl.rentowne.settlement.repository.SettlementRepository;
import pl.rentowne.settlement.service.SettlementReportService;
import pl.rentowne.settlement.service.SettlementService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {
    private final SettlementRepository settlementRepository;
    private final HousingProviderRepository housingProviderRepository;
    private final MeterRepository meterRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final LeaseAgreementRepository leaseAgreementRepository;
    private final RentedObjectRepository rentedObjectRepository;
    private final ApartmentService apartmentService;
    private final SettlementReportService reportService;
    private final SettlementMapper settlementMapper;

    @Transactional
    @Override
    public void calculate(Long rentedObjectId, CalculateRequestDto dto) {
        // Inicjalizacja raportu
        String fileNameData = rentedObjectRepository.getSettlementFileNameData(rentedObjectId);
        reportService.initializeReport(fileNameData, dto.getSettlementDate());

        ElectricityCalcResult elecCtx = null;
        try {
            List<HousingProvider> providerWithProviderFields =
                    housingProviderRepository.findHousingProviderWithProviderFieldsByRentedObjectId(rentedObjectId);
            LeaseAgreementDto leaseAgreement = leaseAgreementRepository.getData(rentedObjectId);
            int rentedObjectsInApartment = apartmentService.getCountOfRentedObjectsInApartment(rentedObjectId);

            reportService.reportBasicInfo(rentedObjectId, dto, rentedObjectsInApartment);

            List<Long> allMeterReadingIds = new ArrayList<>();
            BigDecimal electricResult = BigDecimal.ZERO;
            BigDecimal gasResult;
            BigDecimal waterResult = BigDecimal.ZERO;

            if (dto.isElectricityIncluded()) {
                reportService.startElectricitySection();
                elecCtx = this.calculateElectricityCost(
                        providerWithProviderFields, rentedObjectId, allMeterReadingIds);
                electricResult = (elecCtx != null) ? elecCtx.amount() : BigDecimal.ZERO;
                reportService.reportElectricityResult(electricResult);
            }

            if (dto.isWaterIncluded()) {
                reportService.startWaterSection();
                waterResult = this.calculateWaterCost(
                        providerWithProviderFields, rentedObjectId, waterResult, allMeterReadingIds, leaseAgreement);
                reportService.reportWaterResult(waterResult);
            }

            gasResult = leaseAgreement.getGasDeposit() != null ? leaseAgreement.getGasDeposit() : BigDecimal.ZERO;

            BigDecimal totalSum = electricResult.add(waterResult)
                    .add(leaseAgreement.getCompensationAmount())
                    .add(leaseAgreement.getInternetFee())
                    .add(leaseAgreement.getRentAmount());

            // Logi dla standardowych logów aplikacji
            log.info("Rozliczenie obiektu o id: {}", rentedObjectId);
            log.info("Rozliczano: wodę-{}, prąd-{}", dto.isWaterIncluded(), dto.isElectricityIncluded());
            log.info("Kwota za prąd: {}", electricResult);
            log.info("Kwota za wodę: {}", waterResult);
            log.info("Kwota za gaz: {}", gasResult);
            log.info("Kwota odstępnego: {}", leaseAgreement.getCompensationAmount());
            log.info("Kwota za internet: {}", leaseAgreement.getInternetFee());
            log.info("Kwota czynszu: {}", leaseAgreement.getRentAmount());
            log.info("Kwota całkowita: {}", totalSum);

            reportService.reportSummary(electricResult, waterResult, gasResult, leaseAgreement, totalSum);

            Settlement settlement = Settlement.builder()
                    .electricityAmount(electricResult)
                    .waterAmount(waterResult)
                    .gasAmount(gasResult)
                    .totalAmount(totalSum)
                    .compensationAmount(leaseAgreement.getCompensationAmount())
                    .rentAmount(leaseAgreement.getRentAmount())
                    .internetFee(leaseAgreement.getInternetFee())
                    .gasDeposit(leaseAgreement.getGasDeposit())
                    .settlementDate(dto.getSettlementDate())
                    .rentedObject(RentedObject.builder().id(rentedObjectId).build())
                    .settlementStatus(SettlementStatus.TO_PAY)
                    .build();

            Long settlementId = settlementRepository.saveAndFlush(settlement).getId();
            reportService.reportSettlementCreated(settlementId);

            if (!allMeterReadingIds.isEmpty()) {
                meterReadingRepository.updateSettled(allMeterReadingIds, settlementId);
                reportService.reportSettledMeterReadings(allMeterReadingIds);
            }
            log.info("W następujących odczytach liczników ustawiono odczyty na rozliczone: {}", allMeterReadingIds);

            // Domknięcie cyklu na poziomie mieszkania (tylko elektryka)
            if (elecCtx != null && elecCtx.apartmentId() != null) {
                long pending = meterReadingRepository.countUnclosedAndUnsettledInApartment(
                        elecCtx.apartmentId(), MeterType.ELECTRIC);

                if (pending == 0 && elecCtx.apartmentSnapshotIds() != null && !elecCtx.apartmentSnapshotIds().isEmpty()) {
                    meterReadingRepository.markApartmentSettled(elecCtx.apartmentSnapshotIds());
                    log.debug("[ELEKTRYCZNOŚĆ][CLOSE_CYCLE] aptId={} closed {} readings: {}",
                            elecCtx.apartmentId(), elecCtx.apartmentSnapshotIds().size(), elecCtx.apartmentSnapshotIds());
                    reportService.reportCycleClosure(elecCtx.apartmentId(), elecCtx.apartmentSnapshotIds().size());
                }
            }

            rentedObjectRepository.updateSettlement(rentedObjectId, totalSum, dto.getSettlementDate());

            reportService.reportSuccess();

        } catch (Exception e) {
            reportService.reportError(e);
            log.error("Błąd podczas rozliczenia obiektu {}", rentedObjectId, e);
            throw e;
        } finally {
            reportService.closeReport();
        }
    }

    @Override
    @Transactional
    public void deleteCalculate(final Long settlementId) {

        // 1) Ustal mieszkanie i datę rozliczenia (identyfikator „cyklu”)
        Long apartmentId = settlementRepository.getApartmentIdBySettlementId(settlementId);
        LocalDateTime settlementDate = settlementRepository.getSettlementDateById(settlementId);

        // 2) Zbierz wszystkie rozliczenia tego samego mieszkania i tej samej daty (cały cykl)
        List<Long> cycleSettlementIds = settlementRepository.findSettlementIdsByApartmentAndDate(apartmentId, settlementDate);

        // 3) Cofnij znacznik apartmentSettled dla wszystkich odczytów tego cyklu (ELEKTRYKA)
        if (!cycleSettlementIds.isEmpty()) {
            meterReadingRepository.unsetApartmentSettledBySettlementIdsAndType(cycleSettlementIds, MeterType.ELECTRIC);
            log.debug("[DELETE][CYCLE-OPEN] aptId={} date={} cycleSettlements={} -> apartmentSettled=false",
                    apartmentId, settlementDate, cycleSettlementIds);
        }

        // 4) Cofnij powiązania tylko dla USUWANEGO rozliczenia (settled=false, settlement_id=null)
        meterReadingRepository.updateSettledToFalseBySettlementId(settlementId);

        // 5) Wyczyść 'lastSettlement*' dla obiektu tego rozliczenia
        rentedObjectRepository.clearLastSettlementData(settlementId);

        // 6) Usuń samo rozliczenie
        settlementRepository.deleteById(settlementId);
    }


    private Optional<HousingProvider> findProvider(List<HousingProvider> providers, ProviderType type) {
        return providers.stream()
                .filter(dto -> type.equals(dto.getType()))
                .findFirst();
    }

    // ZWRACA kontekst (kwota dla bieżącej kawalerki + snapshot mieszkania do domknięcia cyklu)
    private ElectricityCalcResult calculateElectricityCost(List<HousingProvider> providerWithProviderFields,
                                                           Long rentedObjectId,
                                                           List<Long> allMeterReadingIds) {

        Optional<HousingProvider> electricProviderOptional = findProvider(providerWithProviderFields, ProviderType.ELECTRICITY);
        if (electricProviderOptional.isEmpty()) {
            return null; // brak dostawcy elektryki dla tego mieszkania
        }
        HousingProvider provider = electricProviderOptional.get();
        BigDecimal vatPercent = Optional.ofNullable(provider.getTax()).orElse(BigDecimal.ZERO); // np. 23

        // --- 1) Identyfikacja mieszkania i kawalerek ---
        Long apartmentId = rentedObjectRepository.findApartmentIdByRentedObjectId(rentedObjectId);
        List<Long> rentedObjectIds = rentedObjectRepository.findRentedObjectIdsByApartmentId(apartmentId)
                .stream().sorted().toList(); // deterministyczna kolejność
        int count = rentedObjectIds.size();

        // --- 2) SNAPSHOT ID odczytów mieszkania (jeszcze nie zamknięte na poziomie mieszkania) ---
        List<Long> apartmentSnapshotIds =
                meterReadingRepository.findUnclosedReadingIdsInApartment(apartmentId, MeterType.ELECTRIC);

        // --- 3) SUMA mieszkania (NETTO) na podstawie SNAPSHOTU ---
        // 3a) Suma zużycia = suma po RO (ignoruje 'settled', patrzy wyłącznie na 'apartmentSettled=false')
        BigDecimal totalConsumption = rentedObjectIds.stream()
                .map(roId -> meterReadingRepository.sumConsumptionUnclosedForRentedObject(roId, MeterType.ELECTRIC))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3b) Zmienne wg zużycia – "jak na fakturze": stawka * SUMA_zużycia (per stawka), r2, a potem suma
        List<ProviderField> fields = provider.getProviderFields();
        BigDecimal variableNetTotal = variableNetForConsumption(totalConsumption, fields);

        // 3c) Miesięczne (suma wszystkich pozycji miesięcznych dla mieszkania)
        BigDecimal monthlySum = sumMonthlyForProvider(provider);

        // 3d) NETTO i BRUTTO mieszkania
        BigDecimal apartmentNet = variableNetTotal.add(monthlySum);
        BigDecimal apartmentGross = r2(apartmentNet.multiply(vatFactor(vatPercent)));

        // --- 4) NETTO per kawalerka (SNAPSHOT) + równy udział miesięcznych ---
        Map<Long, BigDecimal> netPerRo = new LinkedHashMap<>();
        for (Long roId : rentedObjectIds) {
            BigDecimal cons = meterReadingRepository.sumConsumptionUnclosedForRentedObject(roId, MeterType.ELECTRIC);
            BigDecimal variableNet = variableNetForConsumption(cons, fields);
            BigDecimal monthlyShareNet = monthlyShare(monthlySum, count);
            BigDecimal net = variableNet.add(monthlyShareNet).setScale(2, RoundingMode.HALF_UP);
            netPerRo.put(roId, net);
        }

        // --- 5) Alokacja BRUTTO na kawalerki (dopina grosze) ---
        Map<Long, BigDecimal> grossPerRo = allocateGrossPerRentedObject(netPerRo, apartmentGross, vatPercent);

        // --- 6) Odczyty do oznaczenia 'settled' – tylko bieżący RO ---
        List<MeterReadingDto> meterReadingDtos =
                meterRepository.getConsumptionAndIdByRentedObjectAndMeterType(rentedObjectId, MeterType.ELECTRIC);
        allMeterReadingIds.addAll(meterReadingDtos.stream().map(MeterReadingDto::getId).toList());

        BigDecimal resultGrossForCurrent = grossPerRo.getOrDefault(rentedObjectId, BigDecimal.ZERO);

        log.debug("[ELEKTRYCZNOŚĆ][ALLOC] aptId={} roId={} totalConsumption={} apartmentNet={} apartmentGross={} roNet={} roGross={}",
                apartmentId, rentedObjectId, totalConsumption, apartmentNet, apartmentGross,
                netPerRo.get(rentedObjectId), resultGrossForCurrent);

        // Zwracamy kwotę dla bieżącej kawalerki **oraz** kontekst do domknięcia cyklu po zapisie rozliczenia
        return new ElectricityCalcResult(resultGrossForCurrent, apartmentId, apartmentSnapshotIds);
    }



    public BigDecimal calculateWaterCost(List<HousingProvider> providerWithProviderFields, Long rentedObjectId, BigDecimal waterResult, List<Long> allMeterReadingIds, LeaseAgreementDto leaseAgreement) {
        Optional<HousingProvider> waterHousingProviderOptional = findProvider(providerWithProviderFields, ProviderType.WATER);

        if (waterHousingProviderOptional.isPresent()) {
            HousingProvider waterHousingProvider = waterHousingProviderOptional.get();
            Optional<ProviderField> optionalField = waterHousingProvider.getProviderFields().stream().findFirst();

            if (optionalField.isPresent()) {
                ProviderField field = optionalField.get();
                List<MeterReadingDto> meterReadingDtos = meterRepository.getConsumptionAndIdByRentedObjectAndMeterType(rentedObjectId, MeterType.WATER_COLD);
                BigDecimal consumption = meterReadingDtos.stream()
                        .map(MeterReadingDto::getConsumption)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                allMeterReadingIds.addAll(meterReadingDtos.stream()
                        .map(MeterReadingDto::getId)
                        .toList());

                // Bezpośrednie obliczenie kosztu wody na podstawie zużycia
                waterResult = waterResult.add(this.calculateByConsumption(field.getPrice(), consumption));

                // Aplikowanie podatku VAT, jeśli jest ustawiony
                if (waterHousingProvider.getTax() != null) {
                    waterResult = this.calculateVat(waterHousingProvider.getTax(), waterResult);
                }

                // Odjęcie wliczonych w cenę jednostek wody
                BigDecimal includedWaterMeters = this.calculateByConsumptionAndMonthCount(field.getPrice(), leaseAgreement.getIncludedWaterMeters(), meterReadingDtos.size());
                waterResult = waterResult.subtract(includedWaterMeters);
            }
            return waterResult;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateByConsumption(final BigDecimal entityField, BigDecimal consumptionAfterConversion) {
        return consumptionAfterConversion.multiply(entityField).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateByConsumptionAndMonthCount(final BigDecimal entityField, BigDecimal consumptionAfterConversion, final int monthCount) {
        return consumptionAfterConversion.multiply(entityField).multiply(BigDecimal.valueOf(monthCount)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVat(final BigDecimal vat, BigDecimal netAmount) {
        return netAmount.multiply(vat.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP).add(BigDecimal.ONE)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal r2(BigDecimal x) {
        return x.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal vatFactor(BigDecimal vatPercent) {
        return vatPercent
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
                .add(BigDecimal.ONE);
    }

    private BigDecimal sumMonthlyForProvider(HousingProvider provider) {
        return provider.getProviderFields().stream()
                .filter(f -> f.getBillingMethod() == BillingMethod.MONTHLY)
                .map(ProviderField::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add); // np. 22.86
    }

    private BigDecimal monthlyShare(BigDecimal monthlySum, int rentedObjectsCount) {
        if (rentedObjectsCount <= 0) return BigDecimal.ZERO;
        return monthlySum
                .divide(BigDecimal.valueOf(rentedObjectsCount), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP); // np. 22.86 / 2 = 11.43
    }

    private BigDecimal variableNetForConsumption(BigDecimal consumption, List<ProviderField> fields) {
        // zgodnie z Twoją fakturą: (stawka * zużycie) zaokrąglone do 2, potem sumowane
        return fields.stream()
                .filter(f -> f.getBillingMethod() == BillingMethod.ACCORDING_TO_CONSUMPTION)
                .map(f -> r2(f.getPrice().multiply(consumption)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Alokacja grosza: dopina sumę linii do brutto grupy deterministycznie. */
    private Map<Long, BigDecimal> allocateGrossPerRentedObject(Map<Long, BigDecimal> netPerRo,
                                                               BigDecimal apartmentGross,
                                                               BigDecimal vatPercent) {
        if (netPerRo == null || netPerRo.isEmpty()) {
            return Collections.emptyMap();
        }

        BigDecimal factor = vatFactor(vatPercent); // np. 1.23
        record Tmp(Long roId, BigDecimal prelimGross, BigDecimal frac) {}

        List<Tmp> tmps = new ArrayList<>(netPerRo.size());
        for (var e : netPerRo.entrySet()) {
            BigDecimal raw = e.getValue().multiply(factor); // niezaokrąglone brutto
            BigDecimal prelim = r2(raw);                    // wstępnie zaokrąglone
            BigDecimal frac = raw.subtract(prelim);         // „ogon”
            tmps.add(new Tmp(e.getKey(), prelim, frac));
        }

        BigDecimal sumPrelim = tmps.stream().map(Tmp::prelimGross).reduce(BigDecimal.ZERO, BigDecimal::add);
        int centsDelta = apartmentGross.subtract(sumPrelim).movePointRight(2).intValue(); // różnica w groszach

        Comparator<Tmp> cmp = (centsDelta >= 0)
                ? Comparator.comparing(Tmp::frac, Comparator.reverseOrder()).thenComparing(Tmp::roId)
                : Comparator.comparing(Tmp::frac).thenComparing(Tmp::roId);
        tmps.sort(cmp);

        Map<Long, BigDecimal> out = new HashMap<>(tmps.size());
        if (centsDelta == 0) {
            for (Tmp t : tmps) out.put(t.roId(), t.prelimGross());
            return out;
        }

        int steps = Math.abs(centsDelta);
        int n = tmps.size();
        int fullRounds = steps / n;     // ile pełnych „okrążeń” po wszystkich pozycjach
        int remainder  = steps % n;     // ile groszy zostaje do rozdysponowania

        BigDecimal oneCent = new BigDecimal("0.01");
        BigDecimal fullInc = BigDecimal.valueOf(fullRounds).movePointLeft(2); // np. 3 → 0.03

        for (int i = 0; i < n; i++) {
            Tmp t = tmps.get(i);
            BigDecimal corrected = t.prelimGross();

            if (centsDelta > 0) {
                if (fullRounds > 0) corrected = corrected.add(fullInc);
                if (i < remainder) corrected = corrected.add(oneCent);
            } else {
                if (fullRounds > 0) corrected = corrected.subtract(fullInc);
                if (i < remainder) corrected = corrected.subtract(oneCent);
            }
            out.put(t.roId(), corrected);
        }
        return out;
    }


    private record ElectricityCalcResult(BigDecimal amount,
                                         Long apartmentId,
                                         List<Long> apartmentSnapshotIds) { }

    @Override
    @Transactional(readOnly = true)
    public Page<SettlementListDto> getSettlements(Pageable pageable, Long rentedObjectId) {
        Page<Settlement> settlementsPage = settlementRepository.findAllByRentedObjectId(pageable, rentedObjectId);
        return settlementsPage.map(settlementMapper::toListDto);
    }

}
