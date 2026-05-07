package pl.rentowne.invoice;

import pl.rentowne.housing_provider.ProviderType;

record InvoiceFilterDto (
   Long housingProviderId,
   Long apartmentId,
   String monthFrom,
   String monthTo,
   String payDay,
   String payDayMonth,
   ProviderType category
){}

