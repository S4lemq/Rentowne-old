package pl.rentowne.settlement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import pl.rentowne.settlement.model.Settlement;
import pl.rentowne.settlement.model.SettlementStatus;
import pl.rentowne.settlement.repository.SettlementRepository;
import pl.rentowne.settlement.service.SettlementLogWriter;
import pl.rentowne.settlement.service.SettlementService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
    private final SettlementLogWriter logWriter;

    @Transactional
    @Override
    public void calculate(Long rentedObjectId, CalculateRequestDto dto) {
        // Inicjalizacja pliku logu
        String fileNameData = rentedObjectRepository.getSettlementFileNameData(rentedObjectId);
        logWriter.initializeLogFile(fileNameData, dto.getSettlementDate());

        try {
            logWriter.writeLine("ROZPOCZĘCIE ROZLICZENIA");
            logWriter.writeField("ID obiektu wynajmowanego", rentedObjectId);
            logWriter.writeSeparator();

            List<HousingProvider> providerWithProviderFields = housingProviderRepository.findHousingProviderWithProviderFieldsByRentedObjectId(rentedObjectId);
            LeaseAgreementDto leaseAgreement = leaseAgreementRepository.getData(rentedObjectId);
            int rentedObjectsInApartment = apartmentService.getCountOfRentedObjectsInApartment(rentedObjectId);

            logWriter.writeLine("");
            logWriter.writeLine("DANE PODSTAWOWE:");
            logWriter.writeField("Liczba obiektów w mieszkaniu", rentedObjectsInApartment);
            logWriter.writeField("Czy rozliczana woda", dto.isWaterIncluded() ? "TAK" : "NIE");
            logWriter.writeField("Czy rozliczana elektryczność", dto.isElectricityIncluded() ? "TAK" : "NIE");
            logWriter.writeSeparator();

            List<Long> allMeterReadingIds = new ArrayList<>();
            BigDecimal electricResult = BigDecimal.ZERO;
            BigDecimal gasResult;
            BigDecimal waterResult = BigDecimal.ZERO;

            if (dto.isElectricityIncluded()) {
                logWriter.writeLine("");
                logWriter.writeLine("OBLICZANIE KOSZTÓW ELEKTRYCZNOŚCI:");
                electricResult = this.calculateElectricityCost(providerWithProviderFields, rentedObjectId, electricResult, allMeterReadingIds, rentedObjectsInApartment);
                logWriter.writeField("Kwota za prąd", electricResult + " PLN");
            }

            if (dto.isWaterIncluded()) {
                logWriter.writeLine("");
                logWriter.writeLine("OBLICZANIE KOSZTÓW WODY:");
                waterResult = this.calculateWaterCost(providerWithProviderFields, rentedObjectId, waterResult, allMeterReadingIds, leaseAgreement);
                logWriter.writeField("Kwota za wodę", waterResult + " PLN");
            }

            gasResult = leaseAgreement.getGasDeposit() != null ? leaseAgreement.getGasDeposit() : BigDecimal.ZERO;

            BigDecimal totalSum = electricResult.add(waterResult).add(leaseAgreement.getCompensationAmount()).add(leaseAgreement.getInternetFee()).add(leaseAgreement.getRentAmount());

            // Zapis do standardowych logów
            log.info("Rozliczenie obiektu o id: {}", rentedObjectId);
            log.info("Rozliczano: wodę-{}, prąd-{}", dto.isWaterIncluded(), dto.isElectricityIncluded());
            log.info("Kwota za prąd: {}", electricResult);
            log.info("Kwota za wodę: {}", waterResult);
            log.info("Kwota za gaz: {}", gasResult);
            log.info("Kwota odstępnego: {}", leaseAgreement.getCompensationAmount());
            log.info("Kwota za internet: {}", leaseAgreement.getInternetFee());
            log.info("Kwota czynszu: {}", leaseAgreement.getRentAmount());
            log.info("Kwota całkowita: {}", totalSum);

            // Zapis do pliku
            logWriter.writeLine("");
            logWriter.writeSeparator();
            logWriter.writeLine("PODSUMOWANIE ROZLICZENIA:");
            logWriter.writeSeparator();
            logWriter.writeField("Kwota za prąd", electricResult + " PLN");
            logWriter.writeField("Kwota za wodę", waterResult + " PLN");
            logWriter.writeField("Kwota za gaz", gasResult + " PLN");
            logWriter.writeField("Kwota odstępnego", leaseAgreement.getCompensationAmount() + " PLN");
            logWriter.writeField("Kwota za internet", leaseAgreement.getInternetFee() + " PLN");
            logWriter.writeField("Kwota czynszu", leaseAgreement.getRentAmount() + " PLN");
            logWriter.writeSeparator();
            logWriter.writeField("KWOTA CAŁKOWITA", totalSum + " PLN");
            logWriter.writeSeparator();

            Settlement settlement = Settlement.builder()
                    .electricityAmount(electricResult)
                    .waterAmount(waterResult)
                    .gasAmount(gasResult)
                    .totalAmount(totalSum)
                    .settlementDate(dto.getSettlementDate())
                    .rentedObject(RentedObject.builder().id(rentedObjectId).build())
                    .settlementStatus(SettlementStatus.TO_PAY)
                    .build();

            Long settlementId = settlementRepository.saveAndFlush(settlement).getId();

            logWriter.writeLine("");
            logWriter.writeField("ID utworzonego rozliczenia", settlementId);

            if (!allMeterReadingIds.isEmpty()) {
                meterReadingRepository.updateSettled(allMeterReadingIds, settlementId);
                logWriter.writeLine("");
                logWriter.writeLine("ROZLICZONE ODCZYTY LICZNIKÓW:");
                logWriter.writeField("Liczba rozliczonych odczytów", allMeterReadingIds.size());
                logWriter.writeField("ID odczytów", allMeterReadingIds.toString());
            }

            log.info("W następujących odczytach liczników ustawiono odczyty na rozliczone: {}", allMeterReadingIds);

            rentedObjectRepository.updateSettlement(rentedObjectId, totalSum, dto.getSettlementDate());

            logWriter.writeLine("");
            logWriter.writeLine("ROZLICZENIE ZAKOŃCZONE POMYŚLNIE");

        } catch (Exception e) {
            logWriter.writeLine("");
            logWriter.writeLine("BŁĄD PODCZAS ROZLICZENIA:");
            logWriter.writeField("Komunikat błędu", e.getMessage());
            log.error("Błąd podczas rozliczenia obiektu {}", rentedObjectId, e);
            throw e;
        } finally {
            logWriter.closeLogFile();
        }
    }

    @Override
    @Transactional
    public void deleteCalculate(final Long settlementId) {
        meterReadingRepository.updateSettledToFalseBySettlementId(settlementId);
        rentedObjectRepository.clearLastSettlementData(settlementId);
        settlementRepository.deleteById(settlementId);
    }

    private Optional<HousingProvider> findProvider(List<HousingProvider> providers, ProviderType type) {
        return providers.stream()
                .filter(dto -> type.equals(dto.getType()))
                .findFirst();
    }

    public BigDecimal calculateElectricityCost(List<HousingProvider> providerWithProviderFields,
                                               Long rentedObjectId,
                                               BigDecimal electricResult,
                                               List<Long> allMeterReadingIds,
                                               final int rentedObjectsCount) {

        Optional<HousingProvider> electricProviderOptional = findProvider(providerWithProviderFields, ProviderType.ELECTRICITY);
        if (electricProviderOptional.isEmpty()) {
            return BigDecimal.ZERO;
        }
        HousingProvider provider = electricProviderOptional.get();
        BigDecimal vatPercent = Optional.ofNullable(provider.getTax()).orElse(BigDecimal.ZERO); // np. 23

        // --- 1) Identyfikacja mieszkania i kawalerek ---
        Long apartmentId = rentedObjectRepository.findApartmentIdByRentedObjectId(rentedObjectId);
        List<Long> rentedObjectIds = rentedObjectRepository.findRentedObjectIdsByApartmentId(apartmentId);
        int count = rentedObjectIds.size(); // powinno być to samo co rentedObjectsCount, ale liczymy pewnie

        // --- 2) SUMA mieszkania (NETTO) ---
        // 2a) Suma zużycia w mieszkaniu
        BigDecimal totalConsumption = rentedObjectIds.stream()
                .map(roId -> getConsumptionForRentedObject(roId, MeterType.ELECTRIC))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2b) Część zmienna wg zużycia – liczona “jak na fakturze”: (stawka * suma_zużycia) per stawka, zaokrąglenie do 2, potem suma
        List<ProviderField> fields = provider.getProviderFields();
        BigDecimal variableNetTotal = variableNetForConsumption(totalConsumption, fields);

        // 2c) Część miesięczna (suma wszystkich pozycji miesięcznych dla mieszkania)
        BigDecimal monthlySum = sumMonthlyForProvider(provider); // np. 22.86

        // 2d) NETTO mieszkania
        BigDecimal apartmentNet = variableNetTotal.add(monthlySum); // np. 73.41 + 22.86 = 96.27

        // 2e) BRUTTO mieszkania (VAT raz od całości)
        BigDecimal apartmentGross = r2(apartmentNet.multiply(vatFactor(vatPercent))); // np. 118.41

        // --- 3) Rozbicie NETTO na kawalerki (żeby potem rozdzielić BRUTTO spójnie) ---
        Map<Long, BigDecimal> netPerRo = new HashMap<>();
        for (Long roId : rentedObjectIds) {
            BigDecimal cons = getConsumptionForRentedObject(roId, MeterType.ELECTRIC);
            // zmienna część per ro
            BigDecimal variableNet = variableNetForConsumption(cons, fields);
            // miesięczna część dzielona równo
            BigDecimal monthlyShareNet = monthlyShare(monthlySum, count);
            BigDecimal net = variableNet.add(monthlyShareNet).setScale(2, RoundingMode.HALF_UP);
            netPerRo.put(roId, net);
        }

        // --- 4) Alokacja BRUTTO na kawalerki (dopina grosze) ---
        Map<Long, BigDecimal> grossPerRo = allocateGrossPerRentedObject(netPerRo, apartmentGross, vatPercent);

        // --- 5) Zwracamy BRUTTO dla aktualnej kawalerki + obsługa odczytów (jak wcześniej) ---
        // Zbieramy odczyty TYLKO dla bieżącego roId, aby je oznaczyć jako rozliczone
        List<MeterReadingDto> meterReadingDtos =
                meterRepository.getConsumptionAndIdByRentedObjectAndMeterType(rentedObjectId, MeterType.ELECTRIC);
        allMeterReadingIds.addAll(meterReadingDtos.stream().map(MeterReadingDto::getId).toList());

        BigDecimal resultGrossForCurrent = grossPerRo.getOrDefault(rentedObjectId, BigDecimal.ZERO);

        log.debug("[ELEKTRYCZNOŚĆ][ALLOC] aptId={} roId={} totalConsumption={} apartmentNet={} apartmentGross={} roNet={} roGross={}",
                apartmentId, rentedObjectId, totalConsumption, apartmentNet, apartmentGross,
                netPerRo.get(rentedObjectId), resultGrossForCurrent);

        return electricResult.add(resultGrossForCurrent);
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

    private BigDecimal getConsumptionForRentedObject(Long roId, MeterType type) {
        List<MeterReadingDto> dtos = meterRepository.getConsumptionAndIdByRentedObjectAndMeterType(roId, type);
        return dtos.stream()
                .map(MeterReadingDto::getConsumption)
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

}
