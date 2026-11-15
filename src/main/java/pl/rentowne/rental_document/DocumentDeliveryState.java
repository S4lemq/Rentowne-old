package pl.rentowne.rental_document;

enum DocumentDeliveryState {
    PENDING,
    PROCESSING,
    SENT,
    SIMULATED,
    HELD,
    FAILED,
    UNKNOWN,
    CANCELLED
}
