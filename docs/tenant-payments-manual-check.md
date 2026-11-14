# Wpłaty najemców — ręczna weryfikacja

Uruchom backend i frontend standardowo. Liquibase automatycznie wykona migrację `067-rental-payments.sql` przy uruchomieniu backendu.

## Dane historyczne

1. Otwórz **Finanse → Najemcy**. Przed utworzeniem nowych rozliczeń każdy najem z historycznymi rozliczeniami powinien mieć saldo **0 zł**, status **Opłacone** i sumę rzeczywistych wpłat **0 zł**.
2. Kliknij kafelek. Stare miesięczne rozliczenia powinny mieć te same kwoty i daty. Historia wpłat powinna być pusta.
3. Uruchom ponownie backend. Saldo powinno pozostać bez zmian. Ponowne uruchomienie po dodaniu nowych rozliczeń nie może ich automatycznie opłacać.

## Główna ścieżka

Wybierz jednego najemcę. Poniższe kwoty zakładają, że każde nowe rozliczenie wynosi **2 500 zł**. Jeżeli kwoty w Twojej umowie są inne, użyj rzeczywistej „Kwoty całkowitej” i odpowiednio przelicz oczekiwane saldo. Kwoty rozliczenia nie zmienia się w oknie wpłaty.

Po każdej operacji sprawdź kafelek oraz szczegóły otwierane kliknięciem kafelka. Saldo ujemne w interfejsie jest prezentowane jako dodatnia kwota pozostała do zapłaty.

| Krok | Co kliknąć | Oczekiwany wynik przy rozliczeniach po 2 500 zł |
|---|---|---|
| 1 | Na kafelku **Rozlicz**, wybierz datę nowego miesiąca i zatwierdź | Saldo −2 500 zł; do zapłaty 2 500 zł |
| 2 | **Zaksięguj wpłatę → Częściowa / inna wpłata**, wpisz **500**, zmień datę, dodaj opis i zapisz | Saldo −2 000 zł; historia: jedna wpłata 500 zł z podaną datą i opisem |
| 3 | **Rozlicz** kolejny miesiąc | Saldo −4 500 zł; wcześniejszy dług dodany tylko raz |
| 4 | Zaksięguj inną wpłatę **700** | Saldo −3 800 zł; suma wpłat 1 200 zł; dwa osobne wpisy |
| 5 | **Zaksięguj wpłatę → Pełna wpłata** | Księgowane jest **3 800 zł**; saldo 0 zł; **Opłacone**; suma wpłat 5 000 zł |
| 6 | Otwórz ponownie modal | Pełna wpłata jest niedostępna; przycisk księgowania nadal dostępny |
| 7 | Zaksięguj inną wpłatę **150** | Saldo +150 zł; **Nadpłata**; suma wpłat 5 150 zł |
| 8 | **Rozlicz** następny miesiąc | Saldo −2 350 zł; nadpłata pomniejszyła należność |
| 9 | Kliknij kafelek → **Historia wpłat → Edytuj** przy wpłacie 150 zł; zmień kwotę na **200** i potwierdź | Saldo −2 300 zł; jeden zmieniony wpis, bez dodatkowej wpłaty |
| 10 | **Usuń** przy wpłacie 200 zł i potwierdź | Saldo −2 500 zł; suma pozostałych wpłat 5 000 zł |
| 11 | Usuń najnowsze rozliczenie miesięczne przyciskiem kosza w jego wierszu | Saldo 0 zł; wcześniejsze rzeczywiste wpłaty pozostają w historii |

Saldo i status płatności są widoczne w górnym podsumowaniu. Przycisk „Zaksięguj wpłatę” w szczegółach znajduje się pod historią wpłat i jest dostępny również przy pustej historii. Status procesu „Rozliczono/Nierozliczone” jest niezależny od statusu płatności.

## Dodatkowe kliknięcia

- **Błędne kwoty:** wybierz inną wpłatę i wpisz kolejno pustą wartość, `0`, `-10`, `1,001` i tekst. Zapisu nie można zatwierdzić. `500,25` i `500.25` są poprawne.
- **Brak danych:** wybierz nowego najemcę bez rozliczeń i wpłat. Saldo wynosi 0 zł, historia jest pusta, pełna wpłata niedostępna. Inna wpłata tworzy nadpłatę.
- **Anulowanie:** anuluj modal oraz potwierdzenie edycji/usunięcia. Dane i saldo pozostają bez zmian.
- **Dwuklik:** szybko kliknij przycisk zapisu dwa razy. W historii powinna powstać jedna wpłata.
- **Odświeżenie:** po zapisie odśwież stronę i przejdź między kafelkami a szczegółami. Kwoty powinny pozostać identyczne.
- **Kolejne należności:** po pełnej spłacie utwórz nowe rozliczenie. Ponownie pojawia się kwota do zapłaty, o ile nie pokrywa jej nadpłata.
- **Koszty i zysk:** porównaj je przed i po wpłacie. Księgowanie, korekta i usunięcie wpłaty nie zmieniają kosztów, zysku ani kwot miesięcznych rozliczeń.
- **Nieaktywny najem:** przełącz filtr na „Nieaktywne”. Saldo i historia danego najmu nadal są dostępne; wpłaty innych najemców tego lokalu nie wpływają na nie.

## Model i stan weryfikacji

Saldo = rzeczywiste wpłaty + techniczne pokrycie historycznych rozliczeń − suma kwot całkowitych rozliczeń.

Techniczne pokrycie nie ma daty otrzymania pieniędzy i nie jest wpłatą. Przy korekcie historycznego rozliczenia jego pierwotne pokrycie pozostaje, więc zmiana kwoty wpływa na saldo. Przy usunięciu historycznego rozliczenia znika również jego techniczne pokrycie.

Kompilacja backendu i build deweloperski frontendu zakończyły się powodzeniem. Przed końcowym przeglądem przeszło 10 nowych testów backendu i 9 testów frontendu. Pełny zestaw backendu ujawnił kolizję danych nowych testów ze współdzielonymi fixture'ami istniejących testów; nowe testy otrzymały własny kontener PostgreSQL. Po tej korekcie testy nie były ponownie uruchamiane, zgodnie z prośbą o pozostawienie weryfikacji użytkownikowi.

Tymczasowa baza, dane demonstracyjne i serwery używane w trakcie pracy zostały usunięte lub zatrzymane. Powyższe kroki wykonujesz na danych środowiska, które sam uruchomisz.
