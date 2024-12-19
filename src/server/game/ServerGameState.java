package server.game;

import game.model.GameMode;
import game.model.DifficultyLevel;
import game.model.GameRoom;
import game.model.GameStatus;
import game.model.Word;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ServerGameState는 서버 측에서 해당 게임 방의 상태를 관리한다.
 * - 각 플레이어의 점수, pH를 관리
 * - 현재 활성화된 단어 목록을 관리
 * - 게임 시작/종료/진행 상태 관리
 *
 * 이 클래스는 ServerGameController에서 사용하여 게임 진행 로직을 지원한다.
 */
public class ServerGameState {
    private final GameRoom room;
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final Map<String, Double> phValues = new ConcurrentHashMap<>();
    private final List<Word> activeWords = Collections.synchronizedList(new ArrayList<>());
    private volatile GameStatus status = GameStatus.WAITING;

    public ServerGameState(GameRoom room) {
        this.room = room;
        // 방에 있는 모든 플레이어의 점수와 pH 초기화
        for (String player : room.getPlayers()) {
            scores.put(player, 0);
            phValues.put(player, 7.0); // 초기 pH는 7.0 (중성)
        }
    }

    /**
     * 게임 시작 시 호출
     */
    public void start() {
        status = GameStatus.IN_PROGRESS;
    }

    /**
     * 게임 종료 시 호출
     */
    public void end() {
        status = GameStatus.FINISHED;
    }

    public GameStatus getStatus() {
        return status;
    }

    /**
     * 활성 단어 목록에 단어 추가
     */
    public synchronized void addWord(Word w) {
        activeWords.add(w);
    }

    /**
     * 플레이어가 단어를 맞추려고 할 때 호출.
     *
     * @param typedWord 플레이어가 입력한 단어
     * @param player    플레이어 이름
     * @return 맞춘 단어가 존재하면 true, 아니면 false
     */
    public boolean matchWord(String typedWord, String player) {
        // activeWords에서 해당 단어 검색
        Optional<Word> matched = activeWords.stream().filter(w -> w.getText().equals(typedWord)).findFirst();
        if (matched.isPresent()) {
            Word word = matched.get();
            activeWords.remove(word);

            // 점수 계산
            int basePoints = word.getText().length() * 10;
            int finalPoints = basePoints;
            if (word.hasSpecialEffect() && word.getEffect() == Word.SpecialEffect.SCORE_BOOST) {
                finalPoints = (int)(basePoints * 1.5);
            }

            addScore(player, finalPoints);
            return true;
        }
        return false;
    }

    /**
     * 특정 플레이어의 점수 증가
     */
    public void addScore(String player, int points) {
        scores.computeIfPresent(player, (k,v) -> v+points);
    }

    /**
     * 특정 플레이어의 pH 감소
     */
    public void decreasePH(String player, double amount) {
        phValues.computeIfPresent(player, (k,v) -> Math.max(0, v - amount));
    }

    /**
     * 게임 오버 판단 로직
     * 여기서는 pH가 0 이하인 플레이어가 있으면 게임 오버로 간주.
     */
    public boolean isGameOver() {
        for (Double ph : phValues.values()) {
            if (ph <= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 승자 결정 로직
     * pH > 0인 플레이어가 승리. 모두 0 이하라면 점수로 결정하는 등 로직 조정 가능.
     */
    public String getWinner() {
        // 먼저 살아남은 플레이어를 찾는다.
        List<String> alivePlayers = new ArrayList<>();
        for (String player : room.getPlayers()) {
            Double p = phValues.getOrDefault(player,0.0);
            if (p > 0) {
                alivePlayers.add(player);
            }
        }

        if (alivePlayers.size() == 1) {
            return alivePlayers.get(0);
        } else if (alivePlayers.isEmpty()) {
            // 모두 죽었으면 점수 높은 플레이어 승리
            return scores.entrySet().stream()
                    .max(Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(room.getPlayers()[0]); // 혹시 모르니 첫 플레이어
        } else {
            // 여러 명이 살아남았다면 점수로 승자 결정
            String topPlayer = null;
            int topScore = -1;
            for (String p : alivePlayers) {
                int s = scores.getOrDefault(p,0);
                if (s > topScore) {
                    topScore = s;
                    topPlayer = p;
                }
            }
            return topPlayer;
        }
    }

    public int getPlayerScore(String player) {
        return scores.getOrDefault(player,0);
    }

    public int getOpponentScore(String winner) {
        // 플레이어가 2명이라고 가정
        for (String p : scores.keySet()) {
            if (!p.equals(winner)) return scores.get(p);
        }
        // 상대 없는 경우
        return 0;
    }

    public String getOpponentOf(String player) {
        // 2명 게임 가정
        for (String p : scores.keySet()) {
            if (!p.equals(player)) return p;
        }
        return null;
    }

    public GameMode getGameMode() {
        return room.getGameMode();
    }

    public DifficultyLevel getDifficulty() {
        return room.getDifficulty();
    }
}
