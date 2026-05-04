package pl.rentowne.payment_card;

public record PaymentCardDto(
        Long id,
        String number,
        String name,
        int month,
        int year,
        int cvv
) {

    public static PaymentCardDto asDto(PaymentCard entity) {
        return new PaymentCardDto(
                entity.getId(),
                entity.getNumber(),
                entity.getName(),
                entity.getMonth(),
                entity.getYear(),
                entity.getCvv()
        );
    }
}
