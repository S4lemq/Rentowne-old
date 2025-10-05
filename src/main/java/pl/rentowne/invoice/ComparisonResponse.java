package pl.rentowne.invoice;

import pl.rentowne.housing_provider.model.ProviderType;

import java.util.List;

record ComparisonResponse(
        Long housingProviderId,
        String providerName,
        ProviderType providerType,
        List<InvoiceLine> pdfLines,
        List<FieldDiff> differences,
        List<String> notes
) {}
