package pl.rentowne.settlement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.settlement.model.Settlement;
import pl.rentowne.settlement.repository.custom.SettlementRepositoryCustom;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repozytorium odczytów stanu licznika
 */
public interface SettlementRepository extends JpaRepository<Settlement, Long>, SettlementRepositoryCustom {

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
        """,
            countQuery = """
            select count(s) from Settlement s
        where s.rentedObject.id = :rentedObjectId
    """
    )
    Page<Settlement> findAllByRentedObjectId(Pageable pageable, @Param("rentedObjectId") Long rentedObjectId);

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
     * Pobiera najnowsze rozliczenie dla danego obiektu wynajmowanego
     */
    @Query("""
            select s
            from Settlement s
            where s.rentedObject.id = :rentedObjectId
            order by s.settlementDate desc
            limit 1
            """)
    Settlement findLatestByRentedObjectId(@Param("rentedObjectId") Long rentedObjectId);
}
