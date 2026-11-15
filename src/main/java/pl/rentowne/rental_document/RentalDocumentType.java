package pl.rentowne.rental_document;

public enum RentalDocumentType {
    RENT_RECEIPT("Rachunek", "rachunek"),
    DEPOSIT_SETTLEMENT("Rozliczenie kaucji", "rozliczenie-kaucji");

    private final String title;
    private final String filePrefix;

    RentalDocumentType(String title, String filePrefix) {
        this.title = title;
        this.filePrefix = filePrefix;
    }

    public String title() { return title; }
    public String filePrefix() { return filePrefix; }
}
