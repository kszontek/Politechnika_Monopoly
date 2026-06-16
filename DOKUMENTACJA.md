# Politechnika Monopoly — Dokumentacja deweloperska

> Dokumentacja dla programisty. Opisuje **stan aktualny kodu**
> (czerwiec 2026), architekturę, sposób uruchomienia, Struktura danych, silnik gry,
> API oraz znane pułapki. Jeśli coś w `README.md` mówi inaczej — **ten plik jest
> nowszy** (README opisuje stary „szkielet 20–30%" i walutę PLN; projekt jest dziś
> w pełni działającą grą z systemem monet).

---

## 1. Czym jest ten projekt (w jednym akapicie)

Wieloosobowa, przeglądarkowa gra planszowa typu **Monopoly / Business Tour**, osadzona
satyrycznie na kampusie **Politechniki Białostockiej** („Kampus PB", 40 pól). Gracze
rzucają kostką, kupują akademiki i wydziały, pobierają czynsze, ulepszają pola, grają
kartami i walczą o spełnienie jednego z warunków zwycięstwa. Wokół samej rozgrywki
dobudowano kompletny **system** rankingowy: profil gracza, ranking ELO, poziomy,
historia meczów, osiągnięcia, monety, sklep ze skrzynkami (skrzynkay), koło fortuny,
znajomi, publiczne profile, komentarze oraz panele administratora i moderatora.

Projekt powstał jako zaliczenie przedmiotu **„Programowanie aplikacji WWW w języku Java"**
, ale zakresem znacznie wykracza poza minimum kursowe.

---

## 2. użyte technologie

| Warstwa | Technologia |
|---|---|
| Język / build | **Java 17** (cel w `pom.xml`), Maven (przez wrapper `mvnw`/`mvnw.cmd`) |
| Framework | **Spring Boot 3.5.14** (parent POM) |
| Web / widoki | Spring MVC + **Thymeleaf** (+ `thymeleaf-extras-springsecurity6`) |
| REST | Spring Web (`@RestController`) |
| Czas rzeczywisty | **WebSocket + STOMP** (`spring-boot-starter-websocket`, SockJS po stronie klienta) |
| Dane | Spring Data JPA / **Hibernate** |
| Bazy | **PostgreSQL** (profile `localhost`/`postgres` → Neon) oraz **H2 in-memory** (profil `h2`, dev) |
| Bezpieczeństwo | **Spring Security** (logowanie formularzowe, role z bazy, hasła **BCrypt**) |
| Walidacja | Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Pattern`, `@Email`, `@Min`/`@Max`) + walidator własny |
| Frontend gry | **Three.js** (plansza 3D w `static/js/board3d.js`), Font Awesome, fonty Google |
| Klient REST (serwer→serwer) | `RestTemplate` (`config/AppConfig`) → API awatarów **DiceBear** |

> Uwaga: środowiskowo wykryto **JDK 21** na maszynie, ale projekt kompiluje się do
> bajtkodu **Java 17** (`maven.compiler.cel=17`) — działa na 17+.

---

## 3. Szybki start (uruchomienie)

### Wymagania
- Java 17+ (`java -version`).
- Maven **nie jest potrzebny** — używaj wrappera w katalogu projektu.

### ✅ Zalecane: profil `h2` (baza w pamięci, zero konfiguracji)

```powershell
# Windows (PowerShell) — z katalogu C:\havanew
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```
```bash
# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

- Aplikacja: <http://localhost:8080>
- Konsola H2: <http://localhost:8080/h2-console> — JDBC URL `jdbc:h2:mem:monopoly`, user `sa`, hasło puste.
- Profil `h2` ma `ddl-auto=create-drop` → schemat tworzy się sam przy każdym starcie,
  a `DataInitializer` wypełnia konta testowe i przykładowe dane.

### ⚠️ Bardzo ważne: domyślny profil to NIE h2

W `application.properties` ustawione jest `spring.profiles.active=localhost`. Profil
`localhost` (oraz `postgres`) celuje w **współdzieloną produkcyjną bazę PostgreSQL na
Neon** (`currentSchema=monopoly`, `ddl-auto=update`). Dlatego:

- **Samo `mvnw spring-boot:run` (bez `-Dspring-boot.run.profiles=h2`) łączy się z Neon.**
  Do developmentu/testów **zawsze podawaj profil `h2`**.
- Nie uruchamiaj profilu `localhost`/`postgres` bez świadomej zgody — to wspólna baza,
  a `show-sql=true` i scheduler co 30 s realnie ją obciążają.

### Profile konfiguracji (`src/main/resources/`)

| Profil | Plik | Baza | `ddl-auto` |
|---|---|---|---|
| `h2` | `application-h2.properties` | H2 in-memory | `create-drop` |
| `localhost` (domyślny) | `application-localhost.properties` | Neon PostgreSQL | `update` |
| `postgres` | `application-postgres.properties` | PostgreSQL/Neon (zawiera hasło) | `update` |
| `lan` | `application-lan.properties` | gra w sieci LAN (publiczny base-url) | — |

> Sekrety (hasło do Neon) są wprost w `application-postgres.properties`. To dług/ryzyko —
> docelowo do przeniesienia na zmienne środowiskowe (`DB_USER`/`DB_PASSWORD`). Nie
> commituj nowych sekretów.

### Konta testowe (Wstępnie wypełnione przez `config/DataInitializer` tylko gdy baza jest pusta)

| Login | Hasło | Rola |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `moderator` | `moderator123` | MODERATOR |
| `gracz` | `gracz123` | USER (ma historię meczów, znajomych, komentarz) |

Wzorzec hasła: **login + `123`**. Wstępnie wypełnione są też relacje znajomości i przykładowe mecze.

### Build / paczka

```powershell
.\mvnw.cmd clean package            # buduje plik wykonywalny w cel\monopoly-0.0.1-SNAPSHOT.jar
```
Dockerfile + katalog `deploy/` są przygotowane pod konteneryzację.

---

## 4. Architektura wysokiego poziomu

Klasyczny układ warstwowy Spring MVC, z jedną istotną decyzją architektoniczną:
**aktywna rozgrywka żyje w pamięci serwera (RAM), nie w bazie**.

```
przeglądarka (Thymeleaf + board3d.js / Three.js)
        │  HTTP (MVC: strony)        │  REST (JSON: akcje gry)     │  WebSocket/STOMP (push stanu)
        ▼                            ▼                             ▼
   controller/                 controller/rest/              service/GameSyncService
        │                            │                             ▲
        └──────────────► service/ (logika) ◄─────────────────────┘
                              │
        ┌─────────────────────┼───────────────────────────┐
        ▼                     ▼                           ▼
 ActiveGameStore        repository/ (JPA)            GameEconomy (reguły, stałe)
 (gra ACTIVE w RAM)         │
                            ▼
                     PostgreSQL / H2  (users, statystyki, historia, lobby, słowniki)
```

### Cykl życia rozgrywki (kluczowe!)

1. **Lobby (`WAITING`)** — `GameSession` + `GamePlayer` zapisywane normalnie w bazie
   (mało operacji): tworzenie pokoju, dołączanie, zapraszanie, boty, „gotowość".
2. **Start (`ACTIVE`)** — `GameService.startGame()` ustawia status, woła `beginActivePlay()`,
   zapisuje sesję raz, a potem **`loadIntoRam()`** odłącza obiekt od kontekstu JPA
   (`em.detach`) i wkłada do **`ActiveGameStore`** (`ConcurrentHashMap<Long, GameSession>`).
3. **Rozgrywka** — każdy ruch (`roll`, `buy`, `upgrade`, `playCard`, …) modyfikuje obiekt
   **w RAM** pod blokadą `ReentrantLock` per sesja (`withLock`). `persist()` dla gry w RAM
   **nie pisze do bazy** — zapis następuje dopiero na koniec.
4. **Koniec (`FINISHED`)** — `checkGameEnd()` wykrywa warunek zwycięstwa; `persistGameResults()`
   tworzy `MatchHistory`, aktualizuje ELO/poziom/monety/serie/osiągnięcia; `finalizeFinished()`
   robi `em.merge()` i usuwa sesję z RAM.

Po każdej zmianie stanu serwis woła `publishPublic(...)`, które **po commicie transakcji**
rozsyła `GameStateDto` przez WebSocket (`/topic/game/{id}`). Klient nasłuchuje na push,
a dodatkowo ma polling jako fallback.

---

## 5. Struktura pakietów (`src/main/java/pl/pb/monopoly`)

```
MonopolyApplication.java        # @SpringBootApplication, @EnableScheduling
config/                         # konfiguracja Springa (security, websocket, seed, media, presence, patch schematu)
domain/                         # Tabele JPA + enumy (model danych)
repository/                     # interfejsy Spring Data JPA
service/                        # logika biznesowa (silnik gry, ekonomia, system)
dto/                            # rekordy/klasy transferowe (REST + widoki)
controller/                     # Obsługa stron (zwracają widoki Thymeleaf)
controller/rest/                # kontrolery REST (zwracają JSON)
validation/                     # walidacja własna (@PasswordMatches)
util/                           # PublicUrlHelper (budowanie linków publicznych)
```

### Najważniejsze klasy wg rozmiaru/znaczenia
- `service/GameService.java` (~2800 linii) — **serce gry**, cały silnik rozgrywki.
- `static/js/board3d.js` (~3100 linii) — **plansza 3D** (Three.js), główny widok gry.
- `service/GameEconomy.java` — stałe i reguły ekonomii (ceny, czynsze, grupy kolorów).
- `service/skrzynkaService`, `BotAutoplayService` — sklep/skrzynki oraz boty.

---

## 6. Struktura danych (`domain/`)

### Tabele i relacje

| Encja | Tabela | Najważniejsze pola | Relacje |
|---|---|---|---|
| `User` | `users` | `username`, `email`, `firstName`, `lastName`, `age`, **`coins`** (start 1000), `role`, `verified`, `createdAt`, pola profilu (avatar/banner/bg/bio), zawieszenia | **1–1 połączenie** z `PlayerStatistics` (`cascade=ALL, orphanRemoval=true`) |
| `PlayerStatistics` | `player_statistics` | `eloPoints` (start 1000), `level`, `gamesPlayed`, `gamesWon`, `winStreak`, `dailyStreak`, daty `lastLogin`/`lastSpinDate`, stan koła/skrzynek | 1–1 z `User` |
| `GameSession` | `game_sessions` | `code`, `name`, `status` (enum), `createdAt`, `currentTurn`, ~20 pól `pending*` (oczekujące decyzje), `leaderId` | **1–* połączenie** z `GamePlayer` (`cascade=ALL, orphanRemoval=true`) |
| `GamePlayer` | `game_players` | `cash` (start 2 000 000), `position`, `color`, `bankrupt`, `turnOrder`, flagi kart (`shieldActive`, `skipNextRent`, …) | `*–1` do `GameSession` i `User`; 4 `@ElementCollection`: `ownedPositions`, `propertyLevels`, `landingCounts`, `handCards` |
| `MatchHistory` | `match_history` | `playedAt` (**Date**), `boardName`, `won`, `placement`, `playersCount`, `finalCash`, `durationMinutes`, `eloChange` | `*–1` do `User` |
| `Friendship` | `friendships` | para użytkowników + `FriendStatus` (PENDING/ACCEPTED) | `*–1` × 2 do `User` |
| `OwnedItem` | `owned_items` | `itemSlug`, `equipped`, `obtainedAt` | `*–1` do `User` |
| `ProfileComment` (+ `ProfileCommentLike`) | `profile_comments` | autor, cel, treść, odpowiedzi/lajki | `*–1` do `User` |
| `GameInvite` | `game_invites` | zaproszenia do pokoju + `GameInviteStatus` | do `GameSession`, `User` |
| `UserWarning` | `user_warnings` | ostrzeżenia moderacyjne | `*–1` do `User` |
| `Achievement` | `player_achievements` | `code` (`AchievementType`), `unlockedAt` | `*–1` do `User` |

### Enumy
`Role` (ADMIN, MODERATOR, USER, GUEST), `GameStatus` (WAITING, ACTIVE, FINISHED),
`FriendStatus`, `GameInviteStatus`, `HandCardType` (10 kart akcji), `AchievementType`,
`CardType`/`HandCardType`.

### Dwie różne „waluty" — nie pomyl ich!
- **`coins` (monety)** — waluta konta/system (pole `User.coins`, start 1000). Za nią
  kupujesz skrzynki w sklepie i dostajesz nagrody za mecz (+500 wygrana / +100 udział)
  oraz za osiągnięcia.
- **`cash` / pieniądze (PLN)** — waluta wewnątrz pojedynczej rozgrywki (pole `GamePlayer.cash`,
  start 2 000 000 PLN). Istnieje tylko w trakcie meczu, nie jest trwała.

---

## 7. Silnik gry — `GameService` + `GameEconomy`

### Plansza „Kampus PB" (40 pól)
Definicje statyczne w `GameService`:
- `TILES[40]` — nazwy pól (np. „Akademik Alfa", „Erasmus PB — Kurort", „Rektorat").
- `TILE_EFFECTS[40]` — opisy efektów.
- `CHANCE_CARDS` — 20 kart „Szansy" o tematyce studenckiej (USOS, stypendia, mandaty…).

Reguły i liczby w `GameEconomy` (stałe):
- `STARTING_CASH = 2 000 000`, `GO_BONUS = 300 000` (przejście przez START),
  `JAIL_BAIL = 200 000`, podatek `TAX_RATE = 10%` majątku.
- Typy pól: **PROPERTY** (z grupami kolorów), **RESORT** (kurorty: pola 5/15/25/35,
  czynsz 50/100/200/400 tys. wg liczby kurortów), **UTILITY** (pola 12/28, czynsz =
  suma oczek × 15 000), **CHANCE** (7/22/36), **COMMUNITY** (2/17/33 — stypendium
  +150 000), **TAX** (4/38), **JAIL** (10), **GO_TO_JAIL** (30), **FREE_PARKING** (20).
- 8 grup kolorystycznych (`COLOR_GROUPS`), tabela czynszów `TILE_RENT_TABLE`, ceny `TILE_PRICE`.
- Poziomy ulepszeń 0–4: grunt → 1 dom → 2 domy → 3 domy → **Biurowiec** (`MAX_PROPERTY_LEVEL=4`).
- `levelForElo(elo)` = `max(1, elo*6/1000 - 2)` (1000 ELO → poziom 4).
- `netWorth(player)` = gotówka + wartość nieruchomości (do rozstrzygania po czasie).

### Przebieg tury (`doRoll`)
Rzut 2 kostkami → ruch po modulo 40 → bonus za START → obsługa pola:
- **dublet** = dodatkowy rzut; **3 dublety** = prosto do Dziekanatu (więzienie),
- pole z kartą Szansy → losowanie i efekt pieniężny,
- pole nieruchomości: wolne → oferta kupna (`pendingPurchase`); cudze → naliczenie czynszu
  (z uwzględnieniem monopolu ×2, tarczy, „podwójnego czynszu"); własne → oferta ulepszenia.
- po czynszu możliwa **oferta wykupu działki** (mechanika Business Tour, `pendingTakeover`).

### Stany „oczekujące" (pending) — automat decyzji
`GameSession` trzyma zestawy pól `pending*`, które blokują kolejny rzut, dopóki gracz nie
podejmie decyzji: `pendingPurchase` (kup/pomiń/licytuj), `pendingPayment` (spłać dług /
sprzedaj / zbankrutuj), `pendingUpgrade` (ulepsz/pomiń), `pendingBuyback` (odkup po karcie
„Dekret Wywłaszczeniowy"), `pendingTakeover` (wykup cudzej działki), `pendingExtraRoll`.
Dla każdego stanu jest wariant ludzki, **botowy** (`*AsBot`) i **timeout** (`auto*Timeout`).

### Karty akcji w ręce (`HandCardType`, gracz startuje z 3)
`SKIP_RENT` (Karta Ochrony), `EXTRA_ROLL`, `ADD_CASH` (+300k), `SHIELD` (absorbuje do 300k),
`DESTROY_PROPERTY` (przejęcie cudzego pola, ofiara może odkupić za 2×), `FREE_UPGRADE`
(darmowy poziom przy monopolu), `JAIL_PASS`, `DOUBLE_RENT_NEXT`, `TELEPORT`, `SCHOLARSHIP_ALL`
(każdy rywal płaci 100k).

### Warunki zakończenia (`checkGameEnd`)
1. **Czas** — po 60 min (`GAME_DURATION_SECONDS`) wygrywa najwyższy `netWorth`.
2. **3 pełne monopole kolorystyczne.**
3. **Wszystkie 4 kurorty.**
4. **Ostatni niezbankrutowany gracz** (eliminacja).
5. Wszyscy zbankrutowali → remis; sami boci → wygrywa najbogatszy bot.

`appendWinAlerts()` dorzuca do logu ostrzeżenia „gracz blisko wygranej" (2/3 monopole, 2/3 kurorty).

### Działanie wielu osób i zapisywanie
- **`ActiveGameStore`** — RAM dla gier `ACTIVE` + `ReentrantLock` per sesja.
- Wszystkie publiczne metody mutujące owijają logikę w `withLock(sessionId, …)`.
- `persist()` rozróżnia: gra w RAM → bez zapisu (chyba że `FINISHED`); gra spoza RAM
  (lobby) → `sessionRepository.save()`.
- `@Scheduled enforceTimeLimits()` co 30 s domyka gry, którym minął czas.
- `publishPublic()` rejestruje broadcast WebSocket **`afterCommit`** (spójność z transakcją).

---

## 8. Boty i synchronizacja czasu rzeczywistego

- **`BotAutoplayService`** — planuje ruchy botów (rzut, decyzja kupna/ulepszenia/wykupu,
  spłata długu) z opóźnieniem, reagując na `onTurnUpdate(sessionId)`. `GameService`
  wstrzykuje go „leniwie" (`@Lazy`, cykl zależności) i woła `scheduleBotUpdate(...)`
  po każdej zmianie tury.
- **`GameSyncService`** + `config/WebSocketConfig` — broker STOMP; broadcast pełnego
  `GameStateDto` na `/topic/game/{id}` oraz reakcji (emotki) graczy.
- **`UserPresenceService` / `UserPresenceFilter` / `UserPresenceConfig`** — śledzenie
  obecności online; **`OrphanedSessionCleanup`** sprząta porzucone lobby.

---

## 9. Strony i wygląd — kontrolery i API

### Obsługa stron (zwracają widoki Thymeleaf)
| Ścieżka bazowa | Klasa | Funkcja |
|---|---|---|
| `/`, `/dashboard` | `HomeController` | strona powitalna z rankingiem; dashboard gracza  |
| `/login`, `/register` | `AuthController` | logowanie i rejestracja |
| `/u/{username}` | `PublicController` | **publiczny profil** (dostęp bez logowania — współdzielenie po linku) |
| `/game/**` | `GameController` | lobby, tworzenie/dołączanie, widok planszy `/game/{id}` |
| `/friends/**` | `FriendsController` | znajomi: szukanie, zaproszenia, akceptacja |
| `/wheel`, `/wheel/spin` | `WheelController` | koło fortuny (raz dziennie) |
| `/settings/**` | `SettingsController` | edycja profilu, hasło, **upload legitymacji** (weryfikacja) |
| `/u/{u}/comment`, `/comments/**` | `ProfileCommentController` | komentarze pod profilem (edycja/usuwanie/odpowiedzi/lajki) |
| `/admin/**` | `AdminController` | panel administratora (tylko ADMIN) |
| `/moderator/**` | `ModeratorController` | panel moderatora (ADMIN+MODERATOR) |
| `/shop/**`, `/skrzynka/**`, `/inventory/**` | `skrzynkaController` | sklep, otwieranie skrzynek, ekwipunek |

### REST API (JSON, `controller/rest/`)
- **`GET /api/ranking-najlepszych`** — ranking ELO (publiczny). `RankingRestController`.
- **`GET /api/admin/users`** — lista użytkowników w JSON (ADMIN). `AdminUserRestController`.
- **`GET /api/avatar/{username}`** — proxy/awatar. `AvatarController`.
- **`/api/game/{id}/**`** — pełne API rozgrywki (`GameRestController`, ~26 adresów API):
  `state` (GET), `roll`, `buy`, `skip`, `bid`, `sell`, `transfer`, `pay-debt`, `bankrupt`,
  `play-card`, `upgrade`, `skip-upgrade`, `buyback`, `skip-buyback`, `buyout`, `skip-buyout`,
  `ready`, `start`, `add-bot`, `remove-bot/{playerId}`, `invite`, `react`, `leave`
  oraz `invites` (GET/accept/decline). Błędy biznesowe → `400` z `{"error": "..."}`
  (`GameApiExceptionHandler`).

### Klient REST (serwer → usługa zewnętrzna)
`config/AppConfig` dostarcza `RestTemplate`; `service/AvatarFetchService` pobiera awatar
z **DiceBear** przy rejestracji (gdy użytkownik nie ma własnego).

---

## 10. Logowanie i uprawnienia (`config/SecurityConfig`)

- Dwa łańcuchy filtrów: osobny dla konsoli H2 (tylko profil `h2`) i główny dla aplikacji.
- Hasła: **BCrypt** (`PasswordEncoder`); użytkownicy z bazy (`CustomUserDetailsService`).
- Reguły dostępu (skrót):
  - publiczne: `/`, `/login`, `/register`, `/error`, statyki, `/u/**`,
    `GET /api/ranking-najlepszych`, `/api/avatar/**`, `/ws/**` (uwierzytelniony),
  - `/api/admin/**` i `/admin/**` → **ADMIN**,
  - `/moderator/**` → **ADMIN lub MODERATOR**,
  - reszta → uwierzytelniony.
- Logowanie formularzowe (`/login`), sukces → `/dashboard`; obsługa konta zawieszonego
  (`LockedException` → `/login?locked`). CSRF włączony (wyłączony tylko dla `/ws/**`);
  token CSRF wstrzykiwany w `<meta>` w `fragments/layout.html` i wysyłany przez fetch.

---

## 11. system (poza samą rozgrywką)

- **Monety + sklep** (`skrzynkaService`, `skrzynkaController`, `static/js/shop.js`) —
  3 skrzynki kupowane za monety (pionki 300 / tytuły 250 / ramki 350); animacja karuzeli;
  ekwipunek i zakładanie kosmetyków (`OwnedItem.equipped`). Założony pionek/kolor wchodzi
  do gry (`equippedPawnModel`, `equippedColor`). `PawnModelCatalog` mapuje skiny na modele GLTF.
- **Koło fortuny** (`WheelService`, `static/js/wheel.js`) — losowanie raz dziennie +
  `dailyStreak`; nagrody mogą dać kartę do ręki na start meczu lub bonus gotówki.
- **Osiągnięcia** (`AchievementType`, `AchievementService`) — nagroda w monetach, sprawdzane
  po meczu i retroaktywnie na dashboardzie (`checkAndAward`). Tabela `player_achievements`.
- **Znajomi** (`FriendService`) i **zaproszenia do gry** (`GameInvite`) — zapraszać do
  pokoju można tylko znajomych; powiadomienia push (`UserNotificationService`).
- **Komentarze** pod profilem (`ProfileCommentService`) — z odpowiedziami i lajkami.
- **Moderacja** (`ModerationService`, `ModeratorController`) — weryfikacja konta przez
  **legitymację studencką** (upload do prywatnego katalogu poza `/media`, podgląd tylko
  admin/mod), zawieszanie kont, ostrzeżenia (`UserWarning`).

---

## 12. Frontend

- **Thymeleaf** w `src/main/resources/templates/` (m.in. `index`, `dashboard`, `login`,
  `register`, `settings`, `wheel`, `friends`, `game/lobby`, `game/board`, `public/profile`,
  `admin/*`, `moderator/*`, `fragments/layout.html`, `fragments/comments.html`).
- **Plansza 3D** — `static/js/board3d.js` (Three.js + `GLTFLoader.js` dla modeli pionków).
  Komunikuje się z `/api/game/{id}/**`, nasłuchuje WebSocket i pokazuje zegar, modale
  decyzji, animacje kostki/ruchu, panele nieruchomości i kart.
- **Motyw** — jasny (`body.light-page`) / nocny (`html.pb-dark`); przełącznik
  `pbToggleTheme()` w `fragments/layout.html`, zapamiętany w `localStorage` (`pb-theme`),
  anty-błysk przed renderem. CSS: `styles.css` (dark), `light.css`, `home-page.css`.
- **WCAG 2.1** — `lang="pl"`, `aria-*`, `aria-pressed` na przełączniku motywu, skip-link,
  `scope`/`caption` w tabelach, klasa `.sr-only`. Statyki ładowane z cache-busting
  (`th:href="@{/css/styles.css(v='…')}"`).

Pozostałe pliki JS: `game-lobby.js`, `game-notifications.js`, `inventory-panel.js`,
`settings-media.js`, `match-carousel.js`, `pb-dialog.js`, `app.js`, `board-preview.js`.
(`board.js`, `skrzynka.js` to pozostałości po starej planszy 2D — patrz dług techniczny.)

---

## 13. Baza danych i skrypty (`db/`)

- Słowniki `board_tiles`/`board_cards`/`ranks`/`daily_tasks` istnieją jako tabele, ale silnik
  czyta reguły z kodu (`GameEconomy`) — to **podwójne źródło prawdy** (patrz niżej).
- Skrypty SQL w `db/`: m.in. `migrate-balance-to-coins.sql` (migracja `balance`→`coins`
  na Neon — Hibernate `update` nie dodał kolumny `NOT NULL` na tabeli z danymi),
  `drop-dead-tables.sql` (kasowanie martwych tabel — `update` sam ich nie usuwa).
- `config/DatabaseSchemaPatch` — ręczne łaty schematu wykonywane przy starcie.

---

## 14. Problemy i rzeczy do naprawy ⚠️

Najważniejsze rzeczy, które zaskoczą nowego programistę:

1. **Profil domyślny = Neon, nie H2.** Patrz §3. Zawsze uruchamiaj z `-Dspring-boot.run.profiles=h2`.
2. **różnice w bazie na Neon — kolumna `age`.** W `run.log` widać błąd
   `ERROR: column u1_0.age does not exist`: pole `User.age` jest w kodzie, ale produkcyjna
   baza Neon nie ma tej kolumny (`ddl-auto=update` nie zawsze dodaje kolumny `NOT NULL`).
   Na H2 (`create-drop`) problemu nie ma. Przy pracy na Postgresie trzeba wykonać
   `ALTER TABLE monopoly.users ADD COLUMN age ...` ręcznie (analogicznie jak przy `coins`).
3. **Podwójne źródło prawdy planszy** — `GameEconomy` (kod) vs tabele `board_*`. Zmiana
   ekonomii w kodzie nie aktualizuje słowników w bazie. Wybierz jedno źródło docelowo.
4. **`GamePlayer` ma `FetchType.EAGER`** na 4 kolekcjach — każdy odczyt sesji ładuje
   tabele pomocnicze. W RAM to bez znaczenia (gra w pamięci), ale dla lobby/historii
   warto rozważyć LAZY + projekcje.
5. **Polling + WebSocket równolegle** — `board3d.js` odpytuje stan co kilka sekund jako
   fallback, nawet gdy WebSocket działa. Można wyłączać polling przy aktywnym połączeniu.
6. **Martwy/stary kod** — stara plansza 2D (`board.js`, `board-preview.js`), `skrzynka.js`,
   szablon `game/room.html` bez kontrolera; część encji słownikowych nieczytanych w runtime.
7. **`README.md` jest nieaktualny** — opisuje „szkielet 20–30%", walutę PLN/`balance`,
   5 kont i Java 21. Ten plik (`README-NEW.md`) jest źródłem prawdy.
   wytkniętych braków (RAM, cookies sortowania, klient REST, filtrowanie) została już
   naprawiona — aktualny stan opisuje `docs/`.
9. **Testy** — `src/test` zawiera jedynie `ActiveGameStoreTest` i `AchievementServiceTest`;
   silnik `GameService` nie ma testów integracyjnych (kandydat do uzupełnienia).

---

---

## 16. Słowniczek pojęć z kodu

| Pojęcie | Znaczenie |
|---|---|
| pieniądze | gotówka w grze (PLN, `GamePlayer.cash`) |
| monety / coins | waluta konta (`User.coins`) |
| kurort | pole typu RESORT (czynsz rośnie z liczbą posiadanych kurortów) |
| Dziekanat | więzienie (pole 10) |
| WARUNEK | „idź do Dziekanatu" |
| pending* | oczekująca decyzja blokująca dalszy ruch |
| takeover / wykup | przejęcie cudzej działki po czynszu (Business Tour) |
| buyback / odkup | możliwość odkupienia pola przejętego kartą „Dekret Wywłaszczeniowy" |
| dublet | równe oczka na kostkach → dodatkowy rzut (3× → Dziekanat) |

---




