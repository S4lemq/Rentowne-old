package pl.rentowne.settlement;

import org.springframework.stereotype.Component;
import pl.rentowne.apartment.Apartment;
import pl.rentowne.lease_agreement.LeaseAgreement;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.tenant.Tenant;
import pl.rentowne.util.DateUtils;

import java.math.BigDecimal;

/**
 * Mapper dla Settlement
 */
@Component
class SettlementMapper {

    /**
     * Sumuje wartości BigDecimal pomijając null
     */
    private static BigDecimal sumNonNull(BigDecimal... values) {
        BigDecimal sum = BigDecimal.ZERO;
        if (values == null) return sum;
        for (BigDecimal v : values) {
            if (v != null) {
                sum = sum.add(v);
            }
        }
        return sum;
    }

    /**
     * Konwertuje encję Settlement na DTO listy
     *
     * @param settlement encja Settlement
     * @return DTO dla listy
     */
    public SettlementListDto toListDto(Settlement settlement) {
        if (settlement == null) {
            return null;
        }

        // Oblicz sumę czynszu i mediów
        BigDecimal rentAndUtilities = sumNonNull(
                settlement.getRentAmount(),
                settlement.getInternetFee(),
                settlement.getGasDeposit(),
                settlement.getElectricityAmount(),
                settlement.getWaterAmount()
        );

        return new SettlementListDto(
                settlement.getId(),
                DateUtils.formatPolishDate(settlement.getSettlementDate()),
                settlement.getCompensationAmount(),
                settlement.getRentAmount(),
                settlement.getInternetFee(),
                settlement.getGasDeposit(),
                settlement.getElectricityAmount(),
                settlement.getDistributedElectricityAmount(),
                settlement.getWaterAmount(),
                settlement.getGasAmount(),
                settlement.getTotalAmount(),
                rentAndUtilities,
                settlement.getProrationDescription(),
                settlement.getDepositTopUp(),
                settlement.getAdditionalCharge(),
                settlement.getAdditionalChargeDescription(),
                settlement.getAdditionalCharges()
        );
    }

    public SettlementExportRowDto toExportRowDto(Settlement settlement) {
        if (settlement == null) {
            return null;
        }

        Tenant tenant = settlement.getTenant();
        RentedObject rentedObject = settlement.getRentedObject();
        Apartment apartment = rentedObject != null ? rentedObject.getApartment() : null;
        LeaseAgreement leaseAgreement = tenant != null ? tenant.getLeaseAgreement() : null;

        String tenantFullName = tenant != null
                ? (safe(tenant.getFirstname()) + " " + safe(tenant.getLastname())).trim()
                : null;
        String contractStatus = leaseAgreement == null
                ? null
                : leaseAgreement.isContractActive() ? "aktywna" : "nieaktywna";

        BigDecimal rentAndUtilities = sumNonNull(
                settlement.getRentAmount(),
                settlement.getInternetFee(),
                settlement.getGasDeposit(),
                settlement.getElectricityAmount(),
                settlement.getWaterAmount()
        );

        return new SettlementExportRowDto(
                emptyToNull(tenantFullName),
                apartment != null ? apartment.getApartmentName() : null,
                rentedObject != null ? rentedObject.getRentedObjectName() : null,
                contractStatus,
                settlement.getId(),
                settlement.getInsertDate(),
                settlement.getSettlementDate(),
                settlement.getCompensationAmount(),
                rentAndUtilities,
                settlement.getRentAmount(),
                settlement.getInternetFee(),
                settlement.getGasDeposit(),
                settlement.getElectricityAmount(),
                settlement.getDistributedElectricityAmount(),
                settlement.getWaterAmount(),
                settlement.getTotalAmount(),
                settlement.getProrationDescription(),
                settlement.getDepositTopUp(),
                settlement.getAdditionalCharge(),
                settlement.getAdditionalChargeDescription(),
                settlement.getAdditionalCharges()
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
