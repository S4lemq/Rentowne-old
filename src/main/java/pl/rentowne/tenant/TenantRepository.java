package pl.rentowne.tenant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repozytorium najemców
 */
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /**
     * Pobiera najemców z powiązanymi encjami dla danego użytkownika
     */
    @Query("""
            SELECT t
            FROM Tenant t
            JOIN FETCH t.rentedObject ro
            JOIN FETCH ro.apartment a
            WHERE a.user.id = :userId
            AND (:apartmentId IS NULL OR a.id = :apartmentId)
            AND t.active = :active
            ORDER BY a.apartmentName, ro.rentedObjectName, t.id
            """)
    List<Tenant> findTenantsWithDetailsForUser(@Param("userId") Long userId, @Param("apartmentId") Long apartmentId, @Param("active") Boolean active);

    /**
     * Pobiera Optional'a encji najemcy z pełnymi danymi
     * @param id id najemcy
     * @return Optional encji najemcy
     */
    @Query("""
            SELECT t FROM Tenant t
            LEFT JOIN FETCH t.address
            JOIN FETCH t.leaseAgreement
            JOIN FETCH t.rentedObject ro
            JOIN FETCH ro.apartment
            WHERE t.id = :id
            """)
    Optional<Tenant> findByTenantId(@Param("id") Long id);

    /**
     * Pobiera id obiektu do wynajęcia po id najemcy
     * @param id id najemcy
     * @return id obiektu do wynajęcia
     */
    @Query("SELECT t.rentedObject.id FROM Tenant t WHERE t.id = :id")
    Long getRentedObjectId(@Param("id") Long id);

    /**
     * Pobiera najemcę po emailu
     * @param email email najemcy
     * @return {@link Tenant}
     */
    @Query("SELECT t FROM Tenant t WHERE t.email = :email AND t.active = true")
    Tenant getTenantByEmail(@Param("email") String email);

    /**
     * Pobiera id obiektu gdzie jest najemca
     * @param email email najemcy
     * @return id obiektu do wynajęcia
     */
    @Query("SELECT t.rentedObject.id FROM Tenant t WHERE t.email = :email AND t.active = true")
    Long getRentedObjectIdByTenantEmail(@Param("email") String email);

    /**
     * Pobiera najemcę po id obiektu do wynajęcia
     * @param rentedObjectId id obiektu do wynajęcia
     * @return {@link Tenant}
     */
    @Query("SELECT t FROM Tenant t WHERE t.rentedObject.id = :rentedObjectId AND t.active = true")
    Tenant getTenantByRentedObjectId(@Param("rentedObjectId") Long rentedObjectId);

    /**
     * Pobiera listę najemców do tabeli (Page)
     * @param pageable informacje o stronicowaniu
     * @param userId id użytkownika
     * @param searchText filtr: tekst wyszukiwania
     * @return strona z listą najemców
     */
    @Query("""
            SELECT new pl.rentowne.tenant.TenantRowDto(
                t.id, t.firstname, t.lastname, a.apartmentName, ro.rentedObjectName,
                t.phoneNumber, t.email, la.endContractDate
            )
            FROM Tenant t
            JOIN t.rentedObject ro
            JOIN ro.apartment a
            JOIN t.leaseAgreement la
            WHERE a.user.id = :userId
            AND (:searchText IS NULL OR :searchText = ''
                OR LOWER(t.firstname) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(t.lastname) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(t.email) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(a.apartmentName) LIKE LOWER(CONCAT('%', :searchText, '%')))
            """)
    Page<TenantRowDto> getTenantsList(
            Pageable pageable,
            @Param("userId") Long userId,
            @Param("searchText") String searchText
    );

    /**
     * Pobiera listę rozliczeń najemcy do tabeli (Page)
     * @param pageable informacje o stronicowaniu
     * @param rentedObjectId id obiektu do wynajęcia
     * @return strona z listą rozliczeń
     */
    @Query("""
            SELECT new pl.rentowne.tenant.TenantSettlementRowDto(
                s.id, s.settlementDate, s.totalAmount, s.settlementStatus
            )
            FROM Settlement s
            WHERE s.rentedObject.id = :rentedObjectId
            ORDER BY s.id DESC
            """)
    Page<TenantSettlementRowDto> getTenantSettlementsList(
            Pageable pageable,
            @Param("rentedObjectId") Long rentedObjectId
    );

    /**
     * Zwraca wiersz po id
     *
     * @param id id
     * @return encja
     * @throws IllegalArgumentException gdy nie znaleziono
     */
    default Tenant findByIdMandatory(Long id) throws IllegalArgumentException {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Entity not found with id " + id));
    }

    /**
     * Pobiera najemcę po numerze CIF
     *
     * @param cif numer CIF najemcy
     * @return Optional encji najemcy
     */
    Optional<Tenant> findByCif(String cif);
}
