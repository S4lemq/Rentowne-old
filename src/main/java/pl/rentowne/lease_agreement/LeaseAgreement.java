package pl.rentowne.lease_agreement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.rentowne.common.model.BaseEntity;
import pl.rentowne.tenant.Tenant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "LEASE_AGREEMENT",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_LEASE_AGREEMENT", columnNames = "LEASE_AGREEMENT_ID")
        },
        indexes = {
                @Index(name = "PK_LEASE_AGREEMENT", columnList = "LEASE_AGREEMENT_ID", unique = true),
                @Index(name = "I_LEASE_AGREEMENT_1", columnList = "IS_CONTRACT_ACTIVE"),
        }
)
public class LeaseAgreement extends BaseEntity {

    @Id
    @SequenceGenerator(name = "lease_agreement_seq", sequenceName = "LEASE_AGREEMENT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lease_agreement_seq")
    @Column(name = "LEASE_AGREEMENT_ID", nullable = false)
    private Long id;

    @Column(name = "START_CONTRACT_DATE", nullable = false)
    private LocalDateTime startContractDate; //od kiedy najem

    @Column(name = "END_CONTRACT_DATE", nullable = false)
    private LocalDateTime endContractDate; //do kiedy najem

    @Column(name = "DEPOSIT", nullable = false)
    private BigDecimal deposit; //kaucja

    @Column(name = "DEPOSIT_PAID")
    private BigDecimal depositPaid; //wpłacona kaucja

    @Column(name = "DEPOSIT_OPENING_PAID")
    private BigDecimal depositOpeningPaid;

    @Column(name = "DEPOSIT_BALANCE_CONFIRMED", nullable = false)
    private boolean depositBalanceConfirmed;

    @Transient
    private boolean depositAssessed;

    @Column(name = "PAYMENT_DUE_DAY_OF_MONTH", nullable = false)
    private int paymentDueDayOfMonth; //termin płatności

    @Column(name = "RENT_AMOUNT", nullable = false)
    private BigDecimal rentAmount; //kwota czynszu

    @Column(name = "COMPENSATION_AMOUNT", nullable = false)
    private BigDecimal compensationAmount; //kwota odstępnego

    @Column(name = "INTERNET_FEE", nullable = false)
    private BigDecimal internetFee; //kwota za internet

    @Column(name = "GAS_DEPOSIT")
    private BigDecimal gasDeposit; //opłata za gaz

    @Column(name = "INITIAL_ENERGY_METER_READING", nullable = false)
    private BigDecimal initialEnergyMeterReading; //początkowy odczyt energii

    @Column(name = "INITIAL_WATER_METER_READING", nullable = false)
    private BigDecimal initialWaterMeterReading; //początkowy odczyt wody

    @Column(name = "INITIAL_GAS_METER_READING")
    private BigDecimal initialGasMeterReading; //początkowy odczyt gazu

    @Column(name = "DEPOSIT_RETURN_DATE")
    private LocalDateTime depositReturnDate; //data zwrotu kaucji

    @Column(name = "ACTUAL_TERMINATION_DATE")
    private LocalDate actualTerminationDate;

    @Column(name = "DEPOSIT_REFUND_DUE_DATE")
    private LocalDate depositRefundDueDate;

    @Column(name = "RETURNED_DEPOSIT_AMOUNT")
    private BigDecimal returnedDepositAmount; //kwota zwróconej kaucji

    @Column(name = "IS_CONTRACT_ACTIVE", nullable = false)
    private boolean isContractActive; //czy aktywna umowa

    @Column(name = "CONTRACT_SIGNING_DATE", nullable = false)
    private LocalDateTime contractSigningDate; //data podpisania umowy

    @Column(name = "CHARGE_DESCRIPTION")
    private String chargeDescription; //opis obciążenia przy zakończeniu umowy

    @Column(name = "CHARGE_AMOUNT")
    private BigDecimal chargeAmount; //kwota obciążenia przy zakończeniu umowy

    @Column(name = "GAS_CHARGE_AMOUNT")
    private BigDecimal gasChargeAmount; //kwota obciążenia za gaz przy zakończeniu umowy

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "leaseAgreement")
    private Tenant tenant;

}
