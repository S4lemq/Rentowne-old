package pl.rentowne.tenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.tenant.model.Tenant;
import pl.rentowne.tenant.repository.custom.TenantRepositoryCustom;

import java.util.List;

/**
 * Repozytorium najemców
 */
public interface TenantRepository extends JpaRepository<Tenant, Long>, TenantRepositoryCustom {

    /**
     * Pobiera najemców z powiązanymi encjami dla danego użytkownika
     */
    @Query("""
            select t
            from Tenant t
            join fetch t.rentedObject ro
            join fetch ro.apartment a
            where a.user.id = :userId
            and (:apartmentId is null or a.id = :apartmentId)
            and t.active = :active
            order by a.apartmentName, ro.rentedObjectName, t.id
            """)
    List<Tenant> findTenantsWithDetailsForUser(@Param("userId") Long userId, @Param("apartmentId") Long apartmentId, @Param("active") Boolean active);
}
