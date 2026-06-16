package pl.pb.tc.domain;

public enum Role {
    ADMIN("Administrator"),
    MODERATOR("Moderator"),
    USER("Uzytkownik");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
