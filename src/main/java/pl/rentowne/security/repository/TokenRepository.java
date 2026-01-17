package pl.rentowne.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.security.model.Token;

import java.util.List;
import java.util.Optional;

/**
 * Repozytorium tokenów
 */
public interface TokenRepository extends JpaRepository<Token, Long> {

    /**
     * Zwraca wszystkie działające tokeny danego użytkownika
     *
     * @param userId id użytkownika
     * @return lista tokenów {@link Token}
     */
    @Query("""
        SELECT t FROM Token t
        JOIN t.user u
        WHERE u.id = :userId
        AND (t.expired = false OR t.revoked = false)
        """)
    List<Token> findAllValidTokensByUser(@Param("userId") Long userId);

    /**
     * Zwraca token
     *
     * @param tokenValue wartość tokena
     * @return token {@link Token}
     */
    Optional<Token> findByTokenValue(String tokenValue);

    /**
     * Znajduje wszystkie tokeny jwt które wygasły i są nie ważne
     * @return lista id tokenów
     */
    @Query("""
        SELECT t.id FROM Token t
        WHERE t.expired = true
        AND t.revoked = true
        AND t.tokenType = pl.rentowne.security.model.TokenType.BEARER
        """)
    List<Long> findAllExpiredAndRevokedJwtToken();

    /**
     * Usuwa wszystkie tokeny po id
     * @param tokenIds lista id
     */
    @Modifying
    @Query("DELETE FROM Token t WHERE t.id IN :tokenIds")
    void deleteAllByIds(@Param("tokenIds") List<Long> tokenIds);
}
