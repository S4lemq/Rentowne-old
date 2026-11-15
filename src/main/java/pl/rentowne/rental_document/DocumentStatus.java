package pl.rentowne.rental_document;

import java.time.OffsetDateTime;
import java.util.List;

record DocumentStatus(
        boolean sent, boolean unknown, boolean busy, String latest, OffsetDateTime lastSent) {
    static DocumentStatus of(List<DocumentDelivery> history) {
        var issues =
                history.stream().filter(d -> d.getKind() == DocumentDeliveryKind.ISSUE).toList();
        return new DocumentStatus(
                issues.stream().anyMatch(d -> d.getState() == DocumentDeliveryState.SENT),
                issues.stream().anyMatch(d -> d.getState() == DocumentDeliveryState.UNKNOWN),
                history.stream().anyMatch(d -> d.getState() == DocumentDeliveryState.PROCESSING),
                issues.isEmpty() ? null : issues.getFirst().getState().name(),
                issues.stream()
                        .filter(d -> d.getState() == DocumentDeliveryState.SENT)
                        .map(DocumentDelivery::getFinishedAt)
                        .filter(java.util.Objects::nonNull)
                        .max(OffsetDateTime::compareTo)
                        .orElse(null));
    }

    String label(RentalDocument document) {
        if (document.isCancelled())
            return sent
                    ? "Anulowany — wcześniej wysłany"
                    : unknown ? "Anulowany — wynik wysyłki niepewny" : "Anulowany";
        if (busy) return "Wysyłanie";
        if (unknown) return "Niepewny wynik wysyłki";
        if (sent) return "Wysłany";
        if (document.getError() != null) return "Błąd generowania";
        return document.getFilename() != null ? "Do sprawdzenia" : "Brak PDF";
    }
}
