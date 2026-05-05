package pl.rentowne.settlement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
class SettlementLogWriter {

    private static final String BASE_LOG_DIR = "C:/Rentowne/settlement_logs";
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private BufferedWriter currentWriter;
    private String currentFilePath;

    /**
     * Tworzy nowy plik logu dla rozliczenia
     * @param fileNameData dane do nazwy pliku (imię_nazwisko_obiekt)
     * @param settlementDate data rozliczenia
     */
    public void initializeLogFile(String fileNameData, LocalDateTime settlementDate) {
        try {
            // Tworzenie katalogu, jeśli nie istnieje
            Path logDir = Paths.get(BASE_LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            // Czyszczenie nazwy pliku z niedozwolonych znaków
            String sanitizedName = fileNameData != null ? fileNameData.replaceAll("[^a-zA-Z0-9_-]", "_") : "rozliczenie";
            String dateStr = settlementDate.format(FILE_DATE_FORMATTER);
            String fileName = String.format("%s_%s.txt", sanitizedName, dateStr);

            currentFilePath = Paths.get(BASE_LOG_DIR, fileName).toString();
            currentWriter = new BufferedWriter(new FileWriter(currentFilePath, false));

            // Nagłówek pliku
            writeLine("=".repeat(80));
            writeLine("RAPORT ROZLICZENIA");
            writeLine("Data wygenerowania: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writeLine("Data rozliczenia: " + settlementDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writeLine("=".repeat(80));
            writeLine("");

            log.info("Zainicjalizowano plik logu rozliczenia: {}", currentFilePath);
        } catch (IOException e) {
            log.error("Błąd podczas tworzenia pliku logu: {}", e.getMessage(), e);
        }
    }

    /**
     * Zapisuje linię do pliku logu
     */
    public void writeLine(String message) {
        if (currentWriter != null) {
            try {
                currentWriter.write(message);
                currentWriter.newLine();
                currentWriter.flush(); // Natychmiastowy zapis
            } catch (IOException e) {
                log.error("Błąd podczas zapisu do pliku logu: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Zapisuje sformatowaną linię z wartością
     */
    public void writeField(String fieldName, Object value) {
        writeLine(String.format("%-40s: %s", fieldName, value != null ? value.toString() : "N/A"));
    }

    /**
     * Zapisuje separator
     */
    public void writeSeparator() {
        writeLine("-".repeat(80));
    }

    /**
     * Zamyka plik logu
     */
    public void closeLogFile() {
        if (currentWriter != null) {
            try {
                writeLine("");
                writeLine("=".repeat(80));
                writeLine("Koniec raportu");
                writeLine("=".repeat(80));
                currentWriter.close();
                log.info("Zamknięto plik logu rozliczenia: {}", currentFilePath);
                currentFilePath = null;
                currentWriter = null;
            } catch (IOException e) {
                log.error("Błąd podczas zamykania pliku logu: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Sprawdza, czy plik logu jest aktywny
     */
    public boolean isActive() {
        return currentWriter != null;
    }
}


