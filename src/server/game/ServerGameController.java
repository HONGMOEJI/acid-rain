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
        this.leaderboardManager = new LeaderboardManager();
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
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;

        try {
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
                case "LEADERBOARD" -> {
                    if (params.length >= 3) {
                        handleLeaderboardAction(client, params[0], params[1], params[2]);
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
            Word matchedWord = null;
            boolean matched = false;

            synchronized (this) {
                // 여기서 matchWord만 호출하고, 이 메서드 내부에서 단어 제거와 점수/pH 처리를 모두 하도록 함
                matched = gameState.matchWord(typedWord, player.getUsername());
                if (matched) {
                    int newScore = gameState.getPlayerScore(player.getUsername());
                    String opponent = gameState.getOpponentOf(player.getUsername());
                    double opponentPH = gameState.getPlayerPH(opponent);
                    double playerPH = gameState.getPlayerPH(player.getUsername());

                    // 매칭 성공 메시지
                    server.broadcastToRoom(room.getRoomId(),
                            String.format("WORD_MATCHED|%s|%s|%s|%d|%.1f",
                                    room.getRoomId(), typedWord, player.getUsername(),
                                    newScore, playerPH));

                    // pH 업데이트 메시지
                    server.broadcastToRoom(room.getRoomId(),
                            String.format("PH_UPDATE|%s|%s|%.2f",
                                    room.getRoomId(), player.getUsername(),
                                    playerPH));

                    if (opponent != null) {
                        server.broadcastToRoom(room.getRoomId(),
                                String.format("PH_UPDATE|%s|%s|%.2f",
                                        room.getRoomId(), opponent, opponentPH));
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
        GameMode mode;
        DifficultyLevel difficulty;
        try {
            mode = GameMode.valueOf(modeStr.toUpperCase());
            difficulty = DifficultyLevel.valueOf(diffStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            client.sendMessage("ERROR|잘못된 모드 또는 난이도입니다.");
            return;
        }

        switch (leaderboardAction) {
            case "GET_TOP":
                List<LeaderboardEntry> topEntries = leaderboardManager.getTopEntries(mode, difficulty, 10);
                sendLeaderboardEntries(client, topEntries, "TOP_SCORES");
                break;

            case "GET_MY_RECORDS":
                List<LeaderboardEntry> userEntries = leaderboardManager.getUserEntries(client.getUsername());
                // 필요에 따라 특정 모드/난이도 필터링 가능
                // userEntries = userEntries.stream()
                //     .filter(e -> e.getGameMode() == mode && e.getDifficulty() == difficulty)
                //     .collect(Collectors.toList());
                sendLeaderboardEntries(client, userEntries, "USER_RECORDS");
                break;

            default:
                client.sendMessage("ERROR|알 수 없는 리더보드 액션입니다.");
        }
    }

    private void sendLeaderboardEntries(ClientHandler client,
                                        List<LeaderboardEntry> entries,
                                        String responseType) {
        StringBuilder response = new StringBuilder(responseType);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (LeaderboardEntry entry : entries) {
            response.append("|").append(String.format("%s,%d,%s,%s,%s",
                    entry.getUsername(),
                    entry.getScore(),
                    entry.getGameMode().name(),
                    entry.getDifficulty().name(),
                    entry.getTimestamp().format(formatter)));
        }
        client.sendMessage(response.toString());
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
