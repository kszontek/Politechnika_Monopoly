package pl.pb.monopoly.domain;

// Rodzaje pol z kartami na planszy (klasyczne Monopoly: Szansa, Kasa miejska + nasze Wydarzenia).
public enum CardType {
    SZANSA("Szansa"),
    KASA_MIEJSKA("Kasa miejska"),
    WYDARZENIE("Wydarzenie");

    private final String displayName;

    CardType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
