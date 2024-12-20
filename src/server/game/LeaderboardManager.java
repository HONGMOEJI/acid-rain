/*
 * server.game.LeaderboardManager.java
 * 게임의 리더보드(순위표) 데이터를 관리하는 클래스.
 * 게임 모드와 난이도별로 분류된 점수를 파일 시스템에 저장하고 조회하는 기능을 제공함.
 */

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
    private static volatile LeaderboardManager instance;

    // 게임모드+난이도별 리더보드 캐시
    private final Map<String, List<LeaderboardEntry>> leaderboards = new HashMap<>();

    private LeaderboardManager() {
        initializeLeaderboards();
    }

    public static LeaderboardManager getInstance() {
        if (instance == null) {
            synchronized (LeaderboardManager.class) {
                if (instance == null) {
                    instance = new LeaderboardManager();
                }
            }
        }
        return instance;
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
                        LeaderboardEntry entry = LeaderboardEntry.fromString(line.trim());
                        entries.add(entry);
                    } catch (Exception e) {
                        logger.warning("잘못된 리더보드 엔트리 무시: " + line);
                    }
                }
            } catch (IOException e) {
                logger.severe("리더보드 파일 로드 실패 (" + key + "): " + e.getMessage());
            }
        }

        // 점수순 정렬
        entries.sort(Comparator.comparing(LeaderboardEntry::getScore).reversed()
                .thenComparing(LeaderboardEntry::getTimestamp));
        return entries;
    }

    private void saveLeaderboard(String key, List<LeaderboardEntry> entries) {
        Path filePath = Paths.get(LEADERBOARD_DIRECTORY + key + ".txt");
        try {
            List<String> lines = entries.stream()
                    .map(LeaderboardEntry::toFileString)
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

        // 최소 등록 점수 체크
        if (!isScoreQualified(score, difficulty)) {
            return false;
        }

        // 이전 기록이 있다면 제거
        entries.removeIf(e -> e.getUsername().equals(username));

        // 새 기록 추가
        LeaderboardEntry newEntry = new LeaderboardEntry(
                username, score, mode, difficulty, LocalDateTime.now());
        entries.add(newEntry);

        // 점수순 정렬 & 최대 개수 제한
        entries.sort(Comparator.comparing(LeaderboardEntry::getScore).reversed()
                .thenComparing(LeaderboardEntry::getTimestamp));

        if (entries.size() > MAX_ENTRIES_PER_CATEGORY) {
            entries = entries.subList(0, MAX_ENTRIES_PER_CATEGORY);
            leaderboards.put(key, entries);
        }

        // 변경된 리더보드 저장
        saveLeaderboard(key, entries);

        logger.info(String.format("새로운 리더보드 엔트리 추가: %s (%d점, %s, %s)",
                username, score, mode, difficulty));

        return true;
    }

    private boolean isScoreQualified(int score, DifficultyLevel difficulty) {
        return switch (difficulty) {
            case EASY -> score >= 500;
            case MEDIUM -> score >= 750;
            case HARD -> score >= 1000;
        };
    }

    public List<LeaderboardEntry> getTopEntries(GameMode mode, DifficultyLevel difficulty) {
        String key = getLeaderboardKey(mode, difficulty);
        List<LeaderboardEntry> entries = leaderboards.getOrDefault(key, new ArrayList<>());
        return new ArrayList<>(entries); // 방어적 복사
    }

    public List<LeaderboardEntry> getTopEntries(GameMode mode, DifficultyLevel difficulty, int limit) {
        List<LeaderboardEntry> entries = getTopEntries(mode, difficulty);
        if (entries.size() > limit) {
            entries = entries.subList(0, limit);
        }
        return entries;
    }


    public List<LeaderboardEntry> getUserEntries(String username) {
        return leaderboards.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.getUsername().equals(username))
                .sorted(Comparator.comparing(LeaderboardEntry::getScore).reversed()
                        .thenComparing(LeaderboardEntry::getTimestamp))
                .collect(Collectors.toList());
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

    public String formatEntriesForTransmission(List<LeaderboardEntry> entries) {
        return entries.stream()
                .map(this::formatEntryForTransmission)
                .collect(Collectors.joining("|"));
    }

    private String formatEntryForTransmission(LeaderboardEntry entry) {
        return String.format("%s,%d,%s,%s,%s",
                entry.getUsername(),
                entry.getScore(),
                entry.getGameMode().name(),
                entry.getDifficulty().name(),
                entry.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
