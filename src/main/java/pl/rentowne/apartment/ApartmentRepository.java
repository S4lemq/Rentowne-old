package pl.rentowne.apartment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    List<Apartment> findByIdInOrderByApartmentName(List<Long> apartmentIds);

    List<Apartment> findByUser_IdOrderByApartmentName(Long userId);

    @Query("""
            SELECT a.id FROM Apartment a
            JOIN RentedObject ro ON ro.apartment.id = a.id
            WHERE ro.id = :rentedObjectId
            """)
    Long getApartmentIdByRentedObject(@Param("rentedObjectId") Long rentedObjectId);

    @Modifying
    @Query("UPDATE Apartment a SET a.isRented = :flag WHERE a.id = :apartmentId")
    void updateRentedFlag(@Param("apartmentId") Long apartmentId, @Param("flag") boolean flag);

    @Query("""
            SELECT new pl.rentowne.apartment.ApartmentDto(
                a.id, a.apartmentName, null, false,
                null, null, false,
                null, null, null, null,
                null, null
            )
            FROM Apartment a
            WHERE (a.user.id = :loggedUserId AND a.isRented = false)
            OR (:apartmentId IS NOT NULL AND a.id = :apartmentId)
            """)
    List<ApartmentDto> getAllApartmentsByLoggedUserAndApartment(@Param("loggedUserId") Long loggedUserId, @Param("apartmentId") Long apartmentId);

    @Modifying
    @Query("UPDATE Apartment a SET a.pinned = :isPinned WHERE a.id = :id")
    void pinApartment(@Param("isPinned") boolean isPinned, @Param("id") Long id);

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
            SELECT new pl.rentowne.apartment.ApartmentDto(
                a.id, a.apartmentName, a.leasesNumber, a.isRented,
                null, a.image, a.pinned,
                addr.cityName, addr.streetName, addr.buildingNumber, addr.apartmentNumber,
                null, null
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
    Page<ApartmentDto> getApartmentsList(
            Pageable pageable,
            @Param("userId") Long userId,
            @Param("apartmentName") String apartmentName,
            @Param("isRented") Boolean isRented,
            @Param("searchText") String searchText
    );

}
