package pl.rentowne.apartment;

import java.util.List;

public record ApartmentHousingProviderRequest(List<Long> housingProviderIds, Long apartmentId) {
}
