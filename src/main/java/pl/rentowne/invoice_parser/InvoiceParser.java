package pl.rentowne.invoice_parser;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import pl.rentowne.housing_provider.model.ProviderType;
import pl.rentowne.provider_field.model.BillingMethod;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InvoiceParser {

    private final ChatClient chatClient;

    public InvoiceParser(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public record ParsedInvoice(
            String providerName,
            ProviderType providerType,
            BigDecimal taxPercent,
            List<InvoiceLine> items
    ) {}

    public record InvoiceLine(
            String name,
            BigDecimal price,
            BillingMethod billingMethod
    ) {}

    public ParsedInvoice parseForAutofillElectricity(String pdfText) {
        var converter = new BeanOutputConverter<>(ParsedInvoice.class);
        String schema = converter.getFormat();

        String system = """
            Jesteś parserem faktur za ENERGIĘ ELEKTRYCZNĄ w Polsce.
            Zwróć jedynie JSON zgodny ze schematem.
            ZASADY:
            - providerType: ELECTRICITY
            - providerName: wyciągnij z nagłówka/logo/stopki (np. ENEA, TAURON).
            - items: tylko linie taryf/dystrybucji (np. Energia elektryczna czynna, Opłata zmienna sieciowa, Opłata OZE, Opłata abonamentowa,...).
            - billingMethod: kWh -> ACCORDING_TO_CONSUMPTION, zł/mc -> MONTHLY.
            - price: cena jednostkowa netto.
            - taxPercent: jeśli znajdziesz stawkę VAT dla tych pozycji (np. 23), zwróć; inaczej null.
            """;

        String user = """
            PDF (tekst):
            -----------
            %s
            -----------

            Zwróć JSON:
            %s
            """.formatted(pdfText, schema);

        String content = chatClient
                .prompt()
                .system(system)
                .user(user)
                .call()
                .content();

        return converter.convert(content);
    }
}


