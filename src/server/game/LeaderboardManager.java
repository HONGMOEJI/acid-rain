package server.game;

import game.model.GameMode;
import game.model.DifficultyLevel;
import game.model.LeaderboardEntry;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LeaderboardManager {
    private static final Logger logger = Logger.getLogger(LeaderboardManager.class.getName());
    private static final String LEADERBOARD_DIRECTORY = "resources/leaderboard/";
    private static final int MAX_ENTRIES_PER_CATEGORY = 100;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, List<LeaderboardEntry>> leaderboards = new HashMap<>();

    public LeaderboardManager() {
        initializeLeaderboards();
    }

    private void initializeLeaderboards() {
        try {
            Files.createDirectories(Paths.get(LEADERBOARD_DIRECTORY));

            // 각 게임 모드와 난이도 조합에 대한 리더보드 초기화
            for (GameMode mode : GameMode.values()) {
                for (DifficultyLevel diff : DifficultyLevel.values()) {
                    String key = getLeaderboardKey(mode, diff);
                    leaderboards.put(key, loadLeaderboard(key));
                }
            }
            logger.info("리더보드 초기화 완료");
        } catch (IOException e) {
            logger.severe("리더보드 디렉토리 생성 실패: " + e.getMessage());
        }
    }

    private String getLeaderboardKey(GameMode mode, DifficultyLevel difficulty) {
        return mode.name().toLowerCase() + "_" + difficulty.name().toLowerCase();
    }

    private List<LeaderboardEntry> loadLeaderboard(String key) {
        Path filePath = Paths.get(LEADERBOARD_DIRECTORY + key + ".txt");
        List<LeaderboardEntry> entries = new ArrayList<>();

        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        String[] parts = line.split(",");
                        if (parts.length >= 5) {
                            entries.add(new LeaderboardEntry(
                                    parts[0], // username
                                    Integer.parseInt(parts[1]), // score
                                    GameMode.valueOf(parts[2]), // gameMode
                                    DifficultyLevel.valueOf(parts[3]), // difficulty
                                    LocalDateTime.parse(parts[4], DATE_FORMATTER) // timestamp
                            ));
                        } else {
                            logger.warning("잘못된 리더보드 엔트리 무시: " + line);
                        }
                    } catch (Exception e) {
                        logger.severe("잘못된 리더보드 엔트리 무시: " + line + ", 에러: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.severe("리더보드 파일 로드 실패 (" + key + "): " + e.getMessage());
            }
        } else {
            logger.warning("리더보드 파일不存在: " + filePath);
        }

        // 점수 순으로 정렬
        entries.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return entries;
    }

    private void saveLeaderboard(String key, List<LeaderboardEntry> entries) {
        Path filePath = Paths.get(LEADERBOARD_DIRECTORY + key + ".txt");
        try {
            List<String> lines = entries.stream()
                    .map(entry -> String.format("%s,%d,%s,%s,%s",
                            entry.getUsername(),
                            entry.getScore(),
                            entry.getGameMode().name(),
                            entry.getDifficulty().name(),
                            entry.getTimestamp().format(DATE_FORMATTER)))
                    .collect(Collectors.toList());

            Files.write(filePath, lines);
            logger.info("리더보드 저장 완료: " + key);
        } catch (IOException e) {
            logger.severe("리더보드 파일 저장 실패 (" + key + "): " + e.getMessage());
        }
    }

    public synchronized boolean addEntry(String username, int score,
                                         GameMode mode, DifficultyLevel difficulty) {
        String key = getLeaderboardKey(mode, difficulty);
        List<LeaderboardEntry> entries = leaderboards.get(key);

        if (entries == null) {
            entries = new ArrayList<>();
            leaderboards.put(key, entries);
        }

        // 새로운 엔트리 생성
        LeaderboardEntry newEntry = new LeaderboardEntry(
                username, score, mode, difficulty, LocalDateTime.now());

        // 최소 등록 점수 체크 (각 모드/난이도별로 다르게 설정 가능)
        if (!isScoreQualified(score, mode, difficulty)) {
            return false;
        }

        // 이미 더 높은 점수가 있는지 체크
        Optional<LeaderboardEntry> existingBetter = entries.stream()
                .filter(e -> e.getUsername().equals(username))
                .filter(e -> e.getScore() >= score)
                .findFirst();

        if (existingBetter.isPresent()) {
            return false;
        }

        // 기존 엔트리 제거 (같은 사용자)
        entries.removeIf(e -> e.getUsername().equals(username));

        // 새 엔트리 추가
        entries.add(newEntry);

        // 점수순 정렬
        entries.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        // 최대 개수 제한
        if (entries.size() > MAX_ENTRIES_PER_CATEGORY) {
            entries = entries.subList(0, MAX_ENTRIES_PER_CATEGORY);
        }

        // 변경된 리더보드 저장
        leaderboards.put(key, entries);
        saveLeaderboard(key, entries);

        logger.info(String.format("새로운 리더보드 엔트리 추가: %s (%d점, %s, %s)",
                username, score, mode, difficulty));

        return true;
    }

    private boolean isScoreQualified(int score, GameMode mode, DifficultyLevel difficulty) {
        // 난이도별 최소 등록 점수 설정
        int minScore = switch (difficulty) {
            case EASY -> 500;
            case MEDIUM -> 750;
            case HARD -> 1000;
        };
        return score >= minScore;
    }

    public List<LeaderboardEntry> getTopEntries(GameMode mode, DifficultyLevel difficulty, int limit) {
        String key = getLeaderboardKey(mode, difficulty);
        List<LeaderboardEntry> entries = leaderboards.getOrDefault(key, new ArrayList<>());

        logger.info(String.format("Retrieving top entries for %s, %s", mode, difficulty));
        logger.info("Found " + entries.size() + " entries");

        List<LeaderboardEntry> topEntries = entries.stream()
                .limit(limit)
                .collect(Collectors.toList());

        // 각 엔트리의 문자열 형식 로깅
        for (LeaderboardEntry entry : topEntries) {
            logger.info("Entry data: " + entry.toFileString());
        }

        return topEntries;
    }

    public List<LeaderboardEntry> getUserEntries(String username) {
        List<LeaderboardEntry> userEntries = new ArrayList<>();
        for (List<LeaderboardEntry> entries : leaderboards.values()) {
            entries.stream()
                    .filter(e -> e.getUsername().equals(username))
                    .forEach(userEntries::add);
        }
        return userEntries;
    }

    public int getUserRank(String username, GameMode mode, DifficultyLevel difficulty) {
        String key = getLeaderboardKey(mode, difficulty);
        List<LeaderboardEntry> entries = leaderboards.getOrDefault(key, new ArrayList<>());

        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getUsername().equals(username)) {
                return i + 1;
            }
        }
        return -1;
    }
}