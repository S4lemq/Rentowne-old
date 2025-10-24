package pl.rentowne.settlement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.invoice.model.Invoice;
import pl.rentowne.invoice.repository.InvoiceRepository;
import pl.rentowne.settlement.model.dto.MonthlyFinancialReportDto;
import pl.rentowne.settlement.repository.SettlementRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    public List<MonthlyFinancialReportDto> getMonthlyFinancialReport() {
        return getMonthlyFinancialReport(null);
    }

    @Transactional(readOnly = true)
    public List<MonthlyFinancialReportDto> getMonthlyFinancialReport(Integer year) {
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
        return reports;
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
}
