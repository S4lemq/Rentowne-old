package pl.rentowne.tenant_settlement;

import java.util.List;

public interface PaymentService {
    /**
     * Pobiera wszystkie typy płatności
     * @return lista {@ Payment}
     */
    List<Payment> getPayments();
}
