package pl.rentowne.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import pl.rentowne.common.model.BaseEntity;
import pl.rentowne.user.User;

import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "TOKEN",
        uniqueConstraints = {
                @UniqueConstraint(name = "PK_TOKEN", columnNames = "TOKEN_ID"),
                @UniqueConstraint(name = "UK_TOKEN_1", columnNames = "TOKEN_VALUE")
        },
        indexes = {
                @Index(name = "PK_TOKEN", columnList = "TOKEN_ID", unique = true),
                @Index(name = "UK_TOKEN_1", columnList = "TOKEN_VALUE", unique = true)
        }
)
public class Token extends BaseEntity {

    @Id
    @SequenceGenerator(name = "token_seq", sequenceName = "TOKEN_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "token_seq")
    @Column(name = "TOKEN_ID", nullable = false)
    private Long id;

    @Column(name = "TOKEN_VALUE", nullable = false, unique = true)
    private String tokenValue;

    @Column(name = "TOKEN_TYPE", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    @Column(name = "EXPIRED", nullable = false)
    private boolean expired;

    @Column(name = "REVOKED", nullable = false)
    private boolean revoked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ACCOUNT_ID", nullable = false)
    private User user;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return Objects.equals(tokenValue, token.tokenValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenValue);
    }
}
