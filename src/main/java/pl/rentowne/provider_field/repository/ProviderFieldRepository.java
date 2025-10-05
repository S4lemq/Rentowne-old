package pl.rentowne.provider_field.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.rentowne.provider_field.model.ProviderField;
import pl.rentowne.provider_field.repository.custom.ProviderFieldRepositoryCustom;

import java.util.List;

/**
 * Repozytorium pól dostawców świadczeń
 */
public interface ProviderFieldRepository extends JpaRepository<ProviderField, Long>, ProviderFieldRepositoryCustom {

    @Query("select pf from ProviderField pf where pf.housingProvider.id = :providerId")
    List<ProviderField> findByHousingProviderId(@Param("providerId") Long providerId);
}
