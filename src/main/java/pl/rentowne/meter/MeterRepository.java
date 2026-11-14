package pl.rentowne.meter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeterRepository extends JpaRepository<Meter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Meter m where m.id = :id")
    Optional<Meter> lockById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"apartment", "rentedObject"})
    Optional<Meter> findWithApartmentAndRentedObjectById(Long id);

    List<Meter> findAllByApartmentIdOrderByIdAsc(Long apartmentId);

    List<Meter> findAllByRentedObjectId(Long rentedObjectId);

    @Query("""
            SELECT new pl.rentowne.meter.MeterListDto(
                m.id,
                COALESCE(a.apartmentName, ma.apartmentName),
                COALESCE(ro.rentedObjectName, 'Licznik mieszkania'),
                m.meterType,
                m.meterNumber,
                m.distributeReadings
            )
            FROM Meter m
            LEFT JOIN m.rentedObject ro
            LEFT JOIN ro.apartment a
            LEFT JOIN m.apartment ma
            WHERE (
                (m.rentedObject IS NOT NULL AND a.user.id = :userAccountId) OR
                (m.rentedObject IS NULL AND ma.user.id = :userAccountId)
            )
            AND (:apartmentId IS NULL OR
                (m.rentedObject IS NOT NULL AND a.id = :apartmentId) OR
                (m.rentedObject IS NULL AND ma.id = :apartmentId))
            AND (:meterType IS NULL OR m.meterType = :meterType)
            AND (:searchText IS NULL OR :searchText = ''
                OR LOWER(COALESCE(a.apartmentName, ma.apartmentName)) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(ro.rentedObjectName) LIKE LOWER(CONCAT('%', :searchText, '%')))
            ORDER BY ma.apartmentName, ro.rentedObjectName
            """)
    Page<MeterListDto> getMetersList(Pageable pageable,
                                     @Param("userAccountId") Long userAccountId,
                                     @Param("apartmentId") Long apartmentId,
                                     @Param("meterType") MeterType meterType,
                                     @Param("searchText") String searchText);
}
