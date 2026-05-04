package pl.rentowne.invoice;

import pl.rentowne.housing_provider.ProviderType;

record InvoiceFilterDto (
   Long housingProviderId,
   Long apartmentId,
   String month,
   String payDay,
   String payDayMonth,
   ProviderType category
){}

