package pl.rentowne.rental_document;

import jakarta.persistence.criteria.*;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

record DocumentFilter(
        Long userId,
        String tab,
        String type,
        String search,
        Long apartmentId,
        String mailState,
        LocalDate from,
        LocalDate to,
        Long settlementId,
        Long leaseId,
        Long tenantId) {
    Specification<RentalDocument> specification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            if (!"ALL".equals(tab))
                predicates.add(cb.equal(root.get("cancelled"), "CANCELLED".equals(tab)));
            if (present(type))
                predicates.add(cb.equal(root.get("type"), RentalDocumentType.valueOf(type)));
            if (present(search)) {
                String pattern = "%" + search.strip().toLowerCase(Locale.ROOT) + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("number")), pattern),
                                cb.like(cb.lower(root.get("tenantName")), pattern),
                                cb.like(cb.lower(root.get("propertyAddress")), pattern)));
            }
            equal(predicates, cb, root, "apartmentId", apartmentId);
            equal(predicates, cb, root, "settlementId", settlementId);
            equal(predicates, cb, root, "leaseId", leaseId);
            equal(predicates, cb, root, "tenantId", tenantId);
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("issueDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("issueDate"), to));
            if (present(mailState)) {
                var delivery = query.subquery(UUID.class);
                var current = delivery.from(DocumentDelivery.class);
                List<Predicate> criteria = new ArrayList<>();
                criteria.add(cb.equal(current.get("documentId"), root.get("id")));
                criteria.add(cb.equal(current.get("kind"), DocumentDeliveryKind.ISSUE));
                if (!mailState.equals("NOT_SENT")) {
                    criteria.add(
                            cb.equal(
                                    current.get("state"),
                                    DocumentDeliveryState.valueOf(mailState)));
                    var newer = delivery.subquery(UUID.class);
                    var candidate = newer.from(DocumentDelivery.class);
                    newer.select(candidate.get("id"))
                            .where(
                                    cb.equal(
                                            candidate.get("documentId"), current.get("documentId")),
                                    cb.equal(candidate.get("kind"), DocumentDeliveryKind.ISSUE),
                                    cb.or(
                                            cb.greaterThan(
                                                    candidate.<OffsetDateTime>get("createdAt"),
                                                    current.get("createdAt")),
                                            cb.and(
                                                    cb.equal(
                                                            candidate.get("createdAt"),
                                                            current.get("createdAt")),
                                                    cb.greaterThan(
                                                            candidate.<UUID>get("id"),
                                                            current.get("id")))));
                    criteria.add(cb.not(cb.exists(newer)));
                }
                delivery.select(current.get("id")).where(criteria.toArray(Predicate[]::new));
                predicates.add(
                        mailState.equals("NOT_SENT")
                                ? cb.not(cb.exists(delivery))
                                : cb.exists(delivery));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static void equal(
            List<Predicate> predicates,
            CriteriaBuilder cb,
            Root<RentalDocument> root,
            String field,
            Long value) {
        if (value != null) predicates.add(cb.equal(root.get(field), value));
    }
}
