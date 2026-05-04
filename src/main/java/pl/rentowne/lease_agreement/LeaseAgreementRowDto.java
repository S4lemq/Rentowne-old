package pl.rentowne.lease_agreement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record LeaseAgreementRowDto(
        Long tenantId,
        String fullName,
        String apartment,
        String rentedObject,
        BigDecimal compensationAmount, //kwota odstępnego
        BigDecimal rentAmount, //kwota czynszu
        BigDecimal internetFee, //kwota za internet
        BigDecimal gasDeposit, //opłata za gaz
        LocalDateTime startContractDate, //od kiedy najem
        LocalDateTime endContractDate, //do kiedy najem
        BigDecimal deposit, //kaucja
        BigDecimal depositPaid, //wpłacona kaucja
        BigDecimal returnedDepositAmount, //zwrócona kaucja
        boolean isContractActive, //czy umowa jest aktywna
        Long leaseAgreementId
) {}
