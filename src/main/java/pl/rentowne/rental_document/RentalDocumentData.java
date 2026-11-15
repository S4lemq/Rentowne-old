package pl.rentowne.rental_document;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Immutable contents captured when the financial operation is committed. */
@Builder(toBuilder = true)
public record RentalDocumentData(
        RentalDocumentType type,
        String number,
        LocalDate issueDate,
        String issueCity,
        Party owner,
        Party tenant,
        String representativeName,
        String propertyAddress,
        LocalDate contractSigningDate,
        String period,
        LocalDate terminationDate,
        List<Line> lines,
        BigDecimal total,
        BigDecimal depositPaid,
        BigDecimal result,
        Payment payment,
        String note
) {
    public RentalDocumentData {
        lines = List.copyOf(lines);
    }

    public record Party(String name, List<String> address) {
        public Party { address = List.copyOf(address); }
    }

    public record Line(String description, BigDecimal amount) {}

    public record Payment(String recipient, String accountNumber, LocalDate dueDate, String transferTitle,
                          boolean representativeAccount) {}
}
