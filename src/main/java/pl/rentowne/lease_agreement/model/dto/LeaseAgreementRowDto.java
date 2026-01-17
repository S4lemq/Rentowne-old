package pl.rentowne.lease_agreement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LeaseAgreementRowDto {
    private Long tenantId;
    private String fullName;
    private String apartment;
    private String rentedObject;
    private BigDecimal compensationAmount; //kwota odstępnego
    private BigDecimal rentAmount; //kwota czynszu
    private BigDecimal internetFee; //kwota za internet
    private BigDecimal gasDeposit; //opłata za gaz
    private LocalDateTime startContractDate; //od kiedy najem
    private LocalDateTime endContractDate; //do kiedy najem
    private BigDecimal deposit; //kaucja
    private BigDecimal depositPaid; //wpłacona kaucja
    private BigDecimal returnedDepositAmount; //zwrócona kaucja
    private boolean isContractActive; //czy umowa jest aktywna
    private Long leaseAgreementId;
}
