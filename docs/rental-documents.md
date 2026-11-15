# Rachunki i rozliczenia kaucji

Dokument powstaje wyłącznie dla nowego rozliczenia lub zakończenia umowy. Dane stron,
kwoty, konto i odbiorcy są utrwalane w transakcji operacji finansowej. Osobny worker
generuje PDF i wysyła wiadomość po zatwierdzeniu transakcji. Dokumenty historyczne
nie są wystawiane ani wysyłane automatycznie.

## Podgląd szablonów

Uruchom klasę `pl.rentowne.pdf.RentalDocumentPreview` z głównego katalogu projektu,
np. jako zwykłą konfigurację Java w IntelliJ. Nie uruchamia Springa, bazy ani poczty.

Pięć przykładowych plików znajduje się w `output/pdf/`; wyrenderowane strony trafiają
do `target/pdf-preview/`. Narzędzie używa tego samego generatora, fontów Inter i logo
co wiadomości wysyłane przez aplikację. Przykłady zawierają fikcyjne dane.

Skład dokumentów wykonuje Apache FOP. Szablony `receipt.xsl` i `deposit-settlement.xsl`
w `src/main/resources/pdf/` korzystają ze wspólnego `layout.xsl`. FOP zawija tekst,
łamie strony i powtarza nagłówki tabel; w kodzie Java nie ma ręcznego wyliczania
współrzędnych wierszy. Inter jest osadzany z zasobów projektu, niezależnie od fontów
zainstalowanych na serwerze. Przepełnienie obszaru lub brak wymaganej grafiki/fontu
przerywa generowanie zamiast wysłać ucięty dokument. Nietypowo długie słowa mają
niewidoczne miejsca umożliwiające podział. PDFBox służy do renderowania podglądów,
a także pozostaje zależnością istniejącego odczytu faktur przez Tika.

## Konfiguracja

| Zmienna środowiskowa | Znaczenie / domyślna wartość |
| --- | --- |
| `DOCUMENTS_ENABLED` | Tworzenie dokumentów przy nowych operacjach; `true` |
| `DOCUMENT_WORKER_ENABLED` | Przetwarzanie trwałej kolejki; `true` |
| `DEPOSIT_DOCUMENT_MAIL_ENABLED` | Wysyłanie nowych rozliczeń kaucji; `false` |
| `DOCUMENT_PDF_DIRECTORY` | Katalog PDF; `data/rentalDocuments` |
| `DOCUMENT_ISSUE_CITY` | Miejscowość wystawienia; `Poznań` |
| `DOCUMENT_MAIL_FROM` | Nazwa i adres nadawcy; `Rentowne <rentowneApp@gmail.com>` |
| `DOCUMENT_MAIL_REPLY_TO` | Adres odpowiedzi; `rentowneApp@gmail.com` |

SMTP korzysta z istniejącej konfiguracji Spring Mail. Ustaw prawidłowe
`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME` i `SPRING_MAIL_PASSWORD`.
Domyślny transport to SMTP z STARTTLS na porcie 587. Obecne wartości zastępcze hasła
w konfiguracji projektu nie umożliwiają rzeczywistej wysyłki.

`APPLICATION_EMAIL_SENDER=fakeEmailService` symuluje wysyłkę i zapisuje `SIMULATED`.
Tryb rzeczywisty można wskazać przez `APPLICATION_EMAIL_SENDER=emailSimpleService`.
Proste wiadomości istniejące wcześniej w aplikacji nadal używają dotychczasowego API.

Katalog PDF musi znajdować się na trwałym dysku/volume i być uwzględniany w kopii
zapasowej razem z bazą. Nazwy plików są generowane z UUID dokumentu. Raz zapisany
PDF jest wykorzystywany w kolejnych próbach; brak lub zmiana pliku jest błędem,
a nie powodem odtworzenia dokumentu z aktualnych danych stron.

Wysyłka kaucji pozostaje wyłączona do osobnej weryfikacji potrąceń mediów i salda
przy zakończeniu umowy. Mimo tego PDF powstaje i zadanie otrzymuje `HELD`.
Włączenie flagi dotyczy nowych dokumentów; nie zwalnia wcześniej wstrzymanych zadań.

## Numeracja i anulowanie

Właściciel ma osobny roczny licznik rachunków i rozliczeń kaucji. Numer jest nadawany
według daty wystawienia w strefie `Europe/Warsaw`, niezależnie od miesiąca rozliczenia.
Pierwszy numer to `01/rok`. Ponowienie tego samego `requestId` rozliczenia nie tworzy
nowego dokumentu. Liczniki są aktualizowane atomowo w PostgreSQL.

Usunięcie rozliczenia anuluje jego rachunek i zachowuje numer, dane i istniejący PDF.
Niewysłane zadanie jest zatrzymywane. Jeśli rachunek został wysłany lub wynik wysyłki
jest nieznany, powstaje osobne powiadomienie o anulowaniu. Poprawione rozliczenie
otrzymuje nowy numer. Anulowanie i wysyłka są synchronizowane blokadą dokumentu.
Jeżeli proces przerwano po rozpoczęciu SMTP, anulowanie zachowuje wynik `UNKNOWN`
i zleca powiadomienie, nawet jeśli worker nie zdążył jeszcze odzyskać zadania.
Anulowanie dokumentu wysłanego wyłącznie w symulacji nie tworzy prawdziwego maila.

## Stany i obsługa błędów

| Stan | Znaczenie |
| --- | --- |
| `PENDING` | Oczekuje na przygotowanie/wysyłkę lub bezpieczne ponowienie |
| `PROCESSING` | Przejęte przez workera z czasową dzierżawą |
| `SENT` | Serwer SMTP przyjął wiadomość; nie jest to potwierdzenie przeczytania lub doręczenia do skrzynki |
| `SIMULATED` | Wykonano wyłącznie symulację |
| `HELD` | PDF zapisany, wysyłka kaucji wstrzymana |
| `FAILED` | Błąd trwały lub wykorzystane trzy próby |
| `UNKNOWN` | Nie można rozstrzygnąć, czy SMTP przyjął wiadomość; potrzebna weryfikacja przed ponowieniem |
| `CANCELLED` | Dokument anulowany przed wysłaniem |

Jednoznaczne błędy przejściowe są ponawiane po minucie i po pięciu minutach.
Przerwanie przygotowania pliku jest odzyskiwane po wygaśnięciu dwuminutowej dzierżawy.
Przerwanie procesu po rozpoczęciu SMTP prowadzi do `UNKNOWN`, bez automatycznego
powtórzenia. Każda próba ma osobny wpis w `DOCUMENT_DELIVERY_ATTEMPT`.

Tabele `RENTAL_DOCUMENT`, `DOCUMENT_DELIVERY` i historia prób stanowią podstawę
przyszłego widoku dokumentów. Interfejs listy, pobieranie i ręczne ponowienia są
przedmiotem zadania 3.

## Weryfikacja

Szablony mają przykłady i kontrolę wizualną, bez testów układu. Testy integracyjne
`DocumentDeliveryIntegrationTest` używają izolowanego PostgreSQL i lokalnego GreenMail.
Sprawdzają faktyczny MIME/PDF i odbiorców, kolejkę, błędy SMTP, numerację, transakcje,
równoległe przetwarzanie, anulowanie oraz blokadę wysyłki kaucji. Nie korzystają
z rzeczywistych skrzynek ani danych produkcyjnych.
