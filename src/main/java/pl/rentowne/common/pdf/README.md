# Pakiet PDF - Silnik Generowania Dokumentów PDF

## Architektura

Ten pakiet realizuje wzorzec **Facade** z rygorystyczną enkapsulacją.

### Punkt Wejścia (API Publiczne)

- **`PdfFacade`** - jedyna publiczna klasa w pakiecie, stanowi punkt wejścia do funkcjonalności generowania PDF

### Komponenty Wewnętrzne (Package-Private)

Wszystkie pozostałe klasy są package-private i niedostępne spoza pakietu:

- `PdfGeneratorService` - główny serwis generujący dokumenty PDF
- `PdfConfiguration` - konfiguracja czcionek i parametrów
- `PdfGenerationException` - wyjątek rzucany przy błędach generowania

### Modele Publiczne (API)

Klasy modeli są publiczne, ponieważ muszą być używane przez klientów fasady:

- `PdfDocument` - model dokumentu PDF do wygenerowania
- `PdfElement` - model pojedynczego elementu dokumentu (tekst, nagłówek, tytuł)

## Użycie

```java
@Service
@RequiredArgsConstructor
public class SettlementService {
    
    private final PdfFacade pdfFacade;
    
    public byte[] generateSettlementPdf(Settlement settlement) {
        PdfDocument document = PdfDocument.builder()
            .title("Rozliczenie " + settlement.getId())
            .author("System Rentowne")
            .elements(List.of(
                PdfElement.title("Rozliczenie najmu"),
                PdfElement.heading("Dane podstawowe"),
                PdfElement.text("Okres: " + settlement.getPeriod()),
                PdfElement.boldText("Suma: " + settlement.getAmount() + " PLN")
            ))
            .build();
            
        return pdfFacade.generatePdf(document);
    }
}
```

## Zasady

1. **Nigdy nie używaj bezpośrednio** klas package-private (`PdfGeneratorService`, `PdfConfiguration`) spoza pakietu
2. **Zawsze używaj** `PdfFacade` jako punktu wejścia
3. **Buduj dokumenty** używając builderów `PdfDocument` i `PdfElement`
4. **Czcionka domyślna**: Inter (Regular i Bold) - automatycznie ładowana z zasobów

## Technologie

- **Apache PDFBox 3.0.6** - biblioteka do generowania PDF
- **Czcionka Inter** - zasoby w `/fonts/`
- **Spring Boot 3.5.9** - dependency injection i konfiguracja

## Struktura Pakietu

```
pl.rentowne.common.pdf/
├── PdfFacade.java              (public)   - Punkt wejścia
├── PdfDocument.java            (public)   - Model dokumentu
├── PdfElement.java             (public)   - Model elementu
├── PdfGeneratorService.java    (package)  - Serwis generowania
├── PdfConfiguration.java       (package)  - Konfiguracja
└── PdfGenerationException.java (package)  - Wyjątek
```

## Przykład Kontrolera

```java
@RestController
@RequestMapping("/api/settlements/pdf")
@RequiredArgsConstructor
public class SettlementPdfController {
    
    private final PdfFacade pdfFacade;
    
    @GetMapping("/generate")
    public ResponseEntity<byte[]> generatePdf() {
        PdfDocument document = PdfDocument.builder()
            .title("Dokument")
            .elements(List.of(
                PdfElement.title("Tytuł"),
                PdfElement.text("Treść")
            ))
            .build();
            
        byte[] pdfBytes = pdfFacade.generatePdf(document);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "document.pdf");
        
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
```

