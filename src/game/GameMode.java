package game;

public enum GameMode {
    SINGLE("1인 플레이"),
    MULTI("2인 플레이");

    private final String description;

    GameMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
