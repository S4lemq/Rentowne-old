package pl.rentowne.apartment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.apartment.model.Apartment;
import pl.rentowne.apartment.repository.custom.ApartmentRepositoryCustom;

import java.util.List;

/**
 * Repozytorium mieszkań
 */
public interface ApartmentRepository extends JpaRepository<Apartment, Long>, ApartmentRepositoryCustom {

    @Query("SELECT apartment.address.id FROM Apartment apartment WHERE apartment.id = :id")
    Long getAddressId(Long id);

    /**
     * Pobiera informacje o mieszkaniach dla podanych ID
     */
    @Query("""
            select a from Apartment a
            where a.id in :apartmentIds
            order by a.apartmentName
            """)
    List<Apartment> findByIdIn(@Param("apartmentIds") List<Long> apartmentIds);
}
