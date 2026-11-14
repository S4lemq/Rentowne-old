package pl.rentowne.rental_payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rented-objects/{rentedObjectId}/tenants/{tenantId}/payments")
@RequiredArgsConstructor
class RentalPaymentController {
    private final RentalPaymentService service;

    @GetMapping
    RentalPaymentService.RentalPaymentHistoryDto get(@PathVariable Long tenantId, @PathVariable Long rentedObjectId) {
        return service.getHistory(tenantId, rentedObjectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RentalPaymentDto create(@PathVariable Long tenantId, @PathVariable Long rentedObjectId,
                            @Valid @RequestBody CreateRentalPaymentRequest request) {
        return service.create(tenantId, rentedObjectId, request);
    }

    @PutMapping("/{id}")
    RentalPaymentDto update(@PathVariable Long tenantId, @PathVariable Long rentedObjectId, @PathVariable Long id,
                            @Valid @RequestBody UpdateRentalPaymentRequest request) {
        return service.update(tenantId, rentedObjectId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long tenantId, @PathVariable Long rentedObjectId, @PathVariable Long id,
                @RequestParam Long version) {
        service.delete(tenantId, rentedObjectId, id, version);
    }

    @ExceptionHandler(RentalPaymentException.class)
    ResponseEntity<PaymentError> handle(RentalPaymentException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new PaymentError(exception.getMessage()));
    }

    record PaymentError(String message) {}
}
