// game/model/DifficultyLevel.java
package game.model;

public enum DifficultyLevel {
    EASY("쉬움", 1.0f),
    NORMAL("보통", 1.5f),
    HARD("어려움", 2.0f);

    private final String displayName;
    private final float speedMultiplier;

    DifficultyLevel(String displayName, float speedMultiplier) {
        this.displayName = displayName;
        this.speedMultiplier = speedMultiplier;
    }

    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    @Override
    public String toString() {
        return displayName;
    }
}