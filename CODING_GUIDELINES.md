# Coding Guidelines - Rentowne Backend

## Środowisko i technologie

- **Java**: 21
- **Spring Boot**: 3.5
- **Lombok**: aktywny

## Podstawowe zasady kodowania

### 1. Używanie Records
- Preferuj `record` zamiast klas dla immutable DTO
- Records są idealne dla data transfer objects, które nie wymagają logiki biznesowej

```java
// ✅ Dobrze
public record TenantDto(
    @NonNull Long id,
    @NonNull String name,
    @Nullable String email
) {}

// ❌ Źle
@Data
public class TenantDto {
    private Long id;
    private String name;
    private String email;
}
```

### 2. Hermetyzacja
- Klasy i metody powinny być package-private gdy to możliwe
- Unikaj `public` jeśli niepotrzebne
- Przy użyciu Lomboka również hermetyzuj adnotacje

```java
// ✅ Dobrze
@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class TenantServiceImpl implements TenantService {
    // ...
}

// ❌ Źle
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {
    // ...
}
```

### 3. Null Safety
- Używaj `@NonNull` i `@Nullable` z Lombok
- Wykorzystuj API `Optional`
- Metody najlepiej jak zwracają `Optional` gdy mogą być null - nie rzucany jest wyjątek

```java
// ✅ Dobrze
Optional<Tenant> findTenantById(@NonNull Long id);

// ❌ Źle
Tenant findTenantById(Long id); // może zwrócić null lub rzucić wyjątek
```

### 4. Funkcyjne podejście - Stream API i IntStream
- Wykorzystuj Stream API zamiast tradycyjnych pętli gdzie to sensowne
- Używaj `IntStream` zamiast tradycyjnej pętli `for`

```java
// ✅ Dobrze - funkcyjne podejście
return IntStream.iterate(16, maskEnd -> maskEnd >= 12, maskEnd -> maskEnd - 1)
    .mapToObj(maskEnd -> nrb.substring(2, maskEnd))
    .toList();

// ❌ Źle - tradycyjna pętla
List<String> masks = new ArrayList<>();
for (int maskEnd = 16; maskEnd >= 12; maskEnd--) {
    masks.add(nrb.substring(2, maskEnd));
}
return masks;
```

## Praktyki specyficzne dla projektu

### 5. Klasy Utility
- Używaj adnotacji `@Utility` dla klas pomocniczych

### 6. Lombok z umiarem
- Nie używaj `@Data` wszędzie
- Używaj konkretnych adnotacji: `@Getter`, `@Setter`, `@Builder`
- Pamiętaj o hermetyzacji adnotacji (np. `@RequiredArgsConstructor(access = AccessLevel.PROTECTED)`)

### 7. ConfigurationProperties
- Preferuj `@ConfigurationProperties` zamiast `@Value` w Spring

```java
// ✅ Dobrze
@ConfigurationProperties(prefix = "app.payment")
record PaymentProperties(
    String apiKey,
    String merchantId,
    int timeout
) {}

// ❌ Źle
@Value("${app.payment.api-key}")
private String apiKey;
```

## Uniwersalne praktyki Java/Spring

### 8. Java 21 Features
- Wykorzystuj pattern matching
- Używaj switch expressions
- Stosuj text blocks
- Rozważ virtual threads gdzie odpowiednie

```java
// ✅ Dobrze - pattern matching
return switch (status) {
    case ACTIVE -> processActive(tenant);
    case INACTIVE -> processInactive(tenant);
    case PENDING -> processPending(tenant);
};
```

### 9. Spring Boot 3.x
- Używaj najnowszych praktyk Spring Boot 3.x
- Wykorzystuj native hints dla GraalVM jeśli planowane

### 10. Immutability
- Preferuj immutable obiekty
- Używaj `final` dla pól
- Hermetyzuj klasy żeby były package-private

### 11. Exception Handling
- Używaj specific exceptions (np. `SantanderException`)
- Nie łap generic `Exception`
- Twórz hierarchię wyjątków specyficznych dla aplikacji

```java
// ✅ Dobrze
try {
    processPayment(tenantId);
} catch (PaymentProcessingException e) {
    log.error("Payment failed for tenant {}", tenantId, e);
    throw new SantanderException("Payment processing failed", e);
}

// ❌ Źle
try {
    processPayment(tenantId);
} catch (Exception e) {
    log.error("Error", e);
}
```

### 12. Method Naming
- Nazwy metod powinny jasno opisywać co robią
- Konwencje:
  - `is*` / `has*` - metody boolean
  - `get*` - gettery
  - `find*` - wyszukiwanie (może nie znaleźć)
  - `create*` - tworzenie
  - `update*` - aktualizacja
  - `delete*` - usuwanie

### 13. Single Responsibility Principle
- Jedna klasa/metoda = jedna odpowiedzialność
- Jeśli metoda robi więcej niż jedną rzecz, podziel ją

### 14. Dependency Injection
- Używaj constructor injection zamiast field injection
- Constructor injection jest łatwiejszy do testowania i jest immutable

```java
// ✅ Dobrze
@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class TenantService {
    private final TenantRepository repository;
    private final EmailService emailService;
}

// ❌ Źle
@Service
class TenantService {
    @Autowired
    private TenantRepository repository;

    @Autowired
    private EmailService emailService;
}
```

### 15. Constants
- Definiuj jako `private static final` w odpowiednich klasach
- Nazwy UPPER_SNAKE_CASE

```java
private static final int MAX_RETRY_ATTEMPTS = 3;
private static final String DEFAULT_CURRENCY = "PLN";
private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
```

### 16. Validation
- Używaj Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.) zamiast ręcznych sprawdzeń
- Waliduj na granicach systemu (Controller, Service API)

```java
// ✅ Dobrze
public record CreateTenantRequest(
    @NotBlank @Size(max = 100) String name,
    @Email String email,
    @NotNull @Positive BigDecimal rent
) {}

@PostMapping
ResponseEntity<TenantDto> create(@Valid @RequestBody CreateTenantRequest request) {
    // ...
}
```

### 17. Logging
- Używaj structured logging
- Odpowiednie poziomy: `DEBUG`, `INFO`, `WARN`, `ERROR`
- Nie loguj wrażliwych danych (hasła, tokeny, dane osobowe)
- Używaj placeholders zamiast konkatenacji

```java
// ✅ Dobrze
log.info("Creating tenant with name: {}", request.name());
log.error("Failed to process payment for tenant {}", tenantId, exception);

// ❌ Źle
log.info("Creating tenant with name: " + request.name()); // konkatenacja
log.error("Failed to process payment for tenant " + tenantId); // brak exception
```

### 18. Resource Management
- Używaj try-with-resources dla `AutoCloseable`
- Zamykaj zasoby w odpowiedniej kolejności

```java
// ✅ Dobrze
try (var reader = new BufferedReader(new FileReader(file))) {
    return reader.lines().toList();
}
```

## Baza danych i JPA

### 19. Entity Relationships
- Używaj `@JoinColumn` z odpowiednimi fetch strategies
- Domyślnie używaj `FetchType.LAZY`
- Używaj `FetchType.EAGER` tylko gdy konieczne

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;
```

### 20. Query Optimization
- Unikaj N+1 queries
- Używaj `@EntityGraph` lub `JOIN FETCH`
- Monitoruj wykonywane zapytania w logach (hibernate.show_sql)

```java
// ✅ Dobrze
@EntityGraph(attributePaths = {"tenant", "apartment"})
List<Rental> findAllWithTenantAndApartment();

// ❌ Źle - spowoduje N+1 queries
List<Rental> findAll(); // + lazy load tenant dla każdego
```

### 21. Auditing
- Wykorzystuj Spring Data JPA auditing
- Adnotacje: `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`
- Włącz auditing w konfiguracji: `@EnableJpaAuditing`

```java
@EntityListeners(AuditingEntityListener.class)
@Entity
class Tenant {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### 22. Transactions
- Odpowiednie używanie `@Transactional` z właściwymi propagation levels
- Domyślnie metody w Service powinny być transakcyjne
- Transakcje tylko na warstwie Service, nie na Repository

```java
@Service
@Transactional(readOnly = true)
class TenantService {

    @Transactional // write transaction
    public TenantDto create(CreateTenantRequest request) {
        // ...
    }

    public Optional<TenantDto> findById(Long id) {
        // read-only transaction from class level
    }
}
```

## Architektura i Design

### 23. Layer Separation
- Zachowuj podział na warstwy:
  - **Controller**: obsługa HTTP requests/responses, walidacja wejścia, mapowanie na DTO
  - **Service**: logika biznesowa, transakcje
  - **Repository**: dostęp do danych
- Nigdy nie wywołuj Repository bezpośrednio z Controller

### 24. DTO Mapping
- Używaj dedykowanych mapperów zamiast ręcznego kopiowania pól
- Mapowanie może być w:
  - Statycznych metodach fabrycznych w DTO
  - Dedykowanych klasach Mapper
  - MapStruct (rozważ dla większych projektów)

```java
// ✅ Dobrze - metoda w DTO
public record TenantDto(@NonNull Long id, @NonNull String name, @Nullable String email) {

    static TenantDto fromEntity(@NonNull Tenant tenant) {
        return new TenantDto(
            tenant.getId(),
            tenant.getName(),
            tenant.getEmail()
        );
    }

    Tenant toEntity() {
        return Tenant.builder()
            .id(id)
            .name(name)
            .email(email)
            .build();
    }
}

// ✅ Dobrze - dedykowana klasa Mapper
@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class TenantMapper {

    TenantDto toDto(@NonNull Tenant tenant) {
        return new TenantDto(tenant.getId(), tenant.getName(), tenant.getEmail());
    }

    Tenant toEntity(@NonNull TenantDto dto) {
        return Tenant.builder()
            .id(dto.id())
            .name(dto.name())
            .email(dto.email())
            .build();
    }
}
```

### 25. RESTful API Design
- Używaj odpowiednich metod HTTP:
  - `GET` - pobieranie zasobów
  - `POST` - tworzenie nowych zasobów
  - `PUT` - pełna aktualizacja zasobu
  - `PATCH` - częściowa aktualizacja zasobu
  - `DELETE` - usuwanie zasobu
- Odpowiednie kody odpowiedzi HTTP:
  - `200 OK` - sukces (GET, PUT, PATCH)
  - `201 Created` - zasób utworzony (POST)
  - `204 No Content` - sukces bez treści (DELETE)
  - `400 Bad Request` - błąd walidacji
  - `404 Not Found` - zasób nie znaleziony
  - `500 Internal Server Error` - błąd serwera
- Konsekwentne nazewnictwo endpointów (plural nouns)
- Używaj `@Valid` do walidacji request body

```java
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class TenantController {

    private final TenantService tenantService;

    @GetMapping("/{id}")
    ResponseEntity<TenantDto> getTenant(@PathVariable @NonNull Long id) {
        return tenantService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    ResponseEntity<Page<TenantDto>> getAllTenants(Pageable pageable) {
        return ResponseEntity.ok(tenantService.findAll(pageable));
    }

    @PostMapping
    ResponseEntity<TenantDto> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantDto created = tenantService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    ResponseEntity<TenantDto> updateTenant(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody UpdateTenantRequest request) {
        return tenantService.update(id, request)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteTenant(@PathVariable @NonNull Long id) {
        tenantService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 26. Error Handling
- Używaj `@ControllerAdvice` do globalnej obsługi błędów
- Zwracaj spójne struktury błędów

```java
@ControllerAdvice
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class GlobalExceptionHandler {

    @ExceptionHandler(SantanderException.class)
    ResponseEntity<ErrorResponse> handleSantanderException(SantanderException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message,
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }
}

record ErrorResponse(int status, String message, LocalDateTime timestamp) {}
```

## Testing

### 27. Test Structure
- Używaj konwencji Given-When-Then lub Arrange-Act-Assert
- Nazwy testów powinny opisywać co testują: `shouldReturnTenantWhenIdExists()`

### 28. Unit Tests
- Mockuj zależności używając Mockito
- Testuj logikę biznesową w Service
- Pokrycie kodu testami min. 80%

```java
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository repository;

    @InjectMocks
    private TenantServiceImpl service;

    @Test
    void shouldReturnTenantWhenIdExists() {
        // Given
        Long tenantId = 1L;
        Tenant tenant = new Tenant(tenantId, "John Doe", "john@example.com");
        when(repository.findById(tenantId)).thenReturn(Optional.of(tenant));

        // When
        Optional<TenantDto> result = service.findById(tenantId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("John Doe");
        verify(repository).findById(tenantId);
    }
}
```

### 29. Integration Tests
- Używaj `@SpringBootTest` dla testów integracyjnych
- Używaj testcontainers dla bazy danych

## Podsumowanie - Najważniejsze zasady

### Priorytet 1 - Bezpieczeństwo typów i null safety
1. **Records** dla DTO
2. **@NonNull/@Nullable** dla parametrów i pól
3. **Optional** jako typ zwracany zamiast null
4. **Bean Validation** na granicach systemu

### Priorytet 2 - Hermetyzacja i immutability
5. **Package-private** klasy i metody gdzie możliwe
6. **Final** dla pól
7. **Hermetyzacja adnotacji Lombok** (`access = AccessLevel.PROTECTED`)
8. **Immutable objects** (records, final fields)

### Priorytet 3 - Funkcyjne podejście
9. **Stream API** zamiast pętli for
10. **IntStream** dla iteracji numerycznych
11. **Optional API** (map, flatMap, orElse, etc.)
12. **Method references** gdzie możliwe

### Priorytet 4 - Clean Code
13. **Single Responsibility** - jedna odpowiedzialność na klasę/metodę
14. **Layer Separation** - Controller/Service/Repository
15. **Constructor Injection** zamiast field injection
16. **Specific Exceptions** zamiast generic
17. **Clear naming** - jasne, opisowe nazwy

### Priorytet 5 - Performance i optymalizacja
18. **Lazy Loading** jako domyślne
19. **@EntityGraph** do zapobiegania N+1
20. **@Transactional** z odpowiednimi poziomami
21. **Pagination** dla dużych zbiorów danych
