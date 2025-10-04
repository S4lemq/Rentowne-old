package pl.rentowne.settlement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.rentowne.lease_agreement.model.dto.LeaseAgreementDto;
import pl.rentowne.rented_object.model.dto.CalculateRequestDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serwis odpowiedzialny za generowanie raportów z rozliczeń
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SettlementReportService {

    private final SettlementLogWriter logWriter;

    /**
     * Inicjalizuje raport rozliczenia
     */
    public void initializeReport(String fileNameData, LocalDateTime settlementDate) {
        logWriter.initializeLogFile(fileNameData, settlementDate);
        logWriter.writeLine("ROZPOCZĘCIE ROZLICZENIA");
    }

    /**
     * Raportuje podstawowe informacje o rozliczeniu
     */
    public void reportBasicInfo(Long rentedObjectId, CalculateRequestDto dto, int rentedObjectsInApartment) {
        logWriter.writeField("ID obiektu wynajmowanego", rentedObjectId);
        logWriter.writeSeparator();
        logWriter.writeLine("");
        logWriter.writeLine("DANE PODSTAWOWE:");
        logWriter.writeField("Liczba obiektów w mieszkaniu", rentedObjectsInApartment);
        logWriter.writeField("Czy rozliczana woda", dto.isWaterIncluded() ? "TAK" : "NIE");
        logWriter.writeField("Czy rozliczana elektryczność", dto.isElectricityIncluded() ? "TAK" : "NIE");
        logWriter.writeSeparator();
    }

    /**
     * Rozpoczyna sekcję obliczania kosztów elektryczności
     */
    public void startElectricitySection() {
        logWriter.writeLine("");
        logWriter.writeLine("OBLICZANIE KOSZTÓW ELEKTRYCZNOŚCI:");
    }

    /**
     * Raportuje wynik obliczeń elektryczności
     */
    public void reportElectricityResult(BigDecimal amount) {
        logWriter.writeField("Kwota za prąd", amount + " PLN");
    }

    /**
     * Rozpoczyna sekcję obliczania kosztów wody
     */
    public void startWaterSection() {
        logWriter.writeLine("");
        logWriter.writeLine("OBLICZANIE KOSZTÓW WODY:");
    }

    /**
     * Raportuje wynik obliczeń wody
     */
    public void reportWaterResult(BigDecimal amount) {
        logWriter.writeField("Kwota za wodę", amount + " PLN");
    }

    /**
     * Raportuje podsumowanie rozliczenia
     */
    public void reportSummary(BigDecimal electricResult,
                             BigDecimal waterResult,
                             BigDecimal gasResult,
                             LeaseAgreementDto leaseAgreement,
                             BigDecimal totalSum) {
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
    }

    /**
     * Raportuje utworzenie rozliczenia
     */
    public void reportSettlementCreated(Long settlementId) {
        logWriter.writeLine("");
        logWriter.writeField("ID utworzonego rozliczenia", settlementId);
    }

    /**
     * Raportuje rozliczone odczyty liczników
     */
    public void reportSettledMeterReadings(List<Long> meterReadingIds) {
        if (!meterReadingIds.isEmpty()) {
            logWriter.writeLine("");
            logWriter.writeLine("ROZLICZONE ODCZYTY LICZNIKÓW:");
            logWriter.writeField("Liczba rozliczonych odczytów", meterReadingIds.size());
            logWriter.writeField("ID odczytów", meterReadingIds.toString());
        }
    }

    /**
     * Raportuje domknięcie cyklu rozliczeniowego dla elektryczności
     */
    public void reportCycleClosure(Long apartmentId, int closedReadingsCount) {
        logWriter.writeLine("");
        logWriter.writeLine("DOMKNIĘCIE CYKLU (ELEKTRYKA):");
        logWriter.writeField("ID mieszkania", apartmentId);
        logWriter.writeField("Zamknięto odczytów (apartmentSettled=true)", closedReadingsCount);
    }

    /**
     * Raportuje pomyślne zakończenie rozliczenia
     */
    public void reportSuccess() {
        logWriter.writeLine("");
        logWriter.writeLine("ROZLICZENIE ZAKOŃCZONE POMYŚLNIE");
    }

    /**
     * Raportuje błąd podczas rozliczenia
     */
    public void reportError(Exception e) {
        logWriter.writeLine("");
        logWriter.writeLine("BŁĄD PODCZAS ROZLICZENIA:");
        logWriter.writeField("Komunikat błędu", e.getMessage());
    }

    /**
     * Zamyka raport
     */
    public void closeReport() {
        logWriter.closeLogFile();
    }
}

