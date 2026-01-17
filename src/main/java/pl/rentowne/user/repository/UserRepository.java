package pl.rentowne.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.user.model.User;
import pl.rentowne.user.model.dto.UserBasicDto;
import pl.rentowne.user.model.dto.UserProfileDto;

import java.util.Optional;

/**
 * Repozytorium użytkowników
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Zwraca encję użytkownika po jego adresie email
     *
     * @param email adres email
     * @return encja użytkownika {@link User}
     */
    Optional<User> findByEmail(String email);

    /**
     * Sprawdza czy istnieje użytkownik z podanym mailem
     * @param email - email użytkownika
     * @return true - istnieje / false - nie istnieje
     */
    boolean existsByEmail(String email);

    /**
     * Pobiera podstawowe dane użytkownika
     * @param email email użytkownika
     * @return {@link UserBasicDto}
     */
    @Query("SELECT new pl.rentowne.user.model.dto.UserBasicDto(u.id, u.email, u.role) FROM User u WHERE u.email = :email")
    UserBasicDto getByEmail(@Param("email") String email);

    /**
     * Pobiera użytkownika po hashu z linka resetu hasła
     * @param hash hash resetu hasła
     * @return dane użytkownika
     */
    Optional<User> findByHash(String hash);

    /**
     * Pobiera pełne dane użytkownika po adresie email
     * @param email adres email
     * @return dane użytkownika
     */
    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.address
        LEFT JOIN FETCH u.paymentCard
        WHERE u.email = :email
        """)
    User getFullUserDataByEmail(@Param("email") String email);

    /**
     * Pobiera zdjęcie oraz preferowany język użytkownika
     * @param email email użytkownika
     * @return zdjęcie oraz preferowany język
     */
    @Query("SELECT new pl.rentowne.user.model.dto.UserProfileDto(u.preferredLanguage, u.image) FROM User u WHERE u.email = :email")
    UserProfileDto getUserProfileImageAndLang(@Param("email") String email);

    /**
     * Zwraca wiersz po id
     *
     * @param id id
     * @return encja
     * @throws IllegalArgumentException gdy nie znaleziono
     */
    default User findByIdMandatory(Long id) throws IllegalArgumentException {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Entity not found with id " + id));
    }
}
