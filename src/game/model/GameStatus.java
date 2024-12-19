package game.model;

public enum GameStatus {
    WAITING("대기중"),
    IN_PROGRESS("진행중"),
    FINISHED("종료됨"),
    PAUSED("일시정지");

    private final String displayName;

    GameStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}