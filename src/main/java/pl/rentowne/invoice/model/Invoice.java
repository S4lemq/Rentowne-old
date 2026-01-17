package pl.rentowne.invoice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.common.model.BaseEntity;
import pl.rentowne.housing_provider.HousingProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "INVOICE",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_INVOICE", columnNames = "INVOICE_ID")
        },
        indexes = {
                @Index(name = "PK_INVOICE", columnList = "INVOICE_ID", unique = true)
        }
)
public class Invoice extends BaseEntity {

    @Id
    @SequenceGenerator(name = "invoice_seq", sequenceName = "INVOICE_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_seq")
    @Column(name = "INVOICE_ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HOUSING_PROVIDER_ID")
    private HousingProvider housingProvider;

    @Column(name = "INVOICE_NUMBER", nullable = false, length = 100)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "APARTMENT_ID")
    private Apartment apartment;

    @Column(name = "MONTH")
    private LocalDate month;

    @Column(name = "DATE_FROM")
    private LocalDate dateFrom;

    @Column(name = "DATE_TO")
    private LocalDate dateTo;

    @Column(name = "AMOUNT", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "PAY_DAY")
    private LocalDate payDay;

    @Column(name = "CONSUMPTION", precision = 10, scale = 2)
    private BigDecimal consumption;

    @Column(name = "METER_VALUE", precision = 10, scale = 2)
    private BigDecimal meterValue;

    @Column(name = "PAYMENT_DATE")
    private LocalDate paymentDate;

    @Column(name = "PAY_DAY_MONTH")
    private LocalDate payDayMonth;

    @Column(name = "PDF_FILE_NAME", length = 255)
    private String pdfFileName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invoice invoice = (Invoice) o;
        return Objects.equals(id, invoice.id) && Objects.equals(invoiceNumber, invoice.invoiceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, invoiceNumber);
    }
}
