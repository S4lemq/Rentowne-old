package pl.rentowne.invoice.model.dto;

import pl.rentowne.housing_provider.ProviderType;

public record InvoiceFilterDto (
   Long housingProviderId,
   Long apartmentId,
   String month,
   String payDay,
   String payDayMonth,
   ProviderType category
){}

