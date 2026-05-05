package pl.rentowne.payment_card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import pl.rentowne.common.model.BaseEntity;
import pl.rentowne.user.User;

@Getter
@Setter
@Entity
@Table(
        name = "PAYMENT_CARD",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_PAYMENT_CARD", columnNames = "PAYMENT_CARD_ID"),
                @UniqueConstraint(name = "UK_PAYMENT_CARD_1", columnNames = "NUMBER"),
                @UniqueConstraint(name = "UK_PAYMENT_CARD_2", columnNames = "USER_ACCOUNT_ID")
        },
        indexes = {
                @Index(name = "PK_PAYMENT_CARD", columnList = "PAYMENT_CARD_ID", unique = true),
                @Index(name = "UK_PAYMENT_CARD_1", columnList = "NUMBER", unique = true),
                @Index(name = "UK_PAYMENT_CARD_2", columnList = "USER_ACCOUNT_ID", unique = true)
        }
)
public class PaymentCard extends BaseEntity {
    @Id
    @SequenceGenerator(name = "payment_card_seq", sequenceName = "PAYMENT_CARD_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_card_seq")
    @Column(name = "PAYMENT_CARD_ID", nullable = false)
    private Long id;

    @Column(name = "NUMBER", nullable = false, unique = true, length = 16)
    private String number;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "MONTH", nullable = false)
    private int month;

    @Column(name = "YEAR", nullable = false)
    private int year;

    @Column(name = "CVV", nullable = false)
    private int cvv;

    @OneToOne
    @JoinColumn(name = "USER_ACCOUNT_ID", unique = true)
    private User user;

    public PaymentCard() {
    }

    public PaymentCard(Long id, String number, String name, int month, int year, int cvv, User user) {
        this.id = id;
        this.number = number;
        this.name = name;
        this.month = month;
        this.year = year;
        this.cvv = cvv;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public int getCvv() {
        return cvv;
    }
}
