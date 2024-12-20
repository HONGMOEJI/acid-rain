package server.game;

import game.model.GameMode;
import game.model.DifficultyLevel;
import game.model.GameRoom;
import game.model.GameStatus;
import game.model.Word;
import game.model.LeaderboardEntry;

import server.GameServer;
import server.ClientHandler;

import java.util.concurrent.*;
import java.util.List;
import java.util.logging.Logger;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class ServerGameController {
    private static final Logger logger = Logger.getLogger(ServerGameController.class.getName());
    private final GameServer server;
    private final GameRoom room;
    private final ServerGameState gameState;
    private final ServerWordManager wordManager;
    private final LeaderboardManager leaderboardManager;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> spawnTask;
    private ScheduledFuture<?> phCheckTask;

    private static final double PH_CHECK_INTERVAL = 1.0; // 초
    private static final double PH_DECREASE_AMOUNT = 0.5;
    private static final int BLIND_EFFECT_DURATION = 5000; // 5초

    public ServerGameController(GameServer server, GameRoom room) {
        this.server = server;
        this.room = room;
        this.gameState = new ServerGameState(room);
        this.wordManager = new ServerWordManager(room.getGameMode());
        this.leaderboardManager = LeaderboardManager.getInstance();

        logger.info("게임 컨트롤러 생성: " + room.getRoomId());
    }

    public void startGame() {
        try {
            gameState.start();
            long spawnInterval = calculateWordSpawnInterval(room.getDifficulty());

            // 단어 생성 작업 시작
            spawnTask = scheduler.scheduleAtFixedRate(this::spawnWord,
                    0, spawnInterval, TimeUnit.MILLISECONDS);

            // pH 체크 작업 시작
            phCheckTask = scheduler.scheduleAtFixedRate(this::checkPH,
                    0, (long)(PH_CHECK_INTERVAL * 1000), TimeUnit.MILLISECONDS);

            logger.info("게임 시작됨: " + room.getRoomId());
        } catch (Exception e) {
            logger.severe("게임 시작 중 오류 발생: " + e.getMessage());
            stopGame();
            server.broadcastToRoom(room.getRoomId(), "ERROR|게임 시작 실패");
        }
    }

    private void spawnWord() {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            Word word = wordManager.getRandomWord();
            gameState.addWord(word);

            // 특수 효과 정보를 포함한 메시지 구성
            String spawnMessage;
            if (word.hasSpecialEffect()) {
                spawnMessage = String.format("WORD_SPAWNED|%s|%s|%d|%s",
                        room.getRoomId(),
                        word.getText(),
                        word.getX(),
                        word.getEffect().name());  // SCORE_BOOST 또는 BLIND_OPPONENT
            } else {
                spawnMessage = String.format("WORD_SPAWNED|%s|%s|%d",
                        room.getRoomId(),
                        word.getText(),
                        word.getX());
            }

            server.broadcastToRoom(room.getRoomId(), spawnMessage);
            logger.fine("단어 생성: " + word.getText() +
                    (word.hasSpecialEffect() ? ", 효과: " + word.getEffect() : ""));
        } catch (Exception e) {
            logger.severe("단어 생성 중 오류: " + e.getMessage());
        }
    }

    private void checkPH() {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            // 단순히 pH가 0 이하인지만 체크
            for (String player : room.getPlayers()) {
                double ph = gameState.getPlayerPH(player);
                if (ph <= 0) {
                    handleGameOver();
                    return;
                }
            }
        } catch (Exception e) {
            logger.severe("pH 체크 중 오류: " + e.getMessage());
        }
    }

    public void handleGameAction(String roomId, ClientHandler client, String action, String[] params) {
        try {
            // 리더보드 액션은 게임 상태와 무관하게 처리
            if ("LEADERBOARD".equals(action)) {
                if (params.length >= 3) {
                    handleLeaderboardAction(client, params[0], params[1], params[2]);
                    return;
                }
            }

            // 나머지 게임 액션들은 게임 진행 중일 때만 처리
            if (gameState.getStatus() != GameStatus.IN_PROGRESS) {
                return;
            }

            switch (action) {
                case "WORD_INPUT" -> {
                    if (params.length >= 1) {
                        handlePlayerInput(client, params[0]);
                    }
                }
                case "WORD_MISSED" -> {
                    if (params.length >= 1) {
                        handleWordMissed(params[0], client);
                    }
                }
                case "PLAYER_LEAVE_GAME" -> {
                    if (params.length >= 1) {
                        handlePlayerLeaveGame(client);
                    }
                }
                default -> {
                    logger.warning("알 수 없는 게임 액션: " + action);
                    client.sendMessage("ERROR|지원하지 않는 게임 액션입니다: " + action);
                }
            }
        } catch (Exception e) {
            logger.severe("게임 액션 처리 중 오류: " + e.getMessage());
            client.sendMessage("ERROR|게임 액션 처리 중 오류 발생");
        }
    }

    public void handlePlayerInput(ClientHandler player, String typedWord) {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            Word matchedWord = gameState.matchWord(typedWord, player.getUsername());
            if (matchedWord != null) {
                int newScore = gameState.getPlayerScore(player.getUsername());
                String opponent = gameState.getOpponentOf(player.getUsername());
                double opponentPH = opponent != null ? gameState.getPlayerPH(opponent) : 0.0;
                double playerPH = gameState.getPlayerPH(player.getUsername());

                // WORD_MATCHED 메시지 전송 (점수 정보 포함)
                server.broadcastToRoom(room.getRoomId(),
                        String.format("WORD_MATCHED|%s|%s|%s|%d",
                                room.getRoomId(), matchedWord.getText(), player.getUsername(), newScore));

                // pH 업데이트 메시지
                server.broadcastToRoom(room.getRoomId(),
                        String.format("PH_UPDATE|%s|%s|%.2f",
                                room.getRoomId(), player.getUsername(), playerPH));

                if (opponent != null) {
                    server.broadcastToRoom(room.getRoomId(),
                            String.format("PH_UPDATE|%s|%s|%.2f",
                                    room.getRoomId(), opponent, opponentPH));
                }

                // 특수효과 처리
                if (matchedWord.hasSpecialEffect()) {
                    switch (matchedWord.getEffect()) {
                        case BLIND_OPPONENT:
                            if (opponent != null) {
                                // BLIND_EFFECT 메시지 전송 (예: 5초=5000ms)
                                server.broadcastToRoom(room.getRoomId(),
                                        String.format("BLIND_EFFECT|%s|%s|%d",
                                                room.getRoomId(), opponent, 5000));
                            }
                            break;
                        case SCORE_BOOST:
                            // SCORE_BOOST는 점수 계산 시 이미 반영됨
                            break;
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("단어 입력 처리 중 오류: " + e.getMessage());
        }
    }

    public void handleWordMissed(String word, ClientHandler player) {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            synchronized(gameState) {
                gameState.removeWord(word);  // 단어 제거

                // 모든 플레이어의 pH 감소
                for (String playerName : room.getPlayers()) {
                    gameState.decreasePH(playerName, 0.2);  // 단어 놓칠 때마다 0.2 감소
                    double newPH = gameState.getPlayerPH(playerName);

                    server.broadcastToRoom(room.getRoomId(),
                            String.format("WORD_MISSED|%s|%s|%s|%.2f",
                                    room.getRoomId(), word, playerName, newPH));

                    if (newPH <= 0) {
                        handleGameOver();
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("단어 놓침 처리 중 오류: " + e.getMessage());
        }
    }

    public void handlePlayerLeaveGame(ClientHandler player) {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            String leavingPlayer = player.getUsername();
            String opponent = gameState.getOpponentOf(leavingPlayer);

            // 게임 상태 종료
            stopGame();

            if (opponent != null) {
                // 점수 정보 가져오기
                int winnerScore = gameState.getPlayerScore(opponent);
                int loserScore = gameState.getPlayerScore(leavingPlayer);

                // 리더보드 등록 시도
                if (leaderboardManager.addEntry(opponent, winnerScore,
                        room.getGameMode(), room.getDifficulty())) {
                    int rank = leaderboardManager.getUserRank(opponent,
                            room.getGameMode(), room.getDifficulty());
                    server.broadcastToRoom(room.getRoomId(),
                            "LEADERBOARD_UPDATE|" + room.getRoomId() + "|" + opponent + "|" + rank);
                }

                // 게임 종료 메시지 전송 (몰수승/패 처리)
                server.broadcastToRoom(room.getRoomId(),
                        String.format("GAME_OVER|%s|%s|%d|%d|FORFEIT",
                                room.getRoomId(), opponent, winnerScore, loserScore));
            }

            logger.info("플레이어 게임 중 퇴장 (몰수패): " + leavingPlayer);
        } catch (Exception e) {
            logger.severe("플레이어 퇴장 처리 중 오류: " + e.getMessage());
        }
    }

    private void handleGameOver() {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
            String winner = gameState.getWinner();
            if (winner != null) {
                int winnerScore = gameState.getPlayerScore(winner);
                int loserScore = gameState.getOpponentScore(winner);

                // 리더보드 등록 시도
                if (leaderboardManager.addEntry(winner, winnerScore,
                        gameState.getGameMode(), gameState.getDifficulty())) {
                    int rank = leaderboardManager.getUserRank(winner,
                            gameState.getGameMode(), gameState.getDifficulty());
                    server.broadcastToRoom(room.getRoomId(),
                            "LEADERBOARD_UPDATE|" + room.getRoomId() + "|" + winner + "|" + rank);
                }

                server.broadcastToRoom(room.getRoomId(),
                        String.format("GAME_OVER|%s|%s|%d|%d",
                                room.getRoomId(), winner, winnerScore, loserScore));
            }

            stopGame();
            logger.info("게임 종료: " + room.getRoomId() + ", 승자: " + winner);
        } catch (Exception e) {
            logger.severe("게임 종료 처리 중 오류: " + e.getMessage());
        }
    }

    /**
     * 새로운 리더보드 요청 처리 메서드.
     * 예: GAME_ACTION|LEADERBOARD|GET_TOP|JAVA|EASY
     *    GAME_ACTION|LEADERBOARD|GET_MY_RECORDS|JAVA|EASY
     */
    public void handleLeaderboardAction(ClientHandler client, String leaderboardAction, String modeStr, String diffStr) {
        try {
            GameMode mode = GameMode.valueOf(modeStr.toUpperCase());
            DifficultyLevel difficulty = DifficultyLevel.valueOf(diffStr.toUpperCase());

            switch (leaderboardAction) {
                case "GET_TOP" -> {
                    List<LeaderboardEntry> topEntries = leaderboardManager.getTopEntries(mode, difficulty, 10);
                    sendLeaderboardEntries(client, topEntries, "TOP_SCORES");
                    logger.info("상위 기록 전송: " + mode + ", " + difficulty);
                }
                case "GET_MY_RECORDS" -> {
                    List<LeaderboardEntry> userEntries = leaderboardManager.getUserEntries(client.getUsername());
                    sendLeaderboardEntries(client, userEntries, "USER_RECORDS");
                    logger.info("사용자 기록 전송: " + client.getUsername());
                }
                default -> {
                    logger.warning("알 수 없는 리더보드 액션: " + leaderboardAction);
                    client.sendMessage("ERROR|알 수 없는 리더보드 액션입니다.");
                }
            }
        } catch (IllegalArgumentException e) {
            logger.warning("잘못된 모드 또는 난이도: " + e.getMessage());
            client.sendMessage("ERROR|잘못된 모드 또는 난이도입니다.");
        } catch (Exception e) {
            logger.severe("리더보드 처리 중 오류: " + e.getMessage());
            client.sendMessage("ERROR|리더보드 처리 중 오류가 발생했습니다.");
        }
    }

    private void sendLeaderboardEntries(ClientHandler client, List<LeaderboardEntry> entries, String responseType) {
        StringBuilder response = new StringBuilder(responseType);
        for (LeaderboardEntry entry : entries) {
            // 날짜 포맷을 LeaderboardEntry의 포맷과 일치시킴
            response.append("|").append(entry.toFileString());
        }
        client.sendMessage(response.toString());
        logger.info("리더보드 데이터 전송: " + responseType + ", 엔트리 수: " + entries.size());
    }

    public void stopGame() {
        try {
            if (spawnTask != null) {
                spawnTask.cancel(false);
            }
            if (phCheckTask != null) {
                phCheckTask.cancel(false);
            }
            scheduler.shutdown();
            gameState.end();

            logger.info("게임 중지됨: " + room.getRoomId());
        } catch (Exception e) {
            logger.severe("게임 중지 중 오류: " + e.getMessage());
        }
    }

    private long calculateWordSpawnInterval(DifficultyLevel diff) {
        return switch (diff) {
            case EASY -> 4000;    // 4초
            case MEDIUM -> 3500;  // 3.5초
            case HARD -> 2000;    // 2초
        };
    }
}
