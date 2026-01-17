package pl.rentowne.rented_object;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface RentedObjectRepository extends JpaRepository<RentedObject, Long> {

    @EntityGraph(attributePaths = "meters")
    List<RentedObject> findByApartmentId(Long apartmentId);

    @Modifying
    @Query("""
    UPDATE RentedObject ro
    SET ro.lastSettlementDate = NULL,
        ro.lastSettlementTotalAmount = NULL
    WHERE ro.id = (
        SELECT s.rentedObject.id
        FROM Settlement s
        WHERE s.id = :settlementId
    )
    """)
    void clearLastSettlementData(@Param("settlementId") Long settlementId);


    @Query("SELECT ro.apartment.id FROM RentedObject ro WHERE ro.id = :rentedObjectId")
    Long findApartmentIdByRentedObjectId(@Param("rentedObjectId") Long rentedObjectId);

    @Query("SELECT ro.id FROM RentedObject ro WHERE ro.apartment.id = :apartmentId")
    List<Long> findRentedObjectIdsByApartmentId(@Param("apartmentId") Long apartmentId);

    List<RentedObject> findByApartmentIdAndIsRented(Long apartmentId, boolean isRented);

    List<RentedObject> findAllByApartmentId(Long apartmentId);

    @Query("""
            SELECT ro FROM RentedObject ro
            JOIN ro.apartment a
            JOIN a.user u
            WHERE u.id = :userId
            """)
    List<RentedObject> findAllByLoggedUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE RentedObject ro SET ro.isRented = :flag WHERE ro.id = :rentedObjectId")
    void updateRentedFlag(@Param("rentedObjectId") Long rentedObjectId, @Param("flag") boolean flag);

    @Query("""
            SELECT ro FROM RentedObject ro
            WHERE (ro.apartment.id = :apartmentId AND ro.isRented = false)
            OR (:rentedObjectId IS NOT NULL AND ro.id = :rentedObjectId)
            """)
    List<RentedObject> getAllByApartmentAndOptionalRentedObject(@Param("apartmentId") Long apartmentId, @Param("rentedObjectId") Long rentedObjectId);

    @Modifying
    @Query("UPDATE RentedObject ro SET ro.lastSettlementDate = :settlementDate, ro.lastSettlementTotalAmount = :totalSum WHERE ro.id = :rentedObjectId")
    void updateSettlement(@Param("rentedObjectId") Long rentedObjectId, @Param("totalSum") BigDecimal totalSum, @Param("settlementDate") LocalDateTime settlementDate);

    @Query("""
            SELECT new pl.rentowne.rented_object.RentedObjectSettlementRowDto(
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
}
