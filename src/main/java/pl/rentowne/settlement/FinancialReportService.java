package pl.rentowne.settlement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.apartment.ApartmentRepository;
import pl.rentowne.housing_provider.ProviderType;
import pl.rentowne.invoice.Invoice;
import pl.rentowne.invoice.InvoiceRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialReportService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.085"); // 8.5% podatku od odstępnego

    private final SettlementRepository settlementRepository;
    private final InvoiceRepository invoiceRepository;
    private final ApartmentRepository apartmentRepository;

    @Transactional(readOnly = true)
    public YearlyFinancialReportDto getMonthlyFinancialReport() {
        return getMonthlyFinancialReport(null);
    }

    @Transactional(readOnly = true)
    public YearlyFinancialReportDto getMonthlyFinancialReport(Integer year) {
        log.info("Generowanie raportu finansowego miesięcznego" + (year != null ? " dla roku " + year : ""));

        // Zbierz wszystkie unikalne miesiące z Settlement i Invoice
        Set<LocalDate> months = collectAllMonths();

        // Filtruj po roku jeśli podano
        if (year != null) {
            months = months.stream()
                    .filter(month -> month.getYear() == year)
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        List<MonthlyFinancialReportDto> reports = new ArrayList<>();
        BigDecimal cumulativeNetIncome = BigDecimal.ZERO;
        BigDecimal yearlyTotalRevenue = BigDecimal.ZERO;
        BigDecimal yearlyTotalCosts = BigDecimal.ZERO;

        for (LocalDate month : months) {
            int yearValue = month.getYear();
            int monthValue = month.getMonthValue();

            // Przychód: suma totalAmount z Settlement według settlementDate
            BigDecimal totalRevenue = settlementRepository.sumTotalAmountByMonth(yearValue, monthValue);

            // Koszty: suma amount z Invoice według payDayMonth
            BigDecimal invoiceCosts = invoiceRepository.sumAmountByPayDayMonth(yearValue, monthValue);

            // Koszty: podatek 8.5% od odstępnego z Settlement
            BigDecimal compensationAmount = settlementRepository.sumCompensationAmountByMonth(yearValue, monthValue);
            BigDecimal compensationTax = compensationAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);

            // Całkowite koszty
            BigDecimal totalCosts = invoiceCosts.add(compensationTax);

            // Dochód netto = przychód - koszty
            BigDecimal netIncome = totalRevenue.subtract(totalCosts);

            // Suma dochodu narastająco
            cumulativeNetIncome = cumulativeNetIncome.add(netIncome);

            // Sumuj roczne przychody i koszty
            yearlyTotalRevenue = yearlyTotalRevenue.add(totalRevenue);
            yearlyTotalCosts = yearlyTotalCosts.add(totalCosts);

            MonthlyFinancialReportDto report = MonthlyFinancialReportDto.builder()
                    .month(month)
                    .totalRevenue(totalRevenue)
                    .totalCosts(totalCosts)
                    .netIncome(netIncome)
                    .netIncomeSum(cumulativeNetIncome)
                    .build();

            reports.add(report);
        }

        log.info("Wygenerowano raport finansowy dla {} miesięcy", reports.size());

        return YearlyFinancialReportDto.builder()
                .months(reports)
                .yearlyTotalRevenue(yearlyTotalRevenue)
                .yearlyTotalCosts(yearlyTotalCosts)
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlyFinancialDetailDto getMonthlyFinancialDetail(LocalDate month) {
        log.info("Generowanie szczegółów raportu finansowego dla miesiąca: {}", month);

        int year = month.getYear();
        int monthValue = month.getMonthValue();

        // Zbierz wszystkie unikalne ID mieszkań z faktur i rozliczeń
        Set<Long> apartmentIds = new HashSet<>();
        apartmentIds.addAll(invoiceRepository.findDistinctApartmentIdsByMonth(year, monthValue));
        apartmentIds.addAll(settlementRepository.findDistinctApartmentIdsByMonth(year, monthValue));

        if (apartmentIds.isEmpty()) {
            log.warn("Brak danych dla miesiąca: {}", month);
            return MonthlyFinancialDetailDto.builder()
                    .month(month)
                    .summary(createEmptySummary())
                    .apartments(new ArrayList<>())
                    .build();
        }

        // Pobierz informacje o mieszkaniach
        List<Apartment> apartments = apartmentRepository.findByIdInOrderByApartmentName(new ArrayList<>(apartmentIds));

        // Oblicz szczegóły dla każdego mieszkania
        List<ApartmentFinancialDetailDto> apartmentDetails = new ArrayList<>();

        for (Apartment apartment : apartments) {
            ApartmentFinancialDetailDto detail = calculateApartmentFinancialDetail(apartment, year, monthValue);
            apartmentDetails.add(detail);
        }

        // Sortuj po nazwie mieszkania - najpierw alfabetycznie po ulicy, potem numerycznie po numerze
        apartmentDetails.sort((a1, a2) -> {
            String street1 = extractStreetNameFromApartmentName(a1.apartmentName());
            String street2 = extractStreetNameFromApartmentName(a2.apartmentName());

            // Najpierw porównaj nazwy ulic
            int streetComparison = street1.compareTo(street2);
            if (streetComparison != 0) {
                return streetComparison;
            }

            // Jeśli ulice są takie same, porównaj numery
            Integer num1 = extractNumberFromApartmentName(a1.apartmentName());
            Integer num2 = extractNumberFromApartmentName(a2.apartmentName());

            if (num1 != null && num2 != null) {
                return num1.compareTo(num2);
            }

            // Jeśli nie ma numerów, sortuj alfabetycznie
            return a1.apartmentName().compareTo(a2.apartmentName());
        });

        // Oblicz sumyz
        MonthlyFinancialSummaryDto summary = calculateSummary(apartmentDetails, year, monthValue);

        return MonthlyFinancialDetailDto.builder()
                .month(month)
                .summary(summary)
                .apartments(apartmentDetails)
                .build();
    }

    private ApartmentFinancialDetailDto calculateApartmentFinancialDetail(Apartment apartment, int year, int month) {
        Long apartmentId = apartment.getId();

        // Pobierz kwoty faktur według typu
        BigDecimal administrativeFee = invoiceRepository.sumAmountByApartmentMonthAndType(
                apartmentId, year, month, ProviderType.ADMINISTRATIVE_FEE);
        BigDecimal internet = invoiceRepository.sumAmountByApartmentMonthAndType(
                apartmentId, year, month, ProviderType.INTERNET);
        BigDecimal electricity = invoiceRepository.sumAmountByApartmentMonthAndType(
                apartmentId, year, month, ProviderType.ELECTRICITY);
        BigDecimal gas = invoiceRepository.sumAmountByApartmentMonthAndType(
                apartmentId, year, month, ProviderType.GAS);

        // Oblicz podatek 8.5% od odstępnego
        BigDecimal compensationAmount = settlementRepository.sumCompensationAmountByApartmentAndMonth(
                apartmentId, year, month);
        BigDecimal tax = compensationAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);

        // Przychód = totalAmount z Settlement dla danego mieszkania
        BigDecimal revenue = settlementRepository.sumTotalAmountByApartmentAndMonth(apartmentId, year, month);

        // Koszty = faktury + podatek
        BigDecimal costs = administrativeFee
                .add(internet)
                .add(electricity)
                .add(gas)
                .add(tax);

        // Dochód netto = przychód - koszty
        BigDecimal netIncome = revenue.subtract(costs);

        return ApartmentFinancialDetailDto.builder()
                .apartmentId(apartmentId)
                .apartmentName(apartment.getApartmentName())
                .administrativeFee(administrativeFee.compareTo(BigDecimal.ZERO) == 0 ? null : administrativeFee)
                .internet(internet.compareTo(BigDecimal.ZERO) == 0 ? null : internet)
                .electricity(electricity.compareTo(BigDecimal.ZERO) == 0 ? null : electricity)
                .gas(gas.compareTo(BigDecimal.ZERO) == 0 ? null : gas)
                .tax(tax.compareTo(BigDecimal.ZERO) == 0 ? null : tax)
                .revenue(revenue)
                .costs(costs)
                .netIncome(netIncome)
                .build();
    }

    private MonthlyFinancialSummaryDto calculateSummary(List<ApartmentFinancialDetailDto> apartmentDetails,
                                                        int year, int month) {
        BigDecimal totalAdministrativeFee = BigDecimal.ZERO;
        BigDecimal totalInternet = BigDecimal.ZERO;
        BigDecimal totalElectricity = BigDecimal.ZERO;
        BigDecimal totalGas = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCosts = BigDecimal.ZERO;
        BigDecimal netIncome = BigDecimal.ZERO;

        for (ApartmentFinancialDetailDto detail : apartmentDetails) {
            totalAdministrativeFee = totalAdministrativeFee.add(
                    detail.administrativeFee() != null ? detail.administrativeFee() : BigDecimal.ZERO);
            totalInternet = totalInternet.add(
                    detail.internet() != null ? detail.internet() : BigDecimal.ZERO);
            totalElectricity = totalElectricity.add(
                    detail.electricity() != null ? detail.electricity() : BigDecimal.ZERO);
            totalGas = totalGas.add(
                    detail.gas() != null ? detail.gas() : BigDecimal.ZERO);
            totalTax = totalTax.add(
                    detail.tax() != null ? detail.tax() : BigDecimal.ZERO);
            totalRevenue = totalRevenue.add(detail.revenue());
            totalCosts = totalCosts.add(detail.costs());
            netIncome = netIncome.add(detail.netIncome());
        }

        // Oblicz sumę narastającą dochodu (od początku do tego miesiąca włącznie)
        BigDecimal cumulativeNetIncome = calculateCumulativeNetIncome(year, month);

        return MonthlyFinancialSummaryDto.builder()
                .totalAdministrativeFee(totalAdministrativeFee)
                .totalInternet(totalInternet)
                .totalElectricity(totalElectricity)
                .totalGas(totalGas)
                .totalTax(totalTax)
                .totalRevenue(totalRevenue)
                .totalCosts(totalCosts)
                .netIncome(netIncome)
                .cumulativeNetIncome(cumulativeNetIncome)
                .build();
    }

    private BigDecimal calculateCumulativeNetIncome(int targetYear, int targetMonth) {
        // Zbierz wszystkie miesiące do podanego miesiąca włącznie
        Set<LocalDate> months = collectAllMonths();
        LocalDate targetDate = LocalDate.of(targetYear, targetMonth, 1);

        BigDecimal cumulative = BigDecimal.ZERO;

        for (LocalDate month : months) {
            if (month.isAfter(targetDate)) {
                break;
            }

            int year = month.getYear();
            int monthValue = month.getMonthValue();

            // Przychód
            BigDecimal revenue = settlementRepository.sumTotalAmountByMonth(year, monthValue);

            // Koszty
            BigDecimal invoiceCosts = invoiceRepository.sumAmountByPayDayMonth(year, monthValue);
            BigDecimal compensationAmount = settlementRepository.sumCompensationAmountByMonth(year, monthValue);
            BigDecimal compensationTax = compensationAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalCosts = invoiceCosts.add(compensationTax);

            // Dochód netto
            BigDecimal netIncome = revenue.subtract(totalCosts);
            cumulative = cumulative.add(netIncome);
        }

        return cumulative;
    }

    private MonthlyFinancialSummaryDto createEmptySummary() {
        return MonthlyFinancialSummaryDto.builder()
                .totalAdministrativeFee(BigDecimal.ZERO)
                .totalInternet(BigDecimal.ZERO)
                .totalElectricity(BigDecimal.ZERO)
                .totalGas(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .totalRevenue(BigDecimal.ZERO)
                .totalCosts(BigDecimal.ZERO)
                .netIncome(BigDecimal.ZERO)
                .cumulativeNetIncome(BigDecimal.ZERO)
                .build();
    }

    private Set<LocalDate> collectAllMonths() {
        Set<LocalDate> months = new TreeSet<>();

        // Pobierz miesiące z Settlement
        List<LocalDateTime> settlementMonths = settlementRepository.findDistinctMonths();
        for (LocalDateTime dateTime : settlementMonths) {
            months.add(LocalDate.of(dateTime.getYear(), dateTime.getMonthValue(), 1));
        }

        // Pobierz miesiące z Invoice (payDayMonth)
        List<LocalDate> invoiceMonths = invoiceRepository.findAll().stream()
                .map(Invoice::getPayDayMonth)
                .filter(Objects::nonNull)
                .map(payDayMonth -> LocalDate.of(payDayMonth.getYear(), payDayMonth.getMonthValue(), 1))
                .distinct()
                .toList();

        months.addAll(invoiceMonths);

        return months;
    }

    private Integer extractNumberFromApartmentName(String apartmentName) {
        try {
            // Zakładamy, że numer mieszkania jest na końcu nazwy po ostatnim spacji
            String[] parts = apartmentName.split(" ");
            String numberPart = parts[parts.length - 1];

            return Integer.parseInt(numberPart);
        } catch (Exception e) {
            return null; // Jeśli nie udało się wyekstrahować numeru, zwróć null
        }
    }

    private String extractStreetNameFromApartmentName(String apartmentName) {
        try {
            // Zakładamy, że nazwa ulicy jest przed ostatnim spacją
            int lastSpaceIndex = apartmentName.lastIndexOf(' ');
            if (lastSpaceIndex == -1) {
                return apartmentName; // Brak spacji, zwróć całą nazwę
            }

            return apartmentName.substring(0, lastSpaceIndex);
        } catch (Exception e) {
            return apartmentName; // W przypadku błędu zwróć całą nazwę mieszkania
        }
    }
}

