package pl.rentowne.lease_agreement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repozytorium umów najmu
 */
public interface LeaseAgreementRepository extends JpaRepository<LeaseAgreement, Long> {

    /**
     * Pobiera dane umowy do rozliczeń
     * @param rentedObjectId id obiektu, który jest przypisany do umowy
     * @return dane umowy
     */
    @Query("""
            SELECT new pl.rentowne.lease_agreement.LeaseAgreementDto(
                la.rentAmount, la.compensationAmount, la.internetFee, la.gasDeposit
            )
            FROM LeaseAgreement la
            JOIN Tenant t ON la.tenant.id = t.id
            WHERE t.rentedObject.id = :rentedObjectId
            AND t.active = true
            """)
    LeaseAgreementDto getData(@Param("rentedObjectId") Long rentedObjectId);

    /**
     * Pobiera listę umów najmu do tabeli (Page)
     * @param pageable informacje o stronicowaniu
     * @param userId id użytkownika
     * @param isContractActive filtr: czy umowa aktywna
     * @param searchText filtr: tekst wyszukiwania
     * @return strona z listą umów najmu
     */
    @Query("""
            SELECT new pl.rentowne.lease_agreement.LeaseAgreementRowDto(
                t.id,
                CONCAT(t.firstname, ' ', t.lastname),
                a.apartmentName,
                ro.rentedObjectName,
                la.compensationAmount,
                la.rentAmount,
                la.internetFee,
                la.gasDeposit,
                la.startContractDate,
                la.endContractDate,
                la.deposit,
                la.depositPaid,
                la.returnedDepositAmount,
                la.isContractActive,
                la.id
            )
            FROM LeaseAgreement la
            LEFT JOIN Tenant t ON la.id = t.leaseAgreement.id
            LEFT JOIN RentedObject ro ON t.rentedObject.id = ro.id
            LEFT JOIN Apartment a ON ro.apartment.id = a.id
            WHERE a.user.id = :userId
            AND (:isContractActive IS NULL OR la.isContractActive = :isContractActive)
            AND (:searchText IS NULL OR :searchText = ''
                OR LOWER(t.firstname) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(t.lastname) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(a.apartmentName) LIKE LOWER(CONCAT('%', :searchText, '%')))
            ORDER BY a.apartmentName ASC, ro.rentedObjectName ASC
            """)
    Page<LeaseAgreementRowDto> getLeaseAgreementsList(
            Pageable pageable,
            @Param("userId") Long userId,
            @Param("isContractActive") Boolean isContractActive,
            @Param("searchText") String searchText
    );

    /**
     * Zwraca wiersz po id
     *
     * @param id id
     * @return encja
     * @throws IllegalArgumentException gdy nie znaleziono
     */
    default LeaseAgreement findByIdMandatory(Long id) throws IllegalArgumentException {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Entity not found with id " + id));
    }
}
