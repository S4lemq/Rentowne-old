package pl.rentowne.meter.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.meter.model.Meter;
import pl.rentowne.meter.model.dto.MeterListDto;
import pl.rentowne.meter.repository.cutom.MeterRepositoryCustom;

import java.util.List;

/**
 * Repozytorium liczników
 */
public interface MeterRepository extends JpaRepository<Meter, Long>, MeterRepositoryCustom {

    @Query("SELECT meter FROM Meter meter WHERE meter.id = :id")
    Meter getMeterDtoById(@Param("id") Long id);

    @Query("""
            SELECT m
            FROM Meter m
            WHERE m.apartment.id = :apartmentId
            ORDER BY m.id ASC
            """)
    List<Meter> findByApartmentId(@Param("apartmentId") Long apartmentId);

    @Query("""
            SELECT new pl.rentowne.meter.model.dto.MeterListDto(
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
            WHERE (a.user.id = :userAccountId OR ma.user.id = :userAccountId)
            AND (:apartmentId IS NULL OR a.id = :apartmentId OR ma.id = :apartmentId)
            AND (:searchText IS NULL OR :searchText = ''
                OR LOWER(COALESCE(a.apartmentName, ma.apartmentName)) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(ro.rentedObjectName) LIKE LOWER(CONCAT('%', :searchText, '%')))
            """)
    Page<MeterListDto> getMetersList(Pageable pageable,
                                     @Param("userAccountId") Long userAccountId,
                                     @Param("apartmentId") Long apartmentId,
                                     @Param("searchText") String searchText);

    @Query("""
            SELECT m
            FROM Meter m
            WHERE m.apartment.id = :apartmentId
            AND m.meterType = :meterType
            AND m.distributeReadings = true
            """)
    List<Meter> findDistributedMetersByApartmentAndType(@Param("apartmentId") Long apartmentId,
                                                         @Param("meterType") pl.rentowne.meter.model.MeterType meterType);

}
