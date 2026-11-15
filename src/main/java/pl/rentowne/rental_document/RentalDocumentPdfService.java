package pl.rentowne.rental_document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pl.rentowne.pdf.FopPdfRenderer;
import pl.rentowne.pdf.PdfRenderingException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Maps the immutable business snapshot to presentation data; layout belongs to XSL-FO. */
@Service
@RequiredArgsConstructor
public class RentalDocumentPdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final FopPdfRenderer renderer;

    public byte[] generate(RentalDocumentData data) {
        Document xml;
        try { xml = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().newDocument(); }
        catch (ParserConfigurationException error) { throw new PdfRenderingException("Nie można przygotować danych PDF", error); }
        Element root = xml.createElement("document");
        xml.appendChild(root);
        text(root, "heading", data.type().title().toUpperCase(Locale.forLanguageTag("pl-PL")));
        text(root, "number", data.number());
        text(root, "issueDate", date(data.issueDate()));
        text(root, "issueCity", data.issueCity());
        Element parties = child(root, "parties");
        party(parties, "WYNAJMUJĄCY", data.owner(), data.representativeName());
        party(parties, "NAJEMCA", data.tenant(), null);
        Element details = child(root, "details");
        detail(details, "Lokal / obiekt", data.propertyAddress());
        detail(details, "Umowa najmu z dnia", date(data.contractSigningDate()));
        boolean receipt = data.type() == RentalDocumentType.RENT_RECEIPT;
        detail(details, receipt ? "Okres rozliczeniowy" : "Zakończenie najmu", receipt ? data.period() : date(data.terminationDate()));
        Element lines = child(root, "lines");
        if (!receipt) line(lines, "Faktycznie wpłacona kaucja", DocumentMoney.format(data.depositPaid()));
        data.lines().forEach(item -> line(lines, item.description(), DocumentMoney.format(item.amount())));
        if (!receipt) detail(child(root, "deductions"), "Łączna kwota potrąceń", DocumentMoney.format(data.total()));
        Element summary = child(root, "summary");
        text(summary, "label", receipt ? "Razem należności za okres" : data.result().signum() > 0
                ? "Do zwrotu najemcy" : data.result().signum() < 0 ? "Do dopłaty przez najemcę" : "Brak zwrotu i dopłaty");
        text(summary, "amount", DocumentMoney.format(receipt ? data.total() : data.result().abs()));
        text(summary, "words", DocumentMoney.words(receipt ? data.total() : data.result().abs()));
        if (data.payment() != null) {
            var payment = data.payment();
            Element target = child(root, "payment");
            text(target, "heading", !receipt && data.result().signum() > 0 ? "ZWROT KAUCJI" : "PŁATNOŚĆ");
            if (payment.dueDate() != null) detail(target, receipt ? "Termin płatności" : "Termin zwrotu", date(payment.dueDate()));
            detail(target, "Forma płatności", "Przelew na rachunek bankowy");
            detail(target, "Odbiorca przelewu", payment.recipient());
            detail(target, "Numer rachunku", DocumentMoney.account(payment.accountNumber()));
            detail(target, "Tytuł przelewu", payment.transferTitle());
            text(target, "representativeAccount", Boolean.toString(payment.representativeAccount()));
        }
        text(root, "note", data.note());
        return renderer.render(receipt ? "receipt" : "deposit-settlement", data.type().title() + " " + data.number(), xml);
    }

    private static void party(Element parent, String label, RentalDocumentData.Party data, String representative) {
        Element party = child(parent, "party");
        party.setAttribute("label", label);
        text(party, "name", data.name());
        data.address().forEach(line -> text(party, "address", line));
        text(party, "representative", representative);
    }

    private static void line(Element parent, String label, String amount) {
        Element line = child(parent, "line");
        text(line, "description", label);
        text(line, "amount", amount);
    }

    private static void detail(Element parent, String label, String value) {
        if (value == null || value.isBlank()) return;
        text(parent, "detail", value).setAttribute("label", label);
    }

    private static Element child(Element parent, String name) {
        Element element = parent.getOwnerDocument().createElement(name);
        parent.appendChild(element);
        return element;
    }

    private static Element text(Element parent, String name, String value) {
        Element element = child(parent, name);
        // Soft break opportunities also handle unusually long unbroken user-entered text.
        element.setTextContent(value == null ? "" : value.replaceAll("(\\S{24})(?=\\S)", "$1\u200B"));
        return element;
    }

    private static String date(LocalDate date) { return date == null ? null : DATE.format(date); }
}
