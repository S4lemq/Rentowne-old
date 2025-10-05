package pl.rentowne.invoice;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceParser {
    private final ChatClient chatClient;

    public InvoiceParser(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public List<InvoiceLine> parseElectricityInvoice(String pdfText) {
        var converter = new BeanOutputConverter<>(InvoiceLine[].class);
        String schema = converter.getFormat(); // to jest JSON/JSON Schema z klamrami

        String system = """
            Jesteś parserem faktur za ENERGIĘ ELEKTRYCZNĄ.
            Wyodrębnij pozycje {name, price, billingMethod}. Zwróć TYLKO JSON zgodny ze schematem.
            Mapowanie: kWh -> ACCORDING_TO_CONSUMPTION, zł/mc -> MONTHLY. Cena = jednostkowa netto.
            """;

        String user = """
            To jest treść faktury (PL):
            ---------------------------
            %s
            ---------------------------

            Zwróć JSON w formacie (schemat):
            %s
            """.formatted(pdfText, schema);

        String content = chatClient
                .prompt()
                .system(system)
                .user(user)
                .call()
                .content();

        InvoiceLine[] arr = converter.convert(content);
        return arr == null ? List.of() : List.of(arr);
    }
}


