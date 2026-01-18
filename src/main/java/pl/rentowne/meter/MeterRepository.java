package pl.rentowne.meter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.meter_reading.model.dto.MeterReadingDto;

import java.util.List;

/**
 * Repozytorium liczników
 */
public interface MeterRepository extends JpaRepository<Meter, Long> {

    @Query("""
            SELECT m FROM Meter m
            LEFT JOIN FETCH m.apartment a
            LEFT JOIN FETCH m.rentedObject ro
            WHERE m.id = :id
            """)
    Meter getMeterDtoById(@Param("id") Long id);

    @Query("""
            SELECT a.apartmentName
            FROM RentedObject ro
            JOIN ro.apartment a
            WHERE ro.id = :rentedObjectId
            """)
    String getApartmentNameByRentedObjectId(@Param("rentedObjectId") Long rentedObjectId);

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
            """)
    Page<MeterListDto> getMetersList(Pageable pageable,
                                     @Param("userAccountId") Long userAccountId,
                                     @Param("apartmentId") Long apartmentId,
                                     @Param("meterType") MeterType meterType,
                                     @Param("searchText") String searchText);

    @Query("""
            SELECT m
            FROM Meter m
            WHERE m.apartment.id = :apartmentId
            AND m.meterType = :meterType
            AND m.distributeReadings = true
            """)
    List<Meter> findDistributedMetersByApartmentAndType(@Param("apartmentId") Long apartmentId,
                                                         @Param("meterType") MeterType meterType);

    /**
     * Pobiera stany i id licznika, które nie zostały rozliczone dla danego obiektu do wynajęcia i typu licznika
     * @param rentedObjectId id obiektu
     * @param meterType typ licznika
     * @return stan i id licznika
     */
    @Query("""
            SELECT new pl.rentowne.meter_reading.model.dto.MeterReadingDto(mr.id, mr.consumption)
            FROM Meter m
            JOIN MeterReading mr ON mr.meter.id = m.id
            WHERE m.rentedObject.id = :rentedObjectId
            AND m.meterType = :meterType
            AND mr.settled = false
            """)
    List<MeterReadingDto> getConsumptionAndIdByRentedObjectAndMeterType(
            @Param("rentedObjectId") Long rentedObjectId,
            @Param("meterType") MeterType meterType
    );

    /**
     * Pobiera id liczników danego obiektu do wynajęcia
     * @param rentedObjectId obiektu do wynajęcia
     * @return lista id liczników
     */
    @Query("""
            SELECT m.id
            FROM Meter m
            WHERE m.rentedObject.id = :rentedObjectId
            AND (m.meterType = pl.rentowne.meter.model.MeterType.ELECTRIC
                OR m.meterType = pl.rentowne.meter.model.MeterType.WATER_COLD)
            """)
    List<Long> findMeterCountByRentedObject(@Param("rentedObjectId") Long rentedObjectId);

    /**
     * Zwraca wiersz po id
     *
     * @param id id
     * @return encja
     * @throws IllegalArgumentException gdy nie znaleziono
     */
    default Meter findByIdMandatory(Long id) throws IllegalArgumentException {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Entity not found with id " + id));
    }
}
