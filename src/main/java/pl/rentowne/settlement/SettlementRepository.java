package pl.rentowne.settlement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.meter.MeterReadingData;
import pl.rentowne.meter.MeterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repozytorium odczytów stanu licznika
 */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findAllByRentedObjectApartmentUserIdOrderBySettlementDateDescIdDesc(Long userId);

    Optional<Settlement> findFirstByTenantIdAndRentedObjectIdAndDepositAssessmentTrueOrderByIdAsc(Long tenantId, Long rentedObjectId);

    @Query("select s.rentedObject.apartment.id from Settlement s where s.id = :settlementId")
    Long getApartmentIdBySettlementId(@Param("settlementId") Long settlementId);

    @Query("select s.settlementDate from Settlement s where s.id = :settlementId")
    LocalDateTime getSettlementDateById(@Param("settlementId") Long settlementId);

    @Query("""
           select s.id
             from Settlement s
            where s.rentedObject.apartment.id = :apartmentId
              and s.settlementDate = :settlementDate
           """)
    List<Long> findSettlementIdsByApartmentAndDate(@Param("apartmentId") Long apartmentId,
                                                   @Param("settlementDate") LocalDateTime settlementDate);
    @Query(
        value = """
            select s from Settlement s
            where s.rentedObject.id = :rentedObjectId
            and s.tenant.id = :tenantId
        """,
            countQuery = """
            select count(s) from Settlement s
            where s.rentedObject.id = :rentedObjectId
            and s.tenant.id = :tenantId
    """
    )
    Page<Settlement> findAllByRentedObjectIdAndTenantId(Pageable pageable,
                                                        @Param("rentedObjectId") Long rentedObjectId,
                                                        @Param("tenantId") Long tenantId);

    /**
     * Suma totalAmount z Settlement dla danego miesiąca według settlementDate
     */
    @Query("""
            select coalesce(sum(s.totalAmount), 0)
            from Settlement s
            where year(s.settlementDate) = :year
            and month(s.settlementDate) = :month
            """)
    BigDecimal sumTotalAmountByMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Suma compensationAmount z Settlement dla danego miesiąca według settlementDate
     */
    @Query("""
            select coalesce(sum(s.compensationAmount), 0)
            from Settlement s
            where year(s.settlementDate) = :year
            and month(s.settlementDate) = :month
            """)
    BigDecimal sumCompensationAmountByMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Pobiera listę unikalnych miesięcy z settlementDate
     */
    @Query("""
            select distinct function('DATE_TRUNC', 'month', s.settlementDate)
            from Settlement s
            order by function('DATE_TRUNC', 'month', s.settlementDate) asc
            """)
    List<LocalDateTime> findDistinctMonths();

    /**
     * Suma compensationAmount z Settlement dla danego mieszkania i miesiąca
     * Sumuje po wszystkich RentedObjects należących do danego Apartment
     */
    @Query("""
            select coalesce(sum(s.compensationAmount), 0)
            from Settlement s
            where s.rentedObject.apartment.id = :apartmentId
            and year(s.settlementDate) = :year
            and month(s.settlementDate) = :month
            """)
    BigDecimal sumCompensationAmountByApartmentAndMonth(
            @Param("apartmentId") Long apartmentId,
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * Suma totalAmount z Settlement dla danego mieszkania i miesiąca
     * Sumuje po wszystkich RentedObjects należących do danego Apartment
     */
    @Query("""
            select coalesce(sum(s.totalAmount), 0)
            from Settlement s
            where s.rentedObject.apartment.id = :apartmentId
            and year(s.settlementDate) = :year
            and month(s.settlementDate) = :month
            """)
    BigDecimal sumTotalAmountByApartmentAndMonth(
            @Param("apartmentId") Long apartmentId,
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * Pobiera wszystkie unikalne ID mieszkań, które mają rozliczenia w danym miesiącu
     */
    @Query("""
            select distinct s.rentedObject.apartment.id
            from Settlement s
            where year(s.settlementDate) = :year
            and month(s.settlementDate) = :month
            and s.rentedObject.apartment is not null
            """)
    List<Long> findDistinctApartmentIdsByMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Suma gasDeposit z Settlement dla danego RentedObject w zakresie dat (według settlementDate)
     */
    @Query("""
            select coalesce(sum(s.gasDeposit), 0)
            from Settlement s
            where s.rentedObject.id = :rentedObjectId
            and s.settlementDate >= :dateFrom
            and s.settlementDate <= :dateTo
            """)
    BigDecimal sumGasDepositByRentedObjectAndDateRange(
            @Param("rentedObjectId") Long rentedObjectId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo
    );

    /**
     * Suma gasDeposit z Settlement dla danego Apartment w zakresie dat (według settlementDate)
     * Sumuje po wszystkich RentedObjects należących do danego Apartment
     */
    @Query("""
            select coalesce(sum(s.gasDeposit), 0)
            from Settlement s
            where s.rentedObject.apartment.id = :apartmentId
            and s.settlementDate >= :dateFrom
            and s.settlementDate <= :dateTo
            """)
    BigDecimal sumGasDepositByApartmentAndDateRange(
            @Param("apartmentId") Long apartmentId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo
    );

    /**
     * Pobiera najnowsze rozliczenie dla danego obiektu wynajmowanego
     */
    @Query("""
            select s
            from Settlement s
            where s.rentedObject.id = :rentedObjectId
              and s.tenant.id = :tenantId
            order by s.settlementDate desc, s.id desc
            limit 1
            """)
    Settlement findLatestByRentedObjectId(@Param("rentedObjectId") Long rentedObjectId,
                                          @Param("tenantId") Long tenantId);

    /**
     * Sprawdza czy istnieją jakieś rozliczenia dla danego najemcy
     * @param tenantId id najemcy
     * @return true jeśli istnieje co najmniej jedno rozliczenie, false w przeciwnym razie
     */
    @Query("""
            select case when count(s) > 0 then true else false end
            from Settlement s
            where s.tenant.id = :tenantId
            """)
    boolean existsByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Pobiera id ostatniego rozliczenia
     * @param tenantId id najemcy
     * @return id ostatniego rozliczenia
     */
    @Query("""
            SELECT s.id
            FROM Settlement s
            JOIN RentedObject ro ON s.rentedObject.id = ro.id
            JOIN Tenant t ON ro.id = t.rentedObject.id
            WHERE t.id = :tenantId
            ORDER BY s.id DESC
            LIMIT 1
            """)
    Long getLastSettlementByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Aktualizuje status rozliczenia i hash
     *
     * @param settlementId     id rozliczenia
     * @param settlementStatus status
     * @param orderHash hash rozliczenia
     */
    @Modifying
    @Query("UPDATE Settlement s SET s.settlementStatus = :settlementStatus, s.hash = :orderHash WHERE s.id = :settlementId")
    void updateStatusAndHash(
            @Param("settlementId") Long settlementId,
            @Param("settlementStatus") SettlementStatus settlementStatus,
            @Param("orderHash") String orderHash
    );

    /**
     * Aktualizuje status rozliczenia
     * @param orderHash hash rozliczenia
     * @param settlementStatus status
     */
    @Modifying
    @Query("UPDATE Settlement s SET s.settlementStatus = :settlementStatus WHERE s.hash = :orderHash")
    void updateStatus(
            @Param("orderHash") String orderHash,
            @Param("settlementStatus") SettlementStatus settlementStatus
    );

    /**
     * Pobiera stany i id licznika, które nie zostały rozliczone dla danego obiektu do wynajęcia i typu licznika
     * @param rentedObjectId id obiektu
     * @param meterType typ licznika
     * @return lista danych odczytów liczników
     */
    @Query("""
            SELECT new pl.rentowne.meter.MeterReadingData(mr.id, mr.consumption)
            FROM Meter m
            JOIN MeterReading mr ON mr.meter.id = m.id
            WHERE m.rentedObject.id = :rentedObjectId
            AND m.meterType = :meterType
            AND mr.settled = false
            """)
    List<MeterReadingData> getConsumptionAndIdByRentedObjectAndMeterType(
            @Param("rentedObjectId") Long rentedObjectId,
            @Param("meterType") MeterType meterType
    );

    /**
     * Zwraca wiersz po id
     *
     * @param id id
     * @return encja
     * @throws IllegalArgumentException gdy nie znaleziono
     */
    default Settlement findByIdMandatory(Long id) throws IllegalArgumentException {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Entity not found with id " + id));
    }
}

