# Politechnika Monopoly (Spring Boot)

Działający szkielet aplikacji na prezentację postępu (20–30%): logowanie z rolami,
rejestracja z walidacją oraz panel CRUD do zarządzania użytkownikami.

## Uruchomienie

```powershell
cd "C:\Users\miko\Desktop\ja\monopoly"
.\mvnw.cmd spring-boot:run
```

- Start: http://localhost:8080
- Logowanie: http://localhost:8080/login
- Rejestracja: http://localhost:8080/register
- Panel admina (CRUD): http://localhost:8080/users (wymaga roli ADMIN)
- Konsola H2: http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:tc`, user `sa`, bez hasła)

Baza H2 jest w pamięci (`create-drop`) — dane są tworzone na nowo przy każdym starcie.

## Konta testowe

| Login | Hasło | Rola | Dostęp do /users |
|-------|-------|------|------------------|
| admin | admin123 | ADMIN | tak |
| moderator | moderator123 | MODERATOR | nie |
| gracz | gracz123 | USER | nie |

## Zrealizowane elementy

- **Spring Security z bazą** — BCrypt, `CustomUserDetailsService`, 3 role (ADMIN/MODERATOR/USER)
- **Rejestracja z walidacją** (>6 reguł) — `@NotBlank`, `@Size`, `@Pattern`, `@Email`, `@Min`, `@Max`
  oraz własna adnotacja `@PasswordMatches` (zgodność haseł)
- **CRUD MVC** użytkowników — lista, dodawanie, edycja, usuwanie, zmiana rangi
- **Strona powitalna** z rankingiem graczy (na razie statyczny, docelowo z bazy)
- **Relacja kompozycji** — `User` 1—1 `UserProfile` (`cascade = ALL`, `orphanRemoval`)
- **Widoki Thymeleaf** z elementami dostępności (skip-link, `aria-invalid`, `role="alert"`)

## Struktura

```
monopoly/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/
├── README.md
└── src/main/
    ├── java/pl/pb/tc/
    │   ├── MonopolyApplication.java
    │   ├── config/       SecurityConfig, DataInitializer, PageModelAdvice
    │   ├── controller/   AuthController, HomeController, UserManagementController
    │   ├── domain/       User, UserProfile, Role
    │   ├── dto/          RegistrationForm, RankingEntryDto
    │   ├── repository/   UserRepository
    │   ├── service/      UserService, CustomUserDetailsService
    │   └── validation/   PasswordMatches, PasswordMatchesValidator
    └── resources/
        ├── application.properties
        ├── static/css/   styles.css, home-page.css
        ├── static/js/    app.js
        └── templates/    index, login, register, dashboard,
                          fragments/layout, users/list, users/form
```
