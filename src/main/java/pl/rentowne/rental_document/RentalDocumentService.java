package pl.rentowne.rental_document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.address.Address;
import pl.rentowne.lease_agreement.LeaseAgreement;
import pl.rentowne.owner.Owner;
import pl.rentowne.owner.OwnerMapper;
import pl.rentowne.owner.PaymentAccountHolder;
import pl.rentowne.rental_payment.RentalPaymentException;
import pl.rentowne.settlement.CalculateRequestDto;
import pl.rentowne.settlement.Settlement;
import pl.rentowne.settlement.SettlementChargeDto;
import pl.rentowne.tenant.Tenant;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalDocumentService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DocumentProperties properties;
    private final DocumentStore store;
    private final ObjectMapper json;
    private final Clock documentClock;
    private final Validator validator;

    @Transactional
    public void issueReceipt(Settlement settlement, CalculateRequestDto request) {
        if (!properties.enabled() || store.exists("SETTLEMENT:" + settlement.getId())) return;
        Tenant tenant = settlement.getTenant();
        Owner owner = requireParties(tenant);
        var lease = tenant.getLeaseAgreement();
        if (lease.getPaymentDueDayOfMonth() < 1 || lease.getPaymentDueDayOfMonth() > 31)
            throw bad("Uzupełnij poprawny dzień płatności w umowie najemcy.");
        YearMonth month = YearMonth.from(settlement.getSettlementDate());
        List<RentalDocumentData.Line> lines = new ArrayList<>();
        add(lines, "Czynsz najmu - odstępne", settlement.getCompensationAmount());
        add(lines, "Czynsz administracyjny", settlement.getRentAmount());
        add(lines, "Internet", settlement.getInternetFee());
        if (settlement.getGasDeposit() != null) add(lines, "Zaliczka na gaz", settlement.getGasAmount());
        add(lines, "Energia elektryczna", settlement.getElectricityAmount());
        add(lines, "Woda", settlement.getWaterAmount());
        var additional = settlement.getAdditionalCharges();
        for (var charge : additional) {
            String label = charge.type() == SettlementChargeDto.Type.DEPOSIT ? "Uzupełnienie kaucji" : "Dodatkowa opłata";
            if (charge.description() != null && !charge.description().isBlank()
                    && !(charge.type() == SettlementChargeDto.Type.DEPOSIT && charge.description().equalsIgnoreCase("Kaucja"))) {
                label += " - " + charge.description().strip();
            }
            add(lines, label, charge.amount());
        }
        if (additional.isEmpty()) {
            if (positive(settlement.getDepositTopUp())) add(lines, "Uzupełnienie kaucji", settlement.getDepositTopUp());
            if (positive(settlement.getAdditionalCharge())) add(lines, "Dodatkowa opłata" +
                    (settlement.getAdditionalChargeDescription() == null ? "" : " - " + settlement.getAdditionalChargeDescription()), settlement.getAdditionalCharge());
        }
        BigDecimal total = sum(lines);
        if (total.compareTo(DocumentMoney.amount(settlement.getTotalAmount())) != 0)
            throw bad("Suma pozycji rachunku nie odpowiada kwocie rozliczenia. Sprawdź składniki rozliczenia.");
        String period = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pl-PL")).format(month);
        period = period.substring(0, 1).toUpperCase(Locale.forLanguageTag("pl-PL")) + period.substring(1);
        String periodText = Boolean.TRUE.equals(request.isPartialMonth())
                ? DATE.format(request.periodStartDate().toLocalDate()) + " - " + DATE.format(request.periodEndDate().toLocalDate()) : period;
        LocalDate due = month.atDay(Math.min(lease.getPaymentDueDayOfMonth(), month.lengthOfMonth()));
        var data = base(tenant, owner, RentalDocumentType.RENT_RECEIPT).period(periodText).lines(lines).total(total)
                .note(settlement.getProrationDescription()).build();
        persist(data, owner, tenant, settlement.getId(), "SETTLEMENT:" + settlement.getId(), true,
                (number) -> payment(owner, due, "Rachunek " + number + " - " + data.propertyAddress() + " - " + periodText));
    }

    @Transactional
    public void issueDeposit(LeaseAgreement lease, Settlement finalSettlement) {
        if (!properties.enabled() || store.exists("LEASE:" + lease.getId())) return;
        Tenant tenant = lease.getTenant();
        Owner owner = requireParties(tenant);
        List<RentalDocumentData.Line> lines = new ArrayList<>();
        add(lines, "Potrącenie za wodę", finalSettlement.getWaterAmount());
        add(lines, "Potrącenie za energię elektryczną", finalSettlement.getElectricityAmount());
        if (positive(lease.getGasChargeAmount())) add(lines, "Końcowe obciążenie za gaz", lease.getGasChargeAmount());
        if (positive(lease.getChargeAmount())) add(lines, "Inne obciążenie - " + lease.getChargeDescription().strip(), lease.getChargeAmount());
        BigDecimal result = DocumentMoney.amount(lease.getDepositPaid()).subtract(sum(lines));
        if (result.signum() > 0 && !validAccount(tenant.getAccountNumber()))
            throw bad("Uzupełnij poprawny numer konta najemcy do zwrotu kaucji.");
        var data = base(tenant, owner, RentalDocumentType.DEPOSIT_SETTLEMENT).terminationDate(lease.getActualTerminationDate())
                .depositPaid(DocumentMoney.amount(lease.getDepositPaid())).lines(lines).total(sum(lines)).result(result).build();
        persist(data, owner, tenant, finalSettlement.getId(), "LEASE:" + lease.getId(), properties.depositMailEnabled(), number -> {
            if (result.signum() == 0) return null;
            if (result.signum() > 0) return new RentalDocumentData.Payment(fullName(tenant), tenant.getAccountNumber(),
                    lease.getDepositRefundDueDate(), "Zwrot kaucji - rozliczenie " + number + " - " + data.propertyAddress(), false);
            return payment(owner, null, "Dopłata - rozliczenie kaucji " + number + " - " + data.propertyAddress());
        });
    }

    @Transactional
    public void cancelReceipt(Long settlementId) {
        // Cancellation must also work after document generation has been disabled in configuration.
        store.cancelReceipt(settlementId);
    }

    private RentalDocumentData.RentalDocumentDataBuilder base(Tenant tenant, Owner owner, RentalDocumentType type) {
        var object = tenant.getRentedObject();
        return RentalDocumentData.builder().type(type).issueDate(LocalDate.now(documentClock)).issueCity(properties.issueCity())
                .owner(new RentalDocumentData.Party(owner.getFirstName() + " " + owner.getLastName(), ownerAddress(owner)))
                .tenant(new RentalDocumentData.Party(fullName(tenant), address(tenant.getAddress())))
                .representativeName(owner.getRepresentativeName())
                .propertyAddress(String.join(", ", address(object.getApartment().getAddress())) + ", obiekt " + object.getRentedObjectName())
                .contractSigningDate(tenant.getLeaseAgreement().getContractSigningDate().toLocalDate());
    }

    private void persist(RentalDocumentData draft, Owner owner, Tenant tenant, Long settlementId, String source, boolean enabled,
                         java.util.function.Function<String, RentalDocumentData.Payment> payment) {
        int next = store.nextNumber(owner.getId(), draft.type(), draft.issueDate().getYear());
        String number = String.format(Locale.ROOT, "%02d/%d", next, draft.issueDate().getYear());
        RentalDocumentData data = draft.toBuilder().number(number).payment(payment.apply(number)).build();
        UUID id = UUID.randomUUID();
        store.insert(id, owner.getUser().getId(), owner.getId(), tenant.getId(), settlementId,
                tenant.getLeaseAgreement().getId(), source, data, next, encode(data));
        LinkedHashMap<String, String> recipients = new LinkedHashMap<>();
        String to = tenant.getEmail().strip();
        for (String email : new String[]{owner.getEmail(), owner.getRepresentativeEmail()}) {
            if (email != null && !email.strip().equalsIgnoreCase(to)) recipients.putIfAbsent(email.strip().toLowerCase(Locale.ROOT), email.strip());
        }
        store.enqueue(id, "ISSUE", to, encode(List.copyOf(recipients.values())), enabled);
    }

    private Owner requireParties(Tenant tenant) {
        Owner owner = tenant.getRentedObject().getApartment().getOwner();
        List<String> missing = new ArrayList<>();
        if (owner == null) missing.add("Przypisz właściciela w edycji nieruchomości.");
        else {
            validator.validate(OwnerMapper.toDto(owner)).forEach(v -> missing.add(v.getMessage()));
            if (!validEmail(owner.getEmail())) missing.add("Uzupełnij poprawny email właściciela.");
            if (owner.getRepresentativeName() != null && !validEmail(owner.getRepresentativeEmail())) missing.add("Uzupełnij email pełnomocnika.");
        }
        if (!present(tenant.getFirstname()) || !present(tenant.getLastname())) missing.add("Uzupełnij imię i nazwisko najemcy.");
        if (!present(tenant.getRentedObject().getRentedObjectName())) missing.add("Uzupełnij oznaczenie obiektu do wynajęcia.");
        if (!validEmail(tenant.getEmail())) missing.add("Uzupełnij poprawny email najemcy.");
        if (!complete(tenant.getAddress())) missing.add("Uzupełnij adres najemcy.");
        if (!complete(tenant.getRentedObject().getApartment().getAddress())) missing.add("Uzupełnij adres nieruchomości.");
        if (tenant.getLeaseAgreement().getContractSigningDate() == null) missing.add("Uzupełnij datę podpisania umowy.");
        if (!missing.isEmpty()) throw bad(String.join(" ", missing));
        return owner;
    }

    private static RentalDocumentData.Payment payment(Owner owner, LocalDate due, String title) {
        boolean representative = owner.getPaymentAccountHolder() == PaymentAccountHolder.REPRESENTATIVE;
        return new RentalDocumentData.Payment(representative ? owner.getRepresentativeName() : owner.getFirstName() + " " + owner.getLastName(),
                owner.getAccountNumber(), due, title, representative);
    }

    private static List<String> ownerAddress(Owner owner) {
        return List.of(street(owner.getStreetName(), owner.getBuildingNumber(), owner.getApartmentNumber()), owner.getZipCode() + " " + owner.getCityName());
    }

    private static List<String> address(Address address) {
        return List.of(street(address.getStreetName(), address.getBuildingNumber(), address.getApartmentNumber()), address.getZipCode() + " " + address.getCityName());
    }

    private static String street(String name, String building, String flat) {
        return (name == null || name.isBlank() ? "" : "ul. " + name.strip() + " ") + building + (flat == null || flat.isBlank() ? "" : "/" + flat.strip());
    }

    private static boolean complete(Address address) {
        return address != null && present(address.getBuildingNumber()) && present(address.getZipCode()) && present(address.getCityName());
    }

    private static boolean present(String value) { return value != null && !value.isBlank(); }
    private static boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }
    private static String fullName(Tenant tenant) { return tenant.getFirstname() + " " + tenant.getLastname(); }
    private static void add(List<RentalDocumentData.Line> lines, String description, BigDecimal amount) {
        lines.add(new RentalDocumentData.Line(description, DocumentMoney.amount(amount)));
    }
    private static BigDecimal sum(List<RentalDocumentData.Line> lines) {
        return lines.stream().map(RentalDocumentData.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean validEmail(String value) {
        if (!present(value) || value.contains("\r") || value.contains("\n")) return false;
        try { new InternetAddress(value.strip(), true).validate(); return true; }
        catch (AddressException e) { return false; }
    }

    private static boolean validAccount(String value) {
        if (!present(value)) return false;
        String normalized = value.replaceAll("[\\s\\p{Z}]", "").replaceFirst("(?i)^PL", "");
        if (!normalized.matches("\\d{26}")) return false;
        return new java.math.BigInteger(normalized.substring(2) + "2521" + normalized.substring(0, 2))
                .mod(java.math.BigInteger.valueOf(97)).equals(java.math.BigInteger.ONE);
    }

    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Nie można zapisać danych dokumentu", e); }
    }

    private static RentalPaymentException bad(String message) { return new RentalPaymentException(HttpStatus.BAD_REQUEST, message); }
}
