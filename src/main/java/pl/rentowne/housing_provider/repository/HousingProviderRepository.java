package pl.rentowne.housing_provider.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.housing_provider.model.HousingProvider;
import pl.rentowne.housing_provider.model.dto.HousingProviderApartmentDto;
import pl.rentowne.housing_provider.model.dto.HousingProviderListDto;
import pl.rentowne.housing_provider.model.dto.HousingProviderWithAssignmentDto;
import pl.rentowne.housing_provider.repository.custom.HousingProviderRepositoryCustom;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repozytorium dostawców świadczeń
 */
public interface HousingProviderRepository extends JpaRepository<HousingProvider, Long>, HousingProviderRepositoryCustom {

    @Query("SELECT hp FROM HousingProvider hp LEFT JOIN FETCH hp.providerFields WHERE hp.id = :id")
    HousingProvider getHousingById(@Param("id")Long id);

    @Query("SELECT DISTINCT hp FROM HousingProvider hp " +
            "LEFT JOIN FETCH hp.providerFields pf " +
            "JOIN hp.apartments a " +
            "JOIN a.rentedObjects ro " +
            "WHERE ro.id = :rentedObjectId")
    List<HousingProvider> findHousingProviderWithProviderFieldsByRentedObjectId(@Param("rentedObjectId") Long rentedObjectId);

    @EntityGraph(attributePaths = {"providerFields"})
    Optional<HousingProvider> findById(Long id);

    @Query("SELECT hp FROM HousingProvider hp WHERE hp.user.id = :userId")
    List<HousingProvider> findByUserId(@Param("userId") Long userId);

    @Query("""
            select distinct hp from HousingProvider hp
            left join fetch hp.providerFields
            where hp.recurringPayment = true
            """)
    Set<HousingProvider> findProvidersWithRecurringPaymentAndProviderFields();

    @Query("""
            select distinct hp from HousingProvider hp
            left join fetch hp.apartments
            where hp in :providers
            """)
    Set<HousingProvider> fetchApartmentsForProviders(@Param("providers") Set<HousingProvider> providers);

    @Query("""
            SELECT new pl.rentowne.housing_provider.model.dto.HousingProviderListDto(
                hp.id,
                hp.name,
                hp.type,
                hp.tax,
                a.apartmentName
            )
            FROM HousingProvider hp
            JOIN hp.apartments a
            WHERE hp.user.id = :userAccountId
            AND (:apartmentId IS NULL OR a.id = :apartmentId)
            AND (:searchText IS NULL OR :searchText = ''
                OR LOWER(hp.name) LIKE LOWER(CONCAT('%', :searchText, '%'))
                OR LOWER(a.apartmentName) LIKE LOWER(CONCAT('%', :searchText, '%')))
            """)
    Page<HousingProviderListDto> getHousingProvidersList(Pageable pageable,
                                                         @Param("userAccountId") Long userAccountId,
                                                         @Param("apartmentId") Long apartmentId,
                                                         @Param("searchText") String searchText);

    /**
     * Pobiera dostawców świadczeń przypisanych do konkretnego mieszkania
     * @param pageable obiekt paginacji
     * @param userAccountId id użytkownika
     * @param apartmentId id mieszkania
     * @return paginowana lista dostawców dla mieszkania
     */
    @Query("""
            SELECT new pl.rentowne.housing_provider.model.dto.HousingProviderApartmentDto(
                hp.id,
                hp.name,
                hp.type,
                hp.tax
            )
            FROM HousingProvider hp
            JOIN hp.apartments a
            WHERE hp.user.id = :userAccountId
            AND a.id = :apartmentId
            """)
    Page<HousingProviderApartmentDto> getHousingProvidersForApartment(Pageable pageable,
                                                                       @Param("userAccountId") Long userAccountId,
                                                                       @Param("apartmentId") Long apartmentId);

    /**
     * Pobiera wszystkich dostawców użytkownika z informacją czy są przypisani do mieszkania
     * @param pageable obiekt paginacji
     * @param userAccountId id użytkownika
     * @param apartmentId id mieszkania
     * @return paginowana lista dostawców z flagą przypisania
     */
    @Query("""
            SELECT new pl.rentowne.housing_provider.model.dto.HousingProviderWithAssignmentDto(
                hp.id,
                hp.name,
                hp.type,
                hp.tax,
                CASE WHEN EXISTS (SELECT 1 FROM hp.apartments a WHERE a.id = :apartmentId) THEN true ELSE false END
            )
            FROM HousingProvider hp
            WHERE hp.user.id = :userAccountId
            """)
    Page<HousingProviderWithAssignmentDto> getAllProvidersWithAssignmentStatus(Pageable pageable,
                                                                                @Param("userAccountId") Long userAccountId,
                                                                                @Param("apartmentId") Long apartmentId);
}
