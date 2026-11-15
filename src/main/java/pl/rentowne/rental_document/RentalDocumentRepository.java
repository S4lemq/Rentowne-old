package pl.rentowne.rental_document;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

interface RentalDocumentRepository
        extends JpaRepository<RentalDocument, UUID>, JpaSpecificationExecutor<RentalDocument> {
    boolean existsBySourceKey(String sourceKey);

    List<RentalDocument> findByUserIdAndSettlementIdIn(Long userId, Collection<Long> settlementIds);

    Optional<RentalDocument> findByIdAndUserId(UUID id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from RentalDocument d where d.id=:id")
    Optional<RentalDocument> lockById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from RentalDocument d where d.id=:id and d.userId=:userId")
    Optional<RentalDocument> lockOwned(@Param("id") UUID id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select d from RentalDocument d where d.settlementId=:settlementId and not d.cancelled"
                    + " order by d.id")
    List<RentalDocument> lockForCancellation(@Param("settlementId") Long settlementId);

    interface ApartmentProjection {Long getId();String getName();}

    @Query(
        """
            select distinct d.apartmentId as id, coalesce(a.apartmentName,'Usunięta nieruchomość') as name
            from RentalDocument d left join Apartment a on a.id=d.apartmentId
            where d.userId=:userId and d.apartmentId is not null order by name
        """)
    List<ApartmentProjection> apartments(@Param("userId") Long userId);
}
