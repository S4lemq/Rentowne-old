package pl.rentowne.rented_object.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.rented_object.model.RentedObject;
import pl.rentowne.rented_object.repository.custom.RentedObjectRepositoryCustom;

import java.util.List;

/**
 * Repozytorium wynajętych obiektów
 */
public interface RentedObjectRepository extends JpaRepository<RentedObject, Long>, RentedObjectRepositoryCustom {

    @Query("SELECT ro FROM RentedObject ro LEFT JOIN FETCH ro.meters WHERE ro.apartment.id = :apartmentId")
    List<RentedObject> findByApartmentIdWithMeters(@Param("apartmentId") Long apartmentId);

    @Modifying
    @Query("UPDATE RentedObject ro SET ro.lastSettlementDate = NULL, ro.lastSettlementTotalAmount = NULL WHERE ro.id = (SELECT s.rentedObject.id FROM Settlement s WHERE s.id = :settlementId)")
    void clearLastSettlementData(@Param("settlementId") Long settlementId);

    @Query("select ro.apartment.id from RentedObject ro where ro.id = :rentedObjectId")
    Long findApartmentIdByRentedObjectId(@Param("rentedObjectId") Long rentedObjectId);

    @Query("select ro.id from RentedObject ro where ro.apartment.id = :apartmentId")
    List<Long> findRentedObjectIdsByApartmentId(@Param("apartmentId") Long apartmentId);

    @Query("SELECT CONCAT(COALESCE(t.firstname, ''), '_', COALESCE(t.lastname, ''), '_', ro.rentedObjectName) " +
           "FROM RentedObject ro LEFT JOIN ro.tenant t WHERE ro.id = :rentedObjectId")
    String getSettlementFileNameData(@Param("rentedObjectId") Long rentedObjectId);

    /**
     * Pobiera obiekty wynajmowane z powiązanymi encjami dla danego użytkownika
     */
    @Query("""
            select distinct ro
            from RentedObject ro
            left join fetch ro.tenant t
            left join fetch ro.apartment a
            where a.user.id = :userId
            and (:apartmentId is null or a.id = :apartmentId)
            order by a.apartmentName, ro.rentedObjectName
            """)
    List<RentedObject> findRentedObjectsWithDetailsForUser(@Param("userId") Long userId, @Param("apartmentId") Long apartmentId);

}
