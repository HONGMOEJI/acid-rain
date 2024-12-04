// game/model/GameMode.java
package game.model;

public enum GameMode {
    JAVA("Java"),
    PYTHON("Python"),
    KOTLIN("Kotlin"),
    C("C");

    private final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}