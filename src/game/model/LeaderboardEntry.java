package game.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class LeaderboardEntry implements Comparable<LeaderboardEntry> {
    private final String username;
    private final int score;
    private final GameMode gameMode;
    private final DifficultyLevel difficulty;
    private final LocalDateTime timestamp;

    public LeaderboardEntry(String username, int score, GameMode gameMode,
                            DifficultyLevel difficulty, LocalDateTime timestamp) {
        this.username = username;
        this.score = score;
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.timestamp = timestamp;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(LeaderboardEntry other) {
        // 점수로 먼저 비교
        int scoreCompare = Integer.compare(other.score, this.score);
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        // 점수가 같으면 시간순으로 (최신이 앞으로)
        return other.timestamp.compareTo(this.timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaderboardEntry that = (LeaderboardEntry) o;
        return score == that.score &&
                Objects.equals(username, that.username) &&
                gameMode == that.gameMode &&
                difficulty == that.difficulty &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, score, gameMode, difficulty, timestamp);
    }

    @Override
    public String toString() {
        return String.format("%s - %d points (%s, %s) at %s",
                username, score, gameMode.getDisplayName(),
                difficulty.getDisplayName(), timestamp);
    }

    // 문자열로부터 LeaderboardEntry 객체 생성 (파일에서 읽을 때 사용)
    public static LeaderboardEntry fromString(String str) {
        String[] parts = str.split(",");
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid leaderboard entry format: " + str);
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            GameMode mode = GameMode.valueOf(parts[2].trim().toUpperCase());
            DifficultyLevel diff = DifficultyLevel.valueOf(parts[3].trim().toUpperCase());
            LocalDateTime timestamp = LocalDateTime.parse(parts[4].trim(), formatter);

            return new LeaderboardEntry(
                    parts[0].trim(), // username
                    Integer.parseInt(parts[1].trim()), // score
                    mode,
                    diff,
                    timestamp
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse leaderboard entry: " + str, e);
        }
    }

    // 파일에 저장하기 위한 형식으로 변환
    public String toFileString() {
        return String.format("%s,%d,%s,%s,%s",
                username, score, gameMode.name(),
                difficulty.name(), timestamp);
    }

    // 랭킹 표시용 문자열 생성
    public String toDisplayString(int rank) {
        return String.format("#%d   %s   %,d점   %s   %s",
                rank, username, score,
                gameMode.getDisplayName(),
                difficulty.getDisplayName());
    }
}