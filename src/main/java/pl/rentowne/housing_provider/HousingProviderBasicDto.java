package pl.rentowne.housing_provider;

record HousingProviderBasicDto(
        Long id,
        String name,
        ProviderType type,
        boolean recurringPayment
) {
}
