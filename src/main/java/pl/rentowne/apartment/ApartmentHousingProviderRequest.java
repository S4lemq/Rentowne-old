package pl.rentowne.apartment;

import java.util.List;

record ApartmentHousingProviderRequest(List<Long> housingProviderIds, Long apartmentId) {
}
