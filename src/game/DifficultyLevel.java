package game;

public enum DifficultyLevel {
    EASY(2000), // 2초
    MEDIUM(1500), // 1.5초
    HARD(1000);

    private final int wordGenerationDelay;

    DifficultyLevel(int delay) {
        this.wordGenerationDelay = delay;
    }

    public int getDelay() {
        return wordGenerationDelay;
    }
}
