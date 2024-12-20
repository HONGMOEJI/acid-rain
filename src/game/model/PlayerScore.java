/*
 * game.model.PlayerScore.java
 * 플레이어의 점수를 나타내기 위한 모델 클래스
 */

package game.model;

import java.time.LocalDateTime;

public class PlayerScore implements Comparable<PlayerScore> {
    private final String username;
    private final int score;
    private final GameMode gameMode;
    private final DifficultyLevel difficulty;
    private final LocalDateTime timestamp;

    public PlayerScore(String username, int score, GameMode gameMode, DifficultyLevel difficulty) {
        this.username = username;
        this.score = score;
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public int compareTo(PlayerScore other) {
        // 점수를 기준으로 내림차순 정렬 (높은 점수가 먼저 오도록)
        return Integer.compare(other.score, this.score);
    }

    // Getters
    public String getUsername() { return username; }
    public int getScore() { return score; }
    public GameMode getGameMode() { return gameMode; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public LocalDateTime getTimestamp() { return timestamp; }
}