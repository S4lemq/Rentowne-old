package pl.rentowne.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardDto(
        LocalDate asOf,
        int months,
        Summary summary,
        List<Month> trend,
        Attention attention
) {
    public record Summary(BigDecimal receivedThisMonth, BigDecimal paidInvoicesThisMonth,
                          BigDecimal amountDue, long tenantsWithBalance,
                          long apartments, long rentedUnits, long totalUnits) {}

    public record Month(LocalDate month, BigDecimal received, BigDecimal paidInvoices) {}

    public record Attention(Preview<TenantDue> tenants, Preview<InvoiceDue> invoices,
                            Preview<ExpiringAgreement> agreements) {}

    public record Preview<T>(long total, List<T> items) {}

    public record TenantDue(long tenantId, long rentedObjectId, String tenantName,
                            String apartmentName, String rentedObjectName, boolean active,
                            BigDecimal amountDue) {}

    public record InvoiceDue(long id, String number, String providerName, String apartmentName,
                             BigDecimal amount, LocalDate dueDate, boolean overdue) {}

    public record ExpiringAgreement(long tenantId, String tenantName, String apartmentName,
                                    String rentedObjectName, LocalDate endDate) {}
}
