# Przypomnienie o kaucji i lista dodatkowych naliczeń

## Kwoty i dotychczasowe reguły

- `Settlement.totalAmount` pozostaje kwotą wystawionego rozliczenia. Zawiera zwykłe opłaty i wyłącznie ręcznie dodane pozycje naliczeń. Wpłaty go nie zmieniają.
- `RentalBalanceDto.amountDue` jest bieżącym zadłużeniem całego najmu, z wcześniejszymi rozliczeniami, pokryciem historycznym i rzeczywistymi wpłatami. Przycisk pełnej wpłaty korzysta z tej kwoty, obliczanej ponownie pod blokadą najmu.
- `depositReminder` oznacza brakującą, faktycznie nieotrzymaną kaucję (kaucja z umowy minus otrzymana kaucja). Jest przypomnieniem przy kolejnych rozliczeniach i nie podnosi należności.
- `depositDue` jest już naliczoną, niezapłaconą częścią kaucji, zawartą w saldzie. `depositToCharge` oznacza część brakującej kaucji, którą można jeszcze ręcznie naliczyć bez dublowania długu.
- `depositRequired` i `depositPaid` pokazują pełną kaucję z umowy i kwotę faktycznie otrzymaną, łącznie ze stanem początkowym. Kafelek „Kaucja” w podsumowaniu rozliczeń prezentuje je jako „Umowa” i „Wpłacono”, niezależnie od kwoty ostatniej raty lub rozliczenia. Pusta kwota otrzymana jest wyświetlana jako „Nie podano”. Sekcja „Płatności” na kafelku najemcy na liście nie pokazuje podsumowania kaucji z umowy.
- Dotychczasowa „Suma” oznacza czynsz, internet, gaz, prąd i wodę. Zachowano jej znaczenie. Nowe pozycje są pokazane osobno, także w Excelu.
- Zysk na kafelku nadal wynosi odstępne × 0,915. Podatek w raportach nadal wynika wyłącznie z odstępnego. Dodatkowe naliczenie nie zmienia podstawy podatku ani wzoru zysku na kafelku.
- Raport finansowy zachowuje istniejącą definicję przychodu jako sumy kwot wystawionych rozliczeń i dochodu jako przychodu minus faktury i podatek. Nie utożsamia tego z rzeczywistymi przelewami. Zmiana definicji raportowego dochodu wymaga osobnej decyzji biznesowej.

## Naliczanie i wpłaty

Migracja 068 zachowuje historyczne kwoty i wymaga potwierdzenia otrzymanej kaucji w starszych umowach. Nowa umowa z jawną wpłatą, również 0 zł, ma potwierdzony stan. Wpłata przy podpisaniu jest zapisana jako stan początkowy, poza przelewami rozliczającymi saldo.

Żaden typ rozliczenia nie nalicza kaucji automatycznie. W formularzu widnieje przypomnienie oraz początkowo pusta lista dodatkowych naliczeń. Właściciel dodaje dowolne pozycje z kwotą, opcjonalnym opisem i rodzajem: Kaucja albo Inne naliczenie. Przycisk „Dodaj kaucję” tylko wstawia edytowalny wiersz do bieżącego formularza. Można zażądać części brakującej kaucji, a pozostałą część naliczyć w innym miesiącu.

Przykład: kaucja 2 500 zł, otrzymano 2 000 zł, zwykłe opłaty 2 434,79 zł. Bez dodatkowych pozycji rozliczenie wynosi 2 434,79 zł, a przypomnienie nadal 500 zł. Ręczne pozycje Kaucja 300 zł i Klucze 30 zł dają 2 764,79 zł. Po pełnej wpłacie tego rozliczenia otrzymana kaucja wynosi 2 300 zł, a następne przypomnienie 200 zł. Kolejne rozliczenie bez ręcznej pozycji kaucji nadal obejmuje tylko zwykłe opłaty.

Gdy naliczono 500 zł kaucji, a wpłacono na nią 300 zł, przypomnienie również wynosi 200 zł. Ta kwota jest już jednak zawarta w zadłużeniu. Formularz informuje o tym i nie pozwala naliczyć jej ponownie. Pełna wpłata obejmuje tylko już naliczone należności, bez nieujętej jeszcze w rozliczeniach części przypomnienia.

Wszystkie ręczne pozycje dodaje się w całości, również w niepełnym miesiącu. Puste i zerowe kwoty są pomijane; wartości ujemne lub z więcej niż dwoma miejscami po przecinku są odrzucane. Zapis rozliczenia nie tworzy wpłaty. Rodzaj Kaucja jest wybierany jawnie; treść opisu nie służy do odgadywania rodzaju pozycji.

Księgowanie dotyczy jednej kwoty: pełnego bieżącego zadłużenia albo dowolnej kwoty częściowej/innej. Formularz nowej wpłaty i korekty oraz historia wpłat nie pokazują osobnego rozbicia na kaucję. Właściciel podaje tylko kwotę, datę i opcjonalny opis. Po wcześniejszej częściowej wpłacie opcja pełnej wpłaty obejmuje wyłącznie pozostałą należność.

Backend rozlicza składniki w tle: nowa wpłata najpierw pokrywa pozostałe opłaty, następnie już ręcznie naliczoną kaucję. Ta część zwiększa otrzymaną kaucję w umowie. Korekta zachowuje wcześniejsze przypisanie w granicach poprawionej kwoty i należności; usunięcie je odwraca. Zmiana samego opisu lub daty nie zmienia otrzymanej kaucji. Po naliczeniu zmiany otrzymanej kaucji wprowadza się przez historię wpłat; wcześniejsze potwierdzone kwoty można zmieniać w umowie przed naliczeniem.

Nadpłata najpierw pokrywa pozostałe opłaty nowego rozliczenia, następnie ręcznie naliczoną kaucję. Samo przypomnienie nie wykorzystuje nadpłaty. Pokrytą część przypisuje się do istniejących przelewów, bez tworzenia nowego wpisu. Ich późniejsza korekta lub usunięcie koryguje również kaucję.

Lista dodatkowych naliczeń, w tym ręcznie dodana kaucja, jest widoczna w szczegółach kosztów na kafelku najemcy, historii okresów oraz w dodatkowym arkuszu Excela „Pozycje naliczeń”. Dotychczasowe kolumny zbiorcze w eksporcie zachowują sumę pozycji kaucji i pozostałych naliczeń.

## Cofnięcie i ponowienia

Po usunięciu rozliczenia otrzymana kaucja pozostaje odłożona i nie stanowi wolnej nadpłaty. Nowe ręczne naliczenie może obejmować wyłącznie brak nieujęty w pozostałych rozliczeniach. Rozliczenie zapamiętuje otrzymaną wcześniej część (`depositCarryIn`), aby korekta historycznej wpłaty zachowała poprawne zadłużenie i aby tych samych pieniędzy nie wykorzystać dwukrotnie. Dotyczy to również usunięcia jednej z kilku rat kaucji.

FE przesyła UUID operacji rozliczenia i zachowuje identyczne żądanie po utracie odpowiedzi. Tabela `settlement_operation` przechowuje również ślad usuniętych rozliczeń. Powtórzenie udanego zapisu nie nalicza pozycji ponownie; zmienione żądanie lub ponowienie cofniętej operacji zwraca 409. Starsi klienci mogą nadal zapisywać zwykłe rozliczenia bez UUID.

Operacje są mapowane jako encja `SettlementOperation` z kluczem złożonym z najemcy i UUID. Odczyt i zapis wykonuje `SettlementOperationRepository` (Spring Data JPA). `JdbcTemplate` występuje wyłącznie w testach.

Migracja 069 dodaje tabelę pozycji naliczeń i znosi ograniczenie do jednej raty kaucji. Historyczne naliczenia z migracji 068 zostają przeniesione na listę, bez zmiany kwot istniejących rozliczeń. Zmiana zachowania dotyczy przyszłych rozliczeń; nie usuwa już wystawionych należności.

Operacje finansowe blokują wiersz najmu. Błąd zapisu wycofuje całą transakcję. Zakończenie umowy i zwrot kaucji nie uruchamiają dopłaty.

Rozliczenia, historia, eksport i operacje zakończenia umowy są ograniczone do zalogowanego właściciela. Pusta kwota otrzymanej kaucji oznacza niewiadomą; przed wyliczeniem zwrotu lub zakończeniem umowy wymaga wpisania rzeczywistej kwoty, również 0 zł.

Korekta lub usunięcie wpłaty albo innego rozliczenia może uwolnić środki na kaucję. Takie pokrycie również przypisujemy do pozostałych rzeczywistych przelewów. Dzięki temu niespłacona kaucja nie przekracza całego zadłużenia, a pełna wpłata nadal wystarcza do jego spłaty.

Żądanie FE zawiera identyfikator najemcy. Jeśli obiekt ma już inną umowę, zapis ze starego okna zwraca 409 zamiast obciążać nowego najemcę.

## Weryfikacja

Ręczny scenariusz E2E na świeżej umowie: kaucja 2 500 zł, wpłacono 2 000 zł, zwykłe opłaty 2 434,79 zł. Dla powtarzalnych kwot można użyć rozliczenia startowego, które pomija koszty liczników.

| Krok | Oczekiwany wynik |
| --- | --- |
| Zapisz umowę | Przypomnienie 500 zł, saldo 0 zł. |
| Rozlicz bez dodatkowych pozycji | Kwota rozliczenia 2 434,79 zł, kaucja nie została naliczona. |
| Zaksięguj pełną wpłatę 2 434,79 zł | Saldo 0 zł, wpłacona kaucja nadal 2 000 zł, przypomnienie 500 zł. |
| W nowym rozliczeniu dodaj Kaucja 300 zł i Inne/Klucze 30 zł | Dwie pozycje, kwota rozliczenia 2 764,79 zł. Sam zapis nie zwiększa otrzymanej kaucji. |
| Zaksięguj pełną wpłatę 2 764,79 zł | Jeden wpis na 2 764,79 zł; otrzymana kaucja 2 300 zł, saldo 0 zł, przypomnienie 200 zł. |
| Utwórz kolejne rozliczenie bez pozycji Kaucja | Kwota rozliczenia 2 434,79 zł i przypomnienie 200 zł. Brak automatycznej dopłaty. |

Osobno sprawdź naliczenie całych 500 zł kaucji i wpłatę częściową 2 734,79 zł (2 434,79 zł opłat i 300 zł naliczonej kaucji). W księgowaniu podajesz wyłącznie łączną kwotę. Pozostaje 200 zł zadłużenia, a następne rozliczenie pokazuje przypomnienie 200 zł z informacją, że kwota jest już ujęta w saldzie. Próba ponownego naliczenia tych samych 200 zł jest odrzucana. Opcja pełnej wpłaty ma teraz wartość 200 zł. Sprawdź także korektę opisu bez zmiany kwoty, zmniejszenie i usunięcie wpłaty, usuwanie wiersza przed zapisem, historię pozycji po odświeżeniu oraz arkusz „Pozycje naliczeń” w Excelu.

Testy integracyjne na PostgreSQL obejmują brak automatycznego naliczenia, przypomnienie w kolejnych miesiącach, ręczne raty kaucji, częściową spłatę już naliczonej kaucji, kilka pozycji i opisów w jednym rozliczeniu, szczegóły i Excel, korekty i usuwanie, nadpłaty, migrację starszych danych i ponowienia zapisu. Sprawdzają też księgowanie jednej kwoty bez przesyłania podziału oraz zachowanie wcześniejszego przypisania przy korekcie. Testy FE sprawdzają pustą listę przy otwarciu, dodawanie i usuwanie wierszy, walidację kwot, limit łącznej kaucji, podgląd salda oraz brak pól i komunikatów o kaucji w formularzu księgowania i korekty.

Polecenia weryfikacji:

```text
mvn -q -Dtest=RentalPaymentIntegrationTest,RentalBalanceTest,RentalPaymentMigrationTest,OneOffChargeExportTest,ProrationCalculatorTest,FinancialReportServiceTest test
npm test -- --watch=false --browsers=ChromeHeadless --include=src/app/modules/rental-payments/*.spec.ts --include=src/app/modules/calculate-popup/*.spec.ts
npm run build -- --configuration development
```

Standardowy build produkcyjny `npm run build` zatrzymują przekroczenia dotychczasowego limitu 5 kB w niezmienionych stylach komponentów: calculate-popup (6,83 kB), invoice-list (7,59 kB), lease-termination-popup (7,89 kB), monthly-financial-report (5,69 kB), rented-object-settlement-list (7,34 kB). Konfiguracja limitów pozostała bez zmian.
