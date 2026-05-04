package pl.rentowne.settlement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.apartment.ApartmentService;
import pl.rentowne.housing_provider.HousingProvider;
import pl.rentowne.housing_provider.HousingProviderRepository;
import pl.rentowne.housing_provider.ProviderType;
import pl.rentowne.lease_agreement.LeaseAgreementDto;
import pl.rentowne.lease_agreement.LeaseAgreementRepository;
import pl.rentowne.meter.MeterReadingData;
import pl.rentowne.meter.MeterType;
import pl.rentowne.meter_reading.MeterReading;
import pl.rentowne.meter_reading.MeterReadingDto;
import pl.rentowne.meter_reading.MeterReadingRepository;
import pl.rentowne.provider_field.model.BillingMethod;
import pl.rentowne.provider_field.model.ProviderField;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.rented_object.RentedObjectRepository;
import pl.rentowne.settlement.CalculateRequestDto;
import pl.rentowne.settlement.mapper.SettlementMapper;
import pl.rentowne.settlement.model.ProrationMode;
import pl.rentowne.settlement.model.Settlement;
import pl.rentowne.settlement.model.SettlementStatus;
import pl.rentowne.settlement.model.dto.SettlementListDto;
import pl.rentowne.settlement.model.dto.UtilityCostsDto;
import pl.rentowne.settlement.repository.SettlementRepository;
import pl.rentowne.settlement.service.SettlementService;
import pl.rentowne.tenant.model.Tenant;
import pl.rentowne.tenant.repository.TenantRepository;

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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {
    private final SettlementRepository settlementRepository;
    private final HousingProviderRepository housingProviderRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final LeaseAgreementRepository leaseAgreementRepository;
    private final RentedObjectRepository rentedObjectRepository;
    private final ApartmentService apartmentService;
    private final SettlementMapper settlementMapper;
    private final TenantRepository tenantRepository;

    @Transactional
    @Override
    public void calculate(Long rentedObjectId, CalculateRequestDto dto) {
        // Check if this is a partial month settlement
        if (Boolean.TRUE.equals(dto.getIsPartialMonth())) {
            calculatePartialMonth(rentedObjectId, dto);
            return;
        }

        // Original full month settlement logic
        ElectricityCalcResult elecCtx = null;
        try {
            List<HousingProvider> providerWithProviderFields =
                    housingProviderRepository.findHousingProviderWithProviderFieldsByRentedObjectId(rentedObjectId);
            LeaseAgreementDto leaseAgreement = leaseAgreementRepository.getData(rentedObjectId);

            List<Long> allMeterReadingIds = new ArrayList<>();
            BigDecimal electricResult = BigDecimal.ZERO;
            BigDecimal distributedElectricResult = BigDecimal.ZERO;
            BigDecimal gasResult;
            BigDecimal waterResult = BigDecimal.ZERO;

            if (dto.isElectricityIncluded()) {
                elecCtx = this.calculateElectricityCost(
                        providerWithProviderFields, rentedObjectId, allMeterReadingIds);
                electricResult = (elecCtx != null) ? elecCtx.amount() : BigDecimal.ZERO;
                distributedElectricResult = (elecCtx != null) ? elecCtx.distributedAmount() : BigDecimal.ZERO;
            }

            if (dto.isWaterIncluded()) {
                waterResult = this.calculateWaterCost(
                        providerWithProviderFields, rentedObjectId, waterResult, allMeterReadingIds);
            }

            gasResult = leaseAgreement.gasDeposit() != null ? leaseAgreement.gasDeposit() : BigDecimal.ZERO;

            BigDecimal totalSum = electricResult.add(waterResult)
                    .add(leaseAgreement.compensationAmount())
                    .add(leaseAgreement.internetFee())
                    .add(leaseAgreement.rentAmount())
                    .add(gasResult);

            // Logi dla standardowych logów aplikacji
            log.info("Rozliczenie obiektu o id: {}", rentedObjectId);
            log.info("Rozliczano: wodę-{}, prąd-{}", dto.isWaterIncluded(), dto.isElectricityIncluded());
            log.info("Kwota za prąd: {}", electricResult);
            log.info("Kwota za prąd z licznika mieszkania: {}", distributedElectricResult);
            log.info("Kwota za wodę: {}", waterResult);
            log.info("Kwota za gaz: {}", gasResult);
            log.info("Kwota odstępnego: {}", leaseAgreement.compensationAmount());
            log.info("Kwota za internet: {}", leaseAgreement.internetFee());
            log.info("Kwota czynszu: {}", leaseAgreement.rentAmount());
            log.info("Kwota całkowita: {}", totalSum);

            Tenant tenant = tenantRepository.getTenantByRentedObjectId(rentedObjectId);

            Settlement settlement = Settlement.builder()
                    .electricityAmount(electricResult)
                    .distributedElectricityAmount(distributedElectricResult)
                    .waterAmount(waterResult)
                    .gasAmount(gasResult)
                    .totalAmount(totalSum)
                    .compensationAmount(leaseAgreement.compensationAmount())
                    .rentAmount(leaseAgreement.rentAmount())
                    .internetFee(leaseAgreement.internetFee())
                    .gasDeposit(leaseAgreement.gasDeposit())
                    .settlementDate(dto.getSettlementDate())
                    .rentedObject(RentedObject.builder().id(rentedObjectId).build())
                    .tenant(tenant)
                    .settlementStatus(SettlementStatus.TO_PAY)
                    .build();

            Long settlementId = settlementRepository.saveAndFlush(settlement).getId();

            if (!allMeterReadingIds.isEmpty()) {
                meterReadingRepository.updateSettled(allMeterReadingIds, settlementId);
            }
            log.info("W następujących odczytach liczników ustawiono odczyty na rozliczone: {}", allMeterReadingIds);

            // Domknięcie cyklu na poziomie mieszkania (tylko elektryka)
            if (elecCtx != null && elecCtx.apartmentId() != null) {
                long pending = meterReadingRepository.countUnclosedAndUnsettledInApartment(
                        elecCtx.apartmentId(), MeterType.ELECTRIC);

                if (pending == 0 && elecCtx.apartmentSnapshotIds() != null && !elecCtx.apartmentSnapshotIds().isEmpty()) {
                    List<Long> distributeReadings = elecCtx.apartmentSnapshotIds.stream()
                            .filter(ElectricityCalcResult.ReadingSnapshot::distributeReadings)
                            .map(ElectricityCalcResult.ReadingSnapshot::id).toList();

                    Set<Long> notDistributeReadings = elecCtx.apartmentSnapshotIds.stream()
                            .filter(t -> !t.distributeReadings())
                            .map(ElectricityCalcResult.ReadingSnapshot::id).collect(Collectors.toSet());
                    meterReadingRepository.markDistributedReadingsSettled(distributeReadings, notDistributeReadings);
                    meterReadingRepository.markApartmentSettled(notDistributeReadings);
                    log.debug("[ELEKTRYCZNOŚĆ][CLOSE_CYCLE] aptId={} closed {} readings: {}",
                            elecCtx.apartmentId(), elecCtx.apartmentSnapshotIds().size(), elecCtx.apartmentSnapshotIds());
                }
            }

            rentedObjectRepository.updateSettlement(rentedObjectId, totalSum, dto.getSettlementDate());

        } catch (Exception e) {
            log.error("Błąd podczas rozliczenia obiektu {}", rentedObjectId, e);
            throw e;
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
            List<Long> meterReadings = meterReadingRepository.findMeterReadings(cycleSettlementIds);
            List<MeterReading> allDistributed = meterReadingRepository.findAllBySettledTrueAndApartmentSettledTrueAndMeter_DistributeReadingsTrue();

            List<MeterReading> matchingDistributed = allDistributed.stream()
                    .filter(mr -> mr.getMeterReadingSettlementIds() != null && mr.getMeterReadingSettlementIds().stream()
                            .anyMatch(meterReadings::contains))
                    .toList();
            List<Long> meterReadingIds = matchingDistributed.stream().map(MeterReading::getId).toList();
            meterReadingRepository.unsetDistributedReadingsSettled(meterReadingIds);

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


    /**
     * Rozliczenie niepełnego miesiąca z proporcjonalnym przeliczeniem wybranych składników
     */
    @Transactional
    public void calculatePartialMonth(Long rentedObjectId, CalculateRequestDto dto) {
        ElectricityCalcResult elecCtx = null;
        try {
            log.info("Rozpoczynam rozliczenie niepełnego miesiąca dla obiektu: {}", rentedObjectId);

            // Walidacja dat
            if (dto.getPeriodStartDate() == null || dto.getPeriodEndDate() == null) {
                throw new IllegalArgumentException("Dla rozliczenia niepełnego miesiąca wymagane są daty: periodStartDate i periodEndDate");
            }

            List<HousingProvider> providerWithProviderFields =
                    housingProviderRepository.findHousingProviderWithProviderFieldsByRentedObjectId(rentedObjectId);
            LeaseAgreementDto leaseAgreement = leaseAgreementRepository.getData(rentedObjectId);

            // Utworzenie kalkulatora proporcji
            ProrationCalculator prorationCalc = ProrationCalculator.builder()
                    .periodStartDate(dto.getPeriodStartDate())
                    .periodEndDate(dto.getPeriodEndDate())
                    .settlementDate(dto.getSettlementDate())
                    .build();

            log.info("Okres rozliczenia: {} (dni: {}/{})",
                    prorationCalc.getPeriodDescription(),
                    prorationCalc.getActualDays(),
                    prorationCalc.getTotalDaysInMonth());

            List<Long> allMeterReadingIds = new ArrayList<>();
            BigDecimal electricResult = BigDecimal.ZERO;
            BigDecimal distributedElectricResult = BigDecimal.ZERO;
            BigDecimal gasResult;
            BigDecimal waterResult = BigDecimal.ZERO;

            // Prąd - zawsze z liczników (rzeczywiste zużycie)
            if (dto.isElectricityIncluded()) {
                elecCtx = this.calculateElectricityCost(
                        providerWithProviderFields, rentedObjectId, allMeterReadingIds);
                electricResult = (elecCtx != null) ? elecCtx.amount() : BigDecimal.ZERO;
                distributedElectricResult = (elecCtx != null) ? elecCtx.distributedAmount() : BigDecimal.ZERO;
            }

            // Woda - rozliczana z liczników (jak prąd)
            if (dto.isWaterIncluded()) {
                waterResult = this.calculateWaterCostForPartialMonth(
                        providerWithProviderFields, rentedObjectId, waterResult, allMeterReadingIds);
            }

            // Oblicz proporcjonalne kwoty z umowy
            ProrationMode rentMode = Boolean.TRUE.equals(dto.getIsRentProrated()) ? ProrationMode.PRORATED : ProrationMode.FULL;
            ProrationMode compensationMode = Boolean.TRUE.equals(dto.getIsCompensationProrated()) ? ProrationMode.PRORATED : ProrationMode.FULL;
            ProrationMode gasMode = Boolean.TRUE.equals(dto.getIsGasDepositProrated()) ? ProrationMode.PRORATED : ProrationMode.FULL;
            ProrationMode internetMode = Boolean.TRUE.equals(dto.getIsInternetFeeProrated()) ? ProrationMode.PRORATED : ProrationMode.FULL;

            BigDecimal rentAmount = prorationCalc.calculateAmount(leaseAgreement.rentAmount(), rentMode);
            BigDecimal compensationAmount = prorationCalc.calculateAmount(leaseAgreement.compensationAmount(), compensationMode);
            BigDecimal gasDeposit = leaseAgreement.gasDeposit() != null
                    ? prorationCalc.calculateAmount(leaseAgreement.gasDeposit(), gasMode)
                    : BigDecimal.ZERO;
            BigDecimal internetFee = leaseAgreement.internetFee() != null
                    ? prorationCalc.calculateAmount(leaseAgreement.internetFee(), internetMode)
                    : BigDecimal.ZERO;

            gasResult = gasDeposit;

            BigDecimal totalSum = electricResult.add(waterResult)
                    .add(compensationAmount)
                    .add(internetFee)
                    .add(rentAmount);

            // Generuj opis rozliczenia
            String prorationDescription = buildProrationDescription(
                    prorationCalc,
                    rentAmount, rentMode,
                    compensationAmount, compensationMode,
                    gasDeposit, gasMode,
                    internetFee, internetMode
            );

            // Logi
            log.info("Rozliczenie niepełnego miesiąca obiektu o id: {}", rentedObjectId);
            log.info("Okres: {}", prorationCalc.getPeriodDescription());
            log.info("Współczynnik proporcji: {}", prorationCalc.getProrationFactor());
            log.info("Rozliczano: wodę-{}, prąd-{}", dto.isWaterIncluded(), dto.isElectricityIncluded());
            log.info("Kwota czynszu: {} ({})", rentAmount, rentMode.getDescription());
            log.info("Kwota odstępnego: {} ({})", compensationAmount, compensationMode.getDescription());
            log.info("Kwota za prąd: {}", electricResult);
            log.info("Kwota za prąd z licznika mieszkania: {}", distributedElectricResult);
            log.info("Kwota za wodę: {}", waterResult);
            log.info("Kwota za gaz: {} ({})", gasResult, gasMode.getDescription());
            log.info("Kwota za internet: {} ({})", internetFee, internetMode.getDescription());
            log.info("Kwota całkowita: {}", totalSum);

            Tenant tenant = tenantRepository.getTenantByRentedObjectId(rentedObjectId);

            Settlement settlement = Settlement.builder()
                    .electricityAmount(electricResult)
                    .distributedElectricityAmount(distributedElectricResult)
                    .waterAmount(waterResult)
                    .gasAmount(gasResult)
                    .totalAmount(totalSum)
                    .compensationAmount(compensationAmount)
                    .rentAmount(rentAmount)
                    .internetFee(internetFee)
                    .gasDeposit(gasDeposit)
                    .settlementDate(dto.getSettlementDate())
                    .rentedObject(RentedObject.builder().id(rentedObjectId).build())
                    .tenant(tenant)
                    .settlementStatus(SettlementStatus.TO_PAY)
                    .prorationDescription(prorationDescription)
                    .build();

            Long settlementId = settlementRepository.saveAndFlush(settlement).getId();

            if (!allMeterReadingIds.isEmpty()) {
                meterReadingRepository.updateSettled(allMeterReadingIds, settlementId);
            }
            log.info("W następujących odczytach liczników ustawiono odczyty na rozliczone: {}", allMeterReadingIds);

            // Domknięcie cyklu na poziomie mieszkania (tylko elektryka)
            if (elecCtx != null && elecCtx.apartmentId() != null) {
                long pending = meterReadingRepository.countUnclosedAndUnsettledInApartment(
                        elecCtx.apartmentId(), MeterType.ELECTRIC);

                if (pending == 0 && elecCtx.apartmentSnapshotIds() != null && !elecCtx.apartmentSnapshotIds().isEmpty()) {
                    List<Long> distributeReadings = elecCtx.apartmentSnapshotIds.stream()
                            .filter(ElectricityCalcResult.ReadingSnapshot::distributeReadings)
                            .map(ElectricityCalcResult.ReadingSnapshot::id).toList();

                    Set<Long> notDistributeReadings = elecCtx.apartmentSnapshotIds.stream()
                            .filter(t -> !t.distributeReadings())
                            .map(ElectricityCalcResult.ReadingSnapshot::id).collect(Collectors.toSet());
                    meterReadingRepository.markDistributedReadingsSettled(distributeReadings, notDistributeReadings);
                    meterReadingRepository.markApartmentSettled(notDistributeReadings);
                    log.debug("[ELEKTRYCZNOŚĆ][CLOSE_CYCLE] aptId={} closed {} readings: {}",
                            elecCtx.apartmentId(), elecCtx.apartmentSnapshotIds().size(), elecCtx.apartmentSnapshotIds());
                }
            }

            rentedObjectRepository.updateSettlement(rentedObjectId, totalSum, dto.getSettlementDate());

        } catch (Exception e) {
            log.error("Błąd podczas rozliczenia niepełnego miesiąca obiektu {}", rentedObjectId, e);
            throw e;
        }
    }

    /**
     * Buduje opis rozliczenia dla niepełnego miesiąca
     * Format: "15-31.01.2025 (54.84%) | Proporcja: czynsz, gaz | Pełne: odstępne"
     */
    private String buildProrationDescription(
            ProrationCalculator prorationCalc,
            BigDecimal rentAmount, ProrationMode rentMode,
            BigDecimal compensationAmount, ProrationMode compensationMode,
            BigDecimal gasDeposit, ProrationMode gasMode,
            BigDecimal internetFee, ProrationMode internetMode) {

        StringBuilder sb = new StringBuilder();

        // Okres i procent proporcji
        BigDecimal percentage = prorationCalc.getProrationFactor().multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        sb.append(prorationCalc.getPeriodDescription())
          .append(" (").append(percentage).append("%)");

        // Składniki w proporcji
        List<String> proratedItems = new ArrayList<>();
        if (rentMode == ProrationMode.PRORATED) proratedItems.add("czynsz");
        if (compensationMode == ProrationMode.PRORATED) proratedItems.add("odstępne");
        if (gasMode == ProrationMode.PRORATED && gasDeposit != null && gasDeposit.compareTo(BigDecimal.ZERO) > 0) {
            proratedItems.add("gaz");
        }
        if (internetMode == ProrationMode.PRORATED && internetFee != null && internetFee.compareTo(BigDecimal.ZERO) > 0) {
            proratedItems.add("internet");
        }

        if (!proratedItems.isEmpty()) {
            sb.append(" | Proporcja: ").append(String.join(", ", proratedItems));
        }

        // Składniki pełne
        List<String> fullItems = new ArrayList<>();
        if (rentMode == ProrationMode.FULL) fullItems.add("czynsz");
        if (compensationMode == ProrationMode.FULL) fullItems.add("odstępne");
        if (gasMode == ProrationMode.FULL && gasDeposit != null && gasDeposit.compareTo(BigDecimal.ZERO) > 0) {
            fullItems.add("gaz");
        }
        if (internetMode == ProrationMode.FULL && internetFee != null && internetFee.compareTo(BigDecimal.ZERO) > 0) {
            fullItems.add("internet");
        }

        if (!fullItems.isEmpty()) {
            sb.append(" | Pełne: ").append(String.join(", ", fullItems));
        }

        return sb.toString();
    }


    private Optional<HousingProvider> findProvider(List<HousingProvider> providers, ProviderType type) {
        return providers.stream()
                .filter(dto -> type.equals(dto.getType()))
                .findFirst();
    }

    // ZWRACA kontekst (kwota dla bieżącej kawalerki + snapshot mieszkania do domknięcia cyklu + kwota z licznika mieszkania)
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

        // --- 3) Pobierz odczyty z licznika mieszkania (distributeReadings = true) ---
        // WAŻNE: Używamy apartment_settled zamiast settled, żeby odczyty były dostępne dla wszystkich wywołań
        List<MeterReadingDto> distributedReadings =
                meterReadingRepository.findUnsettledDistributedReadingsByApartmentAndType(apartmentId, MeterType.ELECTRIC);

        BigDecimal distributedConsumption = distributedReadings.stream()
                .map(MeterReadingDto::consumption)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // WAŻNE: Dodajemy ID odczytów z licznika mieszkania do apartmentSnapshotIds,
        // żeby były oznaczone jako settled dopiero po przetworzeniu wszystkich kawalerek
        List<Long> distributedReadingIds = distributedReadings.stream()
                .map(MeterReadingDto::id)
                .toList();

        // Tworzenie listy ReadingSnapshot z odpowiednimi flagami
        List<ElectricityCalcResult.ReadingSnapshot> combinedSnapshotIds = new ArrayList<>();

        // Dodaj odczyty z kawalerek (distributeReadings = false)
        for (Long id : apartmentSnapshotIds) {
            combinedSnapshotIds.add(new ElectricityCalcResult.ReadingSnapshot(id, false));
        }

        // Dodaj odczyty z licznika mieszkania (distributeReadings = true)
        for (Long id : distributedReadingIds) {
            combinedSnapshotIds.add(new ElectricityCalcResult.ReadingSnapshot(id, true));
        }

        // Oblicz udział z licznika mieszkania na kawalerkę
        BigDecimal distributedPerRo = (count > 0 && distributedConsumption.compareTo(BigDecimal.ZERO) > 0)
                ? distributedConsumption.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // --- 4) Oblicz kwotę za rozdzielane zużycie (na kawalerkę) ---
        List<ProviderField> fields = provider.getProviderFields();
        BigDecimal distributedAmountNet = variableNetForConsumption(distributedPerRo, fields);
        BigDecimal distributedAmountGross = r2(distributedAmountNet.multiply(vatFactor(vatPercent)));

        // --- 5) SUMA mieszkania (NETTO) - zużycie z kawalerek + licznik mieszkania ---
        BigDecimal totalConsumptionFromRo = rentedObjectIds.stream()
                .map(roId -> meterReadingRepository.sumConsumptionUnclosedForRentedObject(roId, MeterType.ELECTRIC))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalConsumption = totalConsumptionFromRo.add(distributedConsumption);

        // Zmienne wg zużycia – "jak na fakturze": stawka * SUMA_zużycia (per stawka), r2, a potem suma
        BigDecimal variableNetTotal = variableNetForConsumption(totalConsumption, fields);

        // Miesięczne (suma wszystkich pozycji miesięcznych dla mieszkania)
        BigDecimal monthlySum = sumMonthlyForProvider(provider);

        // NETTO i BRUTTO mieszkania
        BigDecimal apartmentNet = variableNetTotal.add(monthlySum);
        BigDecimal apartmentGross = r2(apartmentNet.multiply(vatFactor(vatPercent)));

        // --- 6) NETTO per kawalerka - z uwzględnieniem rozdzielonego zużycia ---
        Map<Long, BigDecimal> netPerRo = new LinkedHashMap<>();

        for (Long roId : rentedObjectIds) {
            BigDecimal consFromRo = meterReadingRepository.sumConsumptionUnclosedForRentedObject(roId, MeterType.ELECTRIC);
            // KLUCZOWE: dodajemy rozdzielone zużycie DO zużycia kawalerki
            BigDecimal totalConsForRo = consFromRo.add(distributedPerRo);
            BigDecimal variableNet = variableNetForConsumption(totalConsForRo, fields);
            BigDecimal monthlyShareNet = monthlyShare(monthlySum, count);
            BigDecimal net = variableNet.add(monthlyShareNet).setScale(2, RoundingMode.HALF_UP);
            netPerRo.put(roId, net);
        }

        // --- 7) Alokacja BRUTTO na kawalerki (dopina grosze) ---
        Map<Long, BigDecimal> grossPerRo = allocateGrossPerRentedObject(netPerRo, apartmentGross, vatPercent);

        // --- 8) Odczyty do oznaczenia 'settled' – tylko bieżący RO ---
        List<MeterReadingData> meterReadings =
                settlementRepository.getConsumptionAndIdByRentedObjectAndMeterType(rentedObjectId, MeterType.ELECTRIC);
        allMeterReadingIds.addAll(meterReadings.stream().map(MeterReadingData::id).toList());

        BigDecimal resultGrossForCurrent = grossPerRo.getOrDefault(rentedObjectId, BigDecimal.ZERO);

        log.debug("[ELEKTRYCZNOŚĆ][ALLOC] aptId={} roId={} totalConsumption={} (fromRo={} + distributed={}) apartmentNet={} apartmentGross={} roNet={} roGross={} distributedPerRo={} distributedAmount={}",
                apartmentId, rentedObjectId, totalConsumption, totalConsumptionFromRo, distributedConsumption,
                apartmentNet, apartmentGross, netPerRo.get(rentedObjectId), resultGrossForCurrent,
                distributedPerRo, distributedAmountGross);

        // Zwracamy kwotę dla bieżącej kawalerki **oraz** kontekst do domknięcia cyklu po zapisie rozliczenia
        return new ElectricityCalcResult(resultGrossForCurrent, apartmentId, combinedSnapshotIds, distributedAmountGross);
    }



    /**
     * Oblicza koszt wody dla niepełnego miesiąca.
     * Woda rozliczana jest jak prąd - na podstawie zużycia z licznika.
     */
    private BigDecimal calculateWaterCostForPartialMonth(
            List<HousingProvider> providerWithProviderFields,
            Long rentedObjectId,
            BigDecimal waterResult,
            List<Long> allMeterReadingIds) {

        Optional<HousingProvider> waterHousingProviderOptional = findProvider(providerWithProviderFields, ProviderType.WATER);

        if (waterHousingProviderOptional.isPresent()) {
            HousingProvider waterHousingProvider = waterHousingProviderOptional.get();
            Optional<ProviderField> optionalField = waterHousingProvider.getProviderFields().stream().findFirst();

            if (optionalField.isPresent()) {
                ProviderField field = optionalField.get();

                // Pobierz nierozliczone odczyty do obliczenia zużycia i oznaczenia jako settled
                List<MeterReadingDto> unsettledReadings = meterReadingRepository.findReadingsWithDatesByRentedObjectAndMeterType(rentedObjectId, MeterType.WATER_COLD);
                BigDecimal consumption = unsettledReadings.stream()
                        .map(MeterReadingDto::consumption)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                allMeterReadingIds.addAll(unsettledReadings.stream()
                        .map(MeterReadingDto::id)
                        .toList());

                // Oblicz koszt wody na podstawie zużycia
                waterResult = waterResult.add(this.calculateByConsumption(field.getPrice(), consumption));

                // Aplikowanie podatku VAT, jeśli jest ustawiony
                if (waterHousingProvider.getTax() != null) {
                    waterResult = this.calculateVat(waterHousingProvider.getTax(), waterResult);
                }
            }
            return waterResult;
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateWaterCost(List<HousingProvider> providerWithProviderFields, Long rentedObjectId, BigDecimal waterResult, List<Long> allMeterReadingIds) {
        Optional<HousingProvider> waterHousingProviderOptional = findProvider(providerWithProviderFields, ProviderType.WATER);

        if (waterHousingProviderOptional.isPresent()) {
            HousingProvider waterHousingProvider = waterHousingProviderOptional.get();
            Optional<ProviderField> optionalField = waterHousingProvider.getProviderFields().stream().findFirst();

            if (optionalField.isPresent()) {
                ProviderField field = optionalField.get();
                // Pobierz nierozliczone odczyty do obliczenia zużycia i oznaczenia jako settled
                List<MeterReadingDto> unsettledReadings = meterReadingRepository.findReadingsWithDatesByRentedObjectAndMeterType(rentedObjectId, MeterType.WATER_COLD);
                BigDecimal consumption = unsettledReadings.stream()
                        .map(MeterReadingDto::consumption)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                allMeterReadingIds.addAll(unsettledReadings.stream()
                        .map(MeterReadingDto::id)
                        .toList());

                // Oblicz koszt wody na podstawie zużycia
                waterResult = waterResult.add(this.calculateByConsumption(field.getPrice(), consumption));

                // Aplikowanie podatku VAT, jeśli jest ustawiony
                if (waterHousingProvider.getTax() != null) {
                    waterResult = this.calculateVat(waterHousingProvider.getTax(), waterResult);
                }
            }
            return waterResult;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateByConsumption(final BigDecimal entityField, BigDecimal consumptionAfterConversion) {
        return consumptionAfterConversion.multiply(entityField).setScale(2, RoundingMode.HALF_UP);
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
                                         List<ReadingSnapshot> apartmentSnapshotIds,
                                         BigDecimal distributedAmount) {
        record ReadingSnapshot(Long id, boolean distributeReadings) {}
    }

    @Override
    @Transactional(readOnly = true)
    public UtilityCostsDto calculateUtilityCostsOnly(Long rentedObjectId) {
        List<HousingProvider> providerWithProviderFields =
                housingProviderRepository.findHousingProviderWithProviderFieldsByRentedObjectId(rentedObjectId);

        List<Long> tempMeterReadingIds = new ArrayList<>();

        // Oblicz prąd
        BigDecimal electricityAmount = BigDecimal.ZERO;
        BigDecimal electricityConsumption = BigDecimal.ZERO;
        ElectricityCalcResult elecCtx = this.calculateElectricityCost(
                providerWithProviderFields, rentedObjectId, tempMeterReadingIds);
        if (elecCtx != null) {
            electricityAmount = elecCtx.amount();
            // Pobierz zużycie prądu
            List<MeterReadingData> electricityReadings =
                    settlementRepository.getConsumptionAndIdByRentedObjectAndMeterType(rentedObjectId, MeterType.ELECTRIC);
            electricityConsumption = electricityReadings.stream()
                    .map(MeterReadingData::consumption)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Oblicz wodę
        tempMeterReadingIds.clear();
        BigDecimal waterAmount = this.calculateWaterCost(
                providerWithProviderFields, rentedObjectId, BigDecimal.ZERO, tempMeterReadingIds);

        // Pobierz zużycie wody
        List<MeterReadingData> waterReadings =
                settlementRepository.getConsumptionAndIdByRentedObjectAndMeterType(rentedObjectId, MeterType.WATER_COLD);
        BigDecimal waterConsumption = waterReadings.stream()
                .map(MeterReadingData::consumption)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UtilityCostsDto.builder()
                .waterConsumption(waterConsumption)
                .waterAmount(waterAmount)
                .electricityConsumption(electricityConsumption)
                .electricityAmount(electricityAmount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SettlementListDto> getSettlements(Pageable pageable, Long rentedObjectId, Long tenantId) {
        Page<Settlement> settlementsPage = settlementRepository.findAllByRentedObjectIdAndTenantId(pageable, rentedObjectId, tenantId);
        return settlementsPage.map(settlementMapper::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsSettlementsByTenantId(Long tenantId) {
        return settlementRepository.existsByTenantId(tenantId);
    }
}
