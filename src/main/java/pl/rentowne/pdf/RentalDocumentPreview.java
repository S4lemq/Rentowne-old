package pl.rentowne.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import pl.rentowne.rental_document.RentalDocumentData;
import pl.rentowne.rental_document.RentalDocumentData.Line;
import pl.rentowne.rental_document.RentalDocumentData.Party;
import pl.rentowne.rental_document.RentalDocumentData.Payment;
import pl.rentowne.rental_document.RentalDocumentPdfService;
import pl.rentowne.rental_document.RentalDocumentType;

import javax.imageio.ImageIO;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Standalone preview tool. Does not start Spring, access a database, or send mail. */
public final class RentalDocumentPreview {
    private RentalDocumentPreview() {}

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "output/pdf");
        Path images = Path.of("target/pdf-preview");
        Files.createDirectories(output);
        Files.createDirectories(images);
        var renderer = new RentalDocumentPdfService(new FopPdfRenderer());
        RentalDocumentData base = RentalDocumentData.builder()
                .type(RentalDocumentType.RENT_RECEIPT).number("01/2027")
                .issueDate(LocalDate.of(2027, 1, 2)).issueCity("Poznań")
                .owner(new Party("Jan Kowalski", List.of("ul. Jasna 12/4", "60-101 Poznań")))
                .tenant(new Party("Anna Nowak", List.of("ul. Kwiatowa 5/12", "60-102 Poznań")))
                .propertyAddress("ul. Zwierzyniecka 14/4, 60-101 Poznań, obiekt 23a")
                .contractSigningDate(LocalDate.of(2026, 6, 15)).period("Styczeń 2027")
                .lines(List.of(line("Czynsz najmu - odstępne", "2000"), line("Czynsz administracyjny", "300"),
                        line("Internet", "40"), line("Zaliczka na gaz", "94.79"), line("Energia elektryczna", "12.88"),
                        line("Woda", "0"), line("Uzupełnienie kaucji", "300"), line("Dodatkowa opłata - klucze", "44"),
                        line("Dodatkowa opłata - krzesła", "500")))
                .total(new BigDecimal("3291.67"))
                .payment(new Payment("Jan Kowalski", "61109010140000071219812874", LocalDate.of(2027, 1, 10),
                        "Rachunek 01/2027 - najem i opłaty - Zwierzyniecka 14/4, obiekt 23a - styczeń 2027", false)).build();
        write(renderer, base, "rachunek", output, images);

        List<Line> longLines = new ArrayList<>(base.lines());
        for (int i = 1; i <= 12; i++) longLines.add(line("Dodatkowe naliczenie " + i
                + " - wymiana elementów wyposażenia zgodnie z protokołem; opis obejmuje materiały oraz wykonanie prac w lokalu."
                + (i == 6 ? " Oznaczenie: " + "ABCDEFGHIJKLMNO".repeat(5) : ""), "25"));
        write(renderer, base.toBuilder().number("02/2027").period("15.01.2027 - 31.01.2027")
                .representativeName("Katarzyna Nowak-Kowalska")
                .lines(longLines).total(new BigDecimal("3591.67"))
                .note("Rozliczenie za niepełny miesiąc. Wybrane składniki naliczono proporcjonalnie do okresu najmu.")
                .payment(new Payment("Katarzyna Nowak-Kowalska", "61109010140000071219812874", LocalDate.of(2027, 1, 10),
                        "Rachunek 02/2027 - Zwierzyniecka 14/4, obiekt 23a - styczeń 2027", true)).build(),
                "rachunek-czesciowy-wielostronicowy", output, images);

        var deposit = base.toBuilder().type(RentalDocumentType.DEPOSIT_SETTLEMENT)
                .terminationDate(LocalDate.of(2026, 12, 31)).depositPaid(new BigDecimal("2500"))
                .lines(List.of(line("Potrącenie za wodę", "120"), line("Potrącenie za energię elektryczną", "150"),
                        line("Końcowe obciążenie za gaz", "100"), line("Inne obciążenie - naprawa wyposażenia", "280")))
                .total(new BigDecimal("650")).result(new BigDecimal("1850"))
                .payment(new Payment("Anna Nowak", "61109010140000071219812874", LocalDate.of(2027, 1, 14),
                        "Zwrot kaucji - umowa z 15.06.2026 - obiekt 23a", false)).build();
        write(renderer, deposit, "kaucja-zwrot", output, images);
        write(renderer, deposit.toBuilder().number("02/2027")
                .lines(List.of(line("Potrącenie za wodę", "120"), line("Potrącenie za energię elektryczną", "150"),
                        line("Końcowe obciążenie za gaz", "100"), line("Inne obciążenie - naprawa wyposażenia", "2740")))
                .total(new BigDecimal("3110")).result(new BigDecimal("-610"))
                .payment(new Payment("Jan Kowalski", "61109010140000071219812874", null, "Dopłata - rozliczenie kaucji 02/2027 - obiekt 23a", false))
                .build(), "kaucja-doplata", output, images);
        write(renderer, deposit.toBuilder().number("03/2027")
                .lines(List.of(line("Potrącenie za wodę", "120"), line("Potrącenie za energię elektryczną", "150"),
                        line("Końcowe obciążenie za gaz", "100"), line("Inne obciążenie - naprawa wyposażenia", "2130")))
                .total(new BigDecimal("2500")).result(BigDecimal.ZERO).payment(null).build(), "kaucja-zero", output, images);
    }

    private static Line line(String label, String amount) { return new Line(label, new BigDecimal(amount)); }

    private static void write(RentalDocumentPdfService renderer, RentalDocumentData data, String name, Path output, Path images) throws Exception {
        byte[] bytes = renderer.generate(data);
        Files.write(output.resolve(name + ".pdf"), bytes);
        try (var pdf = Loader.loadPDF(bytes)) {
            PDFRenderer raster = new PDFRenderer(pdf);
            for (int page = 0; page < pdf.getNumberOfPages(); page++) {
                ImageIO.write(raster.renderImageWithDPI(page, 120), "png", images.resolve(name + "-" + (page + 1) + ".png").toFile());
            }
            System.out.println(name + ": " + pdf.getNumberOfPages() + " stron");
        }
    }
}
