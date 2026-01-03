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

    @Query("SELECT CONCAT(COALESCE(s.tenant.firstname, ''), '_', COALESCE(s.tenant.lastname, ''), '_', s.rentedObject.rentedObjectName) " +
           "FROM Settlement s WHERE s.id = (SELECT MAX(s2.id) FROM Settlement s2 WHERE s2.rentedObject.id = :rentedObjectId)")
    String getSettlementFileNameData(@Param("rentedObjectId") Long rentedObjectId);

    /**
     * Pobiera obiekty wynajmowane z powiązanymi encjami dla danego użytkownika
     */
    @Query("""
            select distinct ro
            from RentedObject ro
            join fetch ro.tenant t
            join t.leaseAgreement ls
            join fetch ro.apartment a
            where a.user.id = :userId
            and (:apartmentId is null or a.id = :apartmentId)
            and ls.isContractActive = :active
            order by a.apartmentName, ro.rentedObjectName
            """)
    List<RentedObject> findRentedObjectsWithDetailsForUser(@Param("userId") Long userId, @Param("apartmentId") Long apartmentId, @Param("active") Boolean active);

}
