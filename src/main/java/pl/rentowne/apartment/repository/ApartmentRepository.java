package pl.rentowne.apartment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.apartment.model.Apartment;
import pl.rentowne.apartment.model.dto.ApartmentBasicDto;
import pl.rentowne.apartment.model.dto.ApartmentRowDto;
import pl.rentowne.rented_object.model.RentedObject;

import java.util.List;
import java.util.Optional;

/**
 * Repozytorium mieszkań
 */
public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    @Query("SELECT apartment.address.id FROM Apartment apartment WHERE apartment.id = :id")
    Long getAddressId(Long id);

    /**
     * Pobiera informacje o mieszkaniach dla podanych ID
     */
    @Query("""
            SELECT a FROM Apartment a
            WHERE a.id IN :apartmentIds
            ORDER BY a.apartmentName
            """)
    List<Apartment> findByIdIn(@Param("apartmentIds") List<Long> apartmentIds);

    /**
     * Pobiera wszystkie mieszkania zalogowanego użytkownika
     */
    @Query("""
            SELECT a FROM Apartment a
            WHERE a.user.id = :userId
            ORDER BY a.apartmentName
            """)
    List<Apartment> findAllByUserId(@Param("userId") Long userId);

    /**
     * Pobiera wszystkie mieszkania zalogowanego użytkownika wraz z adresami
     */
    @Query("""
            SELECT a FROM Apartment a
            LEFT JOIN FETCH a.address
            WHERE a.user.id = :userId
            ORDER BY a.apartmentName
            """)
    List<Apartment> findAllWithAddressByUserId(@Param("userId") Long userId);

    /**
     * Pobiera podstawowe informacje o wszystkich mieszkaniach zalogowanego użytkownika
     */
    @Query("""
            SELECT new pl.rentowne.apartment.model.dto.ApartmentBasicDto(a.id, a.apartmentName)
            FROM Apartment a
            WHERE a.user.id = :userId
            ORDER BY a.apartmentName
            """)
    List<ApartmentBasicDto> findAllBasicByUserId(@Param("userId") Long userId);

    /**
     * Pobiera mieszkanie po id wraz z adresem
     *
     * @param id id mieszkania
     * @return {@link Apartment} encja mieszkania
     */
    @Query("""
            SELECT a FROM Apartment a
            JOIN FETCH a.address
            WHERE a.id = :id
            """)
    Optional<Apartment> findApartmentById(@Param("id") Long id);

    /**
     * Pobiera id mieszkania po id obiektu do wynajęcia
     * @param rentedObjectId id obiektu do wynajęcia
     * @return id mieszkania
     */
    @Query("""
            SELECT a.id FROM Apartment a
            JOIN RentedObject ro ON ro.apartment.id = a.id
            WHERE ro.id = :rentedObjectId
            """)
    Long getApartmentIdByRentedObject(@Param("rentedObjectId") Long rentedObjectId);

    /**
     * Pobiera podstawowe informacje o RentedObject dla danego Apartment
     * bez ładowania tenant'ów aby uniknąć problemów z duplikatami
     * @param apartmentId id mieszkania
     * @return lista obiektów do wynajęcia
     */
    @Query("""
            SELECT ro FROM RentedObject ro
            WHERE ro.apartment.id = :apartmentId
            """)
    List<RentedObject> findRentedObjectsByApartmentId(@Param("apartmentId") Long apartmentId);

    /**
     * Pobiera obiekty do wynajęcia danego mieszkania po fladze wynajęcia obiektów
     * @param apartmentId id mieszkania
     * @param isRented flaga wynajęcia obiektu
     * @return lista id obiektów do wynajęcia
     */
    @Query("""
            SELECT ro.id FROM RentedObject ro
            WHERE ro.isRented = :isRented
            AND ro.apartment.id = :apartmentId
            """)
    List<Long> getAllRentedObjectsByApartmentIdAndRentedFlag(@Param("apartmentId") Long apartmentId, @Param("isRented") boolean isRented);

    /**
     * Aktualizuje flagę czy wynajęte
     * @param apartmentId id mieszkania
     * @param flag flaga
     */
    @Modifying
    @Query("UPDATE Apartment a SET a.isRented = :flag WHERE a.id = :apartmentId")
    void updateRentedFlag(@Param("apartmentId") Long apartmentId, @Param("flag") boolean flag);

    /**
     * Pobiera wszystkie mieszkania zalogowanego użytkownika które nie są wynajęte
     * oraz opcjonalnie dodaje do wyników jeszcze przekazany parametr apartmentId
     * @param loggedUserId id zalogowanego użytkownika
     * @param apartmentId id mieszkania (opcjonalne)
     * @return lista mieszkań
     */
    @Query("""
            SELECT new pl.rentowne.apartment.model.dto.ApartmentBasicDto(a.id, a.apartmentName)
            FROM Apartment a
            WHERE (a.user.id = :loggedUserId AND a.isRented = false)
            OR (:apartmentId IS NOT NULL AND a.id = :apartmentId)
            """)
    List<ApartmentBasicDto> getAllApartmentsByLoggedUserAndApartment(@Param("loggedUserId") Long loggedUserId, @Param("apartmentId") Long apartmentId);

    /**
     * Pobiera liczbę obiektów mieszkania po id obiektu
     * @param rentedObjectId id obiektu
     * @return lista id obiektów
     */
    @Query("""
            SELECT ro.id FROM RentedObject ro
            WHERE ro.apartment.id = (
                SELECT ro2.apartment.id FROM RentedObject ro2 WHERE ro2.id = :rentedObjectId
            )
            """)
    List<Long> getRentedObjectsCountByRentedObjectId(@Param("rentedObjectId") Long rentedObjectId);

    /**
     * Służy do przypinania nieruchomości
     * @param isPinned czy przypięto
     * @param id id mieszkania
     */
    @Modifying
    @Query("UPDATE Apartment a SET a.pinned = :isPinned WHERE a.id = :id")
    void pinApartment(@Param("isPinned") boolean isPinned, @Param("id") Long id);

    /**
     * Pobiera mieszkanie wraz z jego dostawcami
     * @param apartmentId id mieszkania
     * @return {@link Apartment}
     */
    @Query("""
            SELECT a FROM Apartment a
            LEFT JOIN FETCH a.housingProviders
            WHERE a.id = :apartmentId
            """)
    Apartment getApartmentWithHousingProviders(@Param("apartmentId") Long apartmentId);

    /**
     * Pobiera listę mieszkań do tabeli (Page)
     * @param pageable informacje o stronicowaniu
     * @param userId id użytkownika
     * @param apartmentName filtr: nazwa mieszkania
     * @param isRented filtr: czy wynajęte
     * @param searchText filtr: tekst wyszukiwania
     * @return strona z listą mieszkań
     */
    @Query("""
            SELECT new pl.rentowne.apartment.model.dto.ApartmentRowDto(
                a.id, a.apartmentName, a.leasesNumber, a.isRented,
                addr.cityName, addr.streetName, addr.buildingNumber, addr.apartmentNumber,
                a.image, a.pinned
            )
            FROM Apartment a
            LEFT JOIN a.address addr
            WHERE a.user.id = :userId
            AND (:apartmentName IS NULL OR LOWER(a.apartmentName) LIKE LOWER(CONCAT('%', CAST(:apartmentName AS string), '%')))
            AND (:isRented IS NULL OR a.isRented = :isRented)
            AND (:searchText IS NULL OR CAST(:searchText AS string) = ''
                OR LOWER(a.apartmentName) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%'))
                OR LOWER(addr.cityName) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%')))
            ORDER BY a.pinned DESC, a.id ASC
            """)
    Page<ApartmentRowDto> getApartmentsList(
            Pageable pageable,
            @Param("userId") Long userId,
            @Param("apartmentName") String apartmentName,
            @Param("isRented") Boolean isRented,
            @Param("searchText") String searchText
    );

    /**
     * Zwraca wiersz po id
     *
     * @param id id
     * @return encja
     * @throws IllegalArgumentException gdy nie znaleziono
     */
    default Apartment findByIdMandatory(Long id) throws IllegalArgumentException {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Entity not found with id " + id));
    }
}
