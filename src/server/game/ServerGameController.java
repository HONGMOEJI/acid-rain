package server.game;

import game.model.DifficultyLevel;
import game.model.GameRoom;
import game.model.GameStatus;
import game.model.Word;
import server.GameServer;
import server.ClientHandler;

import java.util.concurrent.*;

public class ServerGameController {
    private final GameServer server;
    private final GameRoom room;
    private final ServerGameState gameState;
    private final ServerWordManager wordManager;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> spawnTask;

    public ServerGameController(GameServer server, GameRoom room) {
        this.server = server;
        this.room = room;
        this.gameState = new ServerGameState(room);
        this.wordManager = new ServerWordManager(room.getGameMode());
    }

    public void startGame() {
        gameState.start();
        long spawnInterval = calculateWordSpawnInterval(room.getDifficulty());
        spawnTask = scheduler.scheduleAtFixedRate(this::spawnWord, 0, spawnInterval, TimeUnit.MILLISECONDS);
        // 클라이언트에는 이미 GameServer에서 GAME_START를 보냈다고 가정
    }

    private void spawnWord() {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;
        Word word = wordManager.getRandomWord();
        gameState.addWord(word);
        server.broadcastToRoom(room.getRoomId(),
                "WORD_SPAWNED|" + room.getRoomId() + "|" + word.getText() + "|" + word.getX());
    }

    public void handlePlayerInput(ClientHandler player, String typedWord) {
        if (gameState.getStatus() != GameStatus.IN_PROGRESS) return;
        boolean matched = gameState.matchWord(typedWord, player.getUsername());
        if (matched) {
            // 점수 갱신 후 알림
            int newScore = gameState.getPlayerScore(player.getUsername());
            server.broadcastToRoom(room.getRoomId(),
                    "WORD_MATCHED|" + room.getRoomId() + "|" + typedWord + "|" + player.getUsername() + "|" + newScore);

            // 아이템 단어 효과 적용 필요시 여기서 처리 가능
            // 만약 아이템 효과를 적용하려면 matchWord에서 matchedWord 반환하도록 로직 수정 필요

        } else {
            // 단어가 없거나 이미 누가 맞춤
        }

        checkGameOver();
    }

    private void checkGameOver() {
        if (gameState.isGameOver()) {
            String winner = gameState.getWinner();
            int winnerScore = gameState.getPlayerScore(winner);
            int oppScore = gameState.getOpponentScore(winner);
            server.broadcastToRoom(room.getRoomId(),
                    "GAME_OVER|" + room.getRoomId() + "|" + winner + "|" + winnerScore + "|" + oppScore);
            stopGame();
        }
    }

    public void stopGame() {
        if (spawnTask != null) spawnTask.cancel(true);
        scheduler.shutdownNow();
        gameState.end();
    }

    private long calculateWordSpawnInterval(DifficultyLevel diff) {
        return switch (diff) {
            case EASY -> 2000;
            case MEDIUM -> 1500;
            case HARD -> 1000;
        };
    }
}
