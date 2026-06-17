package pl.pb.monopoly.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Wspolna obsluga bledow dla REST gry - dzieki temu nie musimy w kazdym endpoincie pisac try/catch.
// IllegalArgumentException = blad reguly gry -> 400 z czytelnym komunikatem; reszta -> 500 i log.
@RestControllerAdvice(basePackageClasses = GameRestController.class)
public class GameApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GameApiExceptionHandler.class);

    // "kontrolowane" bledy logiki gry - lecą do gracza jako zrozumialy tekst
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // wszystko inne to juz nasz blad - logujemy ze stacktrace, userowi pokazujemy ogolny komunikat
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> serverError(Exception ex) {
        log.error("Game API error: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Blad serwera. Odswiez strone lub sprobuj ponownie."));
    }
}
