# Pełna Dokumentacja Projektu — Gra o Przetrwanie (Politechnika Monopoly)

Projekt „Gra o Przetrwanie” to zaawansowana, wieloosobowa gra planszowa online, będąca satyrą na życie studenckie na Politechnice Białostockiej. Poniżej znajduje się kompleksowa dokumentacja użytkownika i techniczna.

---

## I. INSTRUKCJA UŻYTKOWNIKA (USER MANUAL)

### 1. Wymagania systemowe i uruchomienie
*   **Java**: Wymagana wersja **Java 21** (JDK).
*   **Pamięć RAM**: Minimum 2GB.
*   **Przeglądarka**: Chrome, Firefox lub Edge (zalecane wsparcie dla WebGL/Three.js).

#### Sposoby uruchomienia (Windows):
1.  **Profil Deweloperski (H2)** — Baza w pamięci, dane resetują się przy restarcie. Najlepszy do szybkiej prezentacji.
    ```powershell
    .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
    ```
2.  **Profil Produkcyjny (PostgreSQL)** — Wymaga zainstalowanego i skonfigurowanego serwera PostgreSQL (skrypt w `db/postgres-setup.sql`).
    ```powershell
    .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
    ```

### 2. Pierwsze kroki (Quick Start)
Po wejściu na [http://localhost:8080](http://localhost:8080):
*   **Zaloguj się**: Skorzystaj z konta `admin` / `admin123` lub `gracz` / `gracz123`.
*   **Rejestracja**: Możesz stworzyć własne konto (`/register`). System automatycznie pobierze dla Ciebie awatar z serwisu DiceBear.
*   **Dashboard**: Tutaj sprawdzisz swoje ELO, poziom oraz obejrzysz historię ostatnich meczów.

### 3. Mechanika Rozgrywki
*   **Lobby**: Stwórz pokój, zaproś znajomych z listy lub dodaj Boty, aby przetestować grę samemu.
*   **Plansza 3D**: Gra wykorzystuje silnik Three.js. Możesz swobodnie obracać kamerą.
*   **System Wykupu (Takeover)**: Unikalna funkcja pozwalająca na przejęcie pola przeciwnika, jeśli na nim staniesz. Cena to 200% wartości gruntu plus ulepszenia.
*   **Ekonomia**: Zamiast standardowych dolarów używamy PLN. Każde pole to znany obiekt na PB (np. Rektorat, Akademiki, Wydziały).
*   **Koło Fortuny**: Dostępne raz na 24h, nagradza za codzienną aktywność (Daily Streak).

---

## II. DOKUMENTACJA TECHNICZNA (TECHNICAL DOCS)

### 1. Architektura Systemu
Aplikacja oparta jest na frameworku **Spring Boot 3.5**. Zastosowano model warstwowy:
*   **Controller**: Obsługa żądań HTTP (MVC i REST).
*   **Service**: Logika biznesowa (silnik gry, boty, ekonomia).
*   **Repository**: Abstrakcja dostępu do danych (Spring Data JPA).
*   **Domain**: Model danych (Encje Hibernate).

### 2. Logika Rozgrywki (Game Engine)
Najważniejszym elementem technologicznym jest **`ActiveGameStore`**. 
*   **RAM-First Architecture**: Aktywne sesje gry są przechowywane w `ConcurrentHashMap` w pamięci operacyjnej serwera. 
*   **Dlaczego?**: Pozwala to na tysiące operacji (rzuty kostką, ruchy pionków) bez generowania zbędnych zapisów do bazy danych PostgreSQL.
*   **Finalizacja**: Dane są zapisywane do tabeli `game_sessions` i `match_history` dopiero w momencie zakończenia meczu.
*   **Locking**: Zastosowano `ReentrantLock` per sesja, co zapewnia bezpieczeństwo wątkowe przy jednoczesnych ruchach graczy i botów.

### 3. Bezpieczeństwo i Role
Wykorzystano **Spring Security 6**.
*   **BCrypt**: Wszystkie hasła są solone i hashowane.
*   **Hierarchia ról**:
    *   `ADMIN`: Pełny CRUD użytkowników, zarządzanie ekwipunkiem, weryfikacja dokumentów.
    *   `MODERATOR`: Możliwość nakładania ostrzeżeń i banowania graczy.
    *   `USER`: Standardowy dostęp do gry i profilu.
    *   `GUEST`: Dostęp tylko do strony głównej i profili publicznych.

### 4. API i Integracje
*   **REST API**: Ponad 30 endpointów obsługujących wszystko: od rankingu (`/api/ranking-najlepszych`) po akcje w grze (`/api/game/{id}/roll`).
*   **WebSocket/STOMP**: Wykorzystywany do natychmiastowego przesyłania stanu gry do wszystkich uczestników sesji bez odświeżania strony.
*   **DiceBear Client**: Aplikacja łączy się z zewnętrznym API w celu generowania unikalnych awatarów dla nowych użytkowników.

### 5. Frontend i UX
*   **Silnik Renderujący**: Customowy kod JavaScript wykorzystujący **Three.js** do animacji pionków i rzutów kostką.
*   **UI Framework**: Czysty HTML5/CSS3 z elementami Tailwind CSS dla zachowania nowoczesnego „Dark Mode”.
*   **WCAG 2.1**: Aplikacja spełnia standardy dostępności (kontrasty, znaczniki aria, obsługa klawiaturą).

---

## III. MAPA WYMAGAŃ (COMPLIANCE MAP)

| Kategoria | Funkcjonalność | Lokalizacja w kodzie |
|---|---|---|
| **Dane** | Relacje 1-1, 1-N, N-N | `domain/` (User, Statistics, MatchHistory) |
| **Dane** | Walidacja (6 reguł) | `dto/RegistrationForm.java`, `@PasswordMatches` |
| **Logic** | Aktywna gra w RAM | `service/ActiveGameStore.java` |
| **Logic** | Boty (AI) | `service/BotAutoplayService.java` |
| **UI** | Panel Admina + Cookies | `AdminController.java`, `templates/admin/` |
| **API** | Usługa REST | `controller/rest/` |
| **API** | Klient REST (DiceBear) | `service/AvatarFetchService.java` |
| **API** | WebSockets | `config/WebSocketConfig.java` |

---

## IV. ROZWIĄZYWANIE PROBLEMÓW (TROUBLESHOOTING)
*   **Błąd 403 (Forbidden)**: Upewnij się, że masz odpowiednią rolę lub wyczyść ciasteczka sesji.
*   **Problem z bazą Neon/Postgres**: Sprawdź zmienne środowiskowe w `application-postgres.properties`. W razie problemów użyj profilu `h2`.
*   **Plansza się nie ładuje**: Sprawdź konsolę przeglądarki (F12) — upewnij się, że WebGL jest włączony i nie ma błędów ładowania modeli `.glb`.

---
*Dokumentacja przygotowana na zaliczenie projektu z przedmiotu Programowanie aplikacji WWW (2026).*
