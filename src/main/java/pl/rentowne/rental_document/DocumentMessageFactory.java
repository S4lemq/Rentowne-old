package pl.rentowne.rental_document;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import pl.rentowne.common.mail.DocumentMail;

import java.util.Locale;

@Component
@RequiredArgsConstructor
class DocumentMessageFactory {
    private final DocumentMapper mapper;

    DocumentMail create(RentalDocument document, DocumentDelivery delivery, byte[] attachment) {
        var data = mapper.data(document);
        boolean cancellation = delivery.getKind() == DocumentDeliveryKind.CANCELLATION;
        String subject =
                (cancellation ? "Anulowanie — " : "") + data.type().title() + " " + data.number();
        String body =
                cancellation
                        ? "Dokument nr "
                                + data.number()
                                + " został anulowany i nie stanowi aktualnej podstawy płatności."
                        : "W załączniku przesyłamy "
                                + data.type().title().toLowerCase(Locale.forLanguageTag("pl"))
                                + " nr "
                                + data.number()
                                + ".\nLokal: "
                                + data.propertyAddress()
                                + (data.period() == null ? "" : "\nOkres: " + data.period());
        return new DocumentMail(
                delivery.getTo(),
                mapper.addresses(delivery.getCcJson()),
                subject,
                "Dzień dobry,\n\n" + body + "\n\nZ poważaniem,\nRentowne Nieruchomości",
                cancellation ? null : DocumentMapper.filename(data, false),
                attachment);
    }
}
