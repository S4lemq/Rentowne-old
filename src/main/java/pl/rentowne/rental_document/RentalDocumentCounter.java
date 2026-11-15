package pl.rentowne.rental_document;

import jakarta.persistence.*;

import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "rental_document_counter")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RentalDocumentCounter {
    @EmbeddedId private Key id;

    @Column(name = "last_number")
    private int lastNumber;

    RentalDocumentCounter(Key id) {
        this.id = id;
    }

    int next() {
        return ++lastNumber;
    }

    @Embeddable
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    static class Key implements Serializable {
        @Column(name = "owner_id")
        private Long ownerId;

        @Enumerated(EnumType.STRING)
        @Column(name = "document_type")
        private RentalDocumentType type;

        @Column(name = "document_year")
        private int year;
    }
}
