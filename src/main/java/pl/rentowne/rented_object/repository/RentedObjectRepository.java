package pl.rentowne.rented_object.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.rented_object.model.RentedObject;
import pl.rentowne.rented_object.model.dto.RentedObjectDto;
import pl.rentowne.rented_object.model.dto.RentedObjectSettlementRowDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repozytorium wynajętych obiektów
 */
public interface RentedObjectRepository extends JpaRepository<RentedObject, Long> {

    @Query("SELECT ro FROM RentedObject ro LEFT JOIN FETCH ro.meters WHERE ro.apartment.id = :apartmentId")
    List<RentedObject> findByApartmentIdWithMeters(@Param("apartmentId") Long apartmentId);

    @Modifying
    @Query("UPDATE RentedObject ro SET ro.lastSettlementDate = NULL, ro.lastSettlementTotalAmount = NULL WHERE ro.id = (SELECT s.rentedObject.id FROM Settlement s WHERE s.id = :settlementId)")
    void clearLastSettlementData(@Param("settlementId") Long settlementId);

    @Query("SELECT ro.apartment.id FROM RentedObject ro WHERE ro.id = :rentedObjectId")
    Long findApartmentIdByRentedObjectId(@Param("rentedObjectId") Long rentedObjectId);

    @Query("SELECT ro.id FROM RentedObject ro WHERE ro.apartment.id = :apartmentId")
    List<Long> findRentedObjectIdsByApartmentId(@Param("apartmentId") Long apartmentId);

    List<RentedObject> findByApartmentIdAndIsRented(Long apartmentId, boolean isRented);

    @Query("SELECT CONCAT(COALESCE(s.tenant.firstname, ''), '_', COALESCE(s.tenant.lastname, ''), '_', s.rentedObject.rentedObjectName) " +
           "FROM Settlement s WHERE s.id = (SELECT MAX(s2.id) FROM Settlement s2 WHERE s2.rentedObject.id = :rentedObjectId)")
    String getSettlementFileNameData(@Param("rentedObjectId") Long rentedObjectId);

    /**
     * Pobiera wszystkie obiekty do wynajęcia po id mieszkania
     * @param apartmentId id mieszkania
     * @return lista encji obiektów do wynajęcia
     */
    @Query("SELECT ro FROM RentedObject ro WHERE ro.apartment.id = :apartmentId")
    List<RentedObject> getAllByApartmentId(@Param("apartmentId") Long apartmentId);

    /**
     * Usuwa wszystkie wynajęte obiekty po id mieszkania
     * @param apartmentId id mieszkania
     */
    @Modifying
    @Query("DELETE FROM RentedObject ro WHERE ro.apartment.id = :apartmentId")
    void deleteAllByApartmentId(@Param("apartmentId") Long apartmentId);

    /**
     * Pobiera wszystkie obiekty do wynajęcia danego użytkownika
     * @param userId id użytkownika
     * @return lista obiektów do wynajęcia
     */
    @Query("""
            SELECT new pl.rentowne.rented_object.model.dto.RentedObjectDto(ro.id, ro.rentedObjectName)
            FROM RentedObject ro
            JOIN ro.apartment a
            JOIN a.user u
            WHERE u.id = :userId
            """)
    List<RentedObjectDto> getAllRentedObjectDtos(@Param("userId") Long userId);

    /**
     * Aktualizuje flagę czy wynajęte
     * @param rentedObjectId id obiektu do wynajęcia
     * @param flag flaga
     */
    @Modifying
    @Query("UPDATE RentedObject ro SET ro.isRented = :flag WHERE ro.id = :rentedObjectId")
    void updateRentedFlag(@Param("rentedObjectId") Long rentedObjectId, @Param("flag") boolean flag);

    /**
     * Pobiera wszystkie obiekty do wynajęcia po id mieszkania i opcjonalnie pobiera jeszcze dany obiekt po drugim parametrze
     * @param apartmentId id mieszkania
     * @param rentedObjectId id obiektu opcjonalnego
     * @return lista obiektów do wynajęcia
     */
    @Query("""
            SELECT new pl.rentowne.rented_object.model.dto.RentedObjectDto(ro.id, ro.rentedObjectName)
            FROM RentedObject ro
            WHERE (ro.apartment.id = :apartmentId AND ro.isRented = false)
            OR (:rentedObjectId IS NOT NULL AND ro.id = :rentedObjectId)
            """)
    List<RentedObjectDto> getAllByApartmentAndOptionalRentedObject(@Param("apartmentId") Long apartmentId, @Param("rentedObjectId") Long rentedObjectId);

    /**
     * Aktualizuje w obiekcie ostatnią datę rozliczenia oraz kwotę całkowitą do zapłaty przez najemcę
     *
     * @param rentedObjectId id obiektu do wynajęcia
     * @param totalSum       kwota całkowita do zapłaty przez najemcę
     * @param settlementDate data rozliczenia
     */
    @Modifying
    @Query("UPDATE RentedObject ro SET ro.lastSettlementDate = :settlementDate, ro.lastSettlementTotalAmount = :totalSum WHERE ro.id = :rentedObjectId")
    void updateSettlement(@Param("rentedObjectId") Long rentedObjectId, @Param("totalSum") BigDecimal totalSum, @Param("settlementDate") LocalDateTime settlementDate);

    /**
     * Pobiera listę rozliczeń obiektów do wynajęcia do tabeli (Page)
     * @param pageable informacje o stronicowaniu
     * @param userId id użytkownika
     * @param searchText filtr: tekst wyszukiwania
     * @return strona z listą rozliczeń
     */
    @Query("""
            SELECT new pl.rentowne.rented_object.model.dto.RentedObjectSettlementRowDto(
                ro.id, a.apartmentName, ro.rentedObjectName,
                t.firstname, t.lastname,
                ro.lastSettlementDate, ro.lastSettlementTotalAmount,
                s.compensationAmount
            )
            FROM Apartment a
            JOIN RentedObject ro ON a.id = ro.apartment.id
            LEFT JOIN Tenant t ON ro.id = t.rentedObject.id AND t.active = true
            LEFT JOIN Settlement s ON ro.id = s.rentedObject.id AND s.settlementDate = (
                SELECT MAX(s2.settlementDate)
                FROM Settlement s2
                WHERE s2.rentedObject.id = ro.id
            )
            WHERE a.user.id = :userId
            AND (:searchText IS NULL OR :searchText = ''
                OR LOWER(a.apartmentName) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(ro.rentedObjectName) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(t.firstname) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(t.lastname) LIKE LOWER(CONCAT('%', :searchText, '%')))
            ORDER BY a.apartmentName ASC, ro.rentedObjectName ASC
            """)
    Page<RentedObjectSettlementRowDto> getRentedObjectSettlementsList(
            Pageable pageable,
            @Param("userId") Long userId,
            @Param("searchText") String searchText
    );

    /**
     * Zwraca wiersz po id
     *
     * @param id id
     * @return encja
     * @throws IllegalArgumentException gdy nie znaleziono
     */
    default RentedObject findByIdMandatory(Long id) throws IllegalArgumentException {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Entity not found with id " + id));
    }
}
