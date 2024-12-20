/*
 * client.app.GameClient.java
 * 게임 클라이언트 클래스
 */

package client.app;

import client.event.GameEventListener;
import client.network.MessageHandler;
import game.model.DifficultyLevel;
import game.model.GameMode;
import game.model.GameRoom;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class GameClient implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(GameClient.class.getName());
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final MessageHandler messageHandler;
    private GameEventListener eventListener;
    private final String username;
    private final ExecutorService executorService;
    private volatile boolean isRunning;
    private final String host;
    private final int port;
    private GameRoom currentRoom;

    public GameClient(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.messageHandler = new MessageHandler(this);
        this.executorService = Executors.newSingleThreadExecutor();
        this.isRunning = false;
    }

    // 소켓 연결을 위한 메서드
    public void connect() throws IOException {
        if (isRunning) {
            logger.warning("이미 연결되어 있습니다.");
            return;
        }

        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            isRunning = true;
            sendMessage("LOGIN|" + username);
            sendMessage("ROOM_LIST");
            startMessageReceiver();

            logger.info("서버에 연결되었습니다: " + host + ":" + port);
        } catch (IOException e) {
            cleanup();
            throw new IOException("서버 연결 실패: " + e.getMessage(), e);
        }
    }

    // 메시지 수신을 위한 메서드
    private void startMessageReceiver() {
        executorService.submit(() -> {
            try {
                String message;
                while (isRunning && (message = reader.readLine()) != null) {
                    messageHandler.handleMessage(message);
                }
            } catch (IOException e) {
                if (isRunning) {
                    logger.severe("메시지 수신 중 오류 발생: " + e.getMessage());
                    handleConnectionLost();
                }
            }
        });
    }

    // 기본 통신 메서드
    public void sendMessage(String message) {
        if (writer != null && isConnected()) {
            writer.println(message);
            logger.info("메시지 전송: " + message);
        } else {
            logger.warning("메시지 전송 실패 (연결 없음): " + message);
        }
    }

    // Game Lobby 관련 메서드
    public void sendPlayerListRequest(String roomId) {
        sendMessage("PLAYER_LIST|" + roomId);
    }

    public void sendCreateRoomRequest(GameRoom room) {
        String message = String.format("CREATE_ROOM|%s|%s|%s|%s|%d",
                room.getRoomName(),
                room.getPassword(),
                room.getGameMode().name(),
                room.getDifficulty().name(),
                room.getMaxPlayers()
        );
        sendMessage(message);
    }

    public void sendJoinRoomRequest(String roomId, String password) {
        StringBuilder message = new StringBuilder("JOIN_ROOM|" + roomId);
        if (password != null && !password.isEmpty()) {
            message.append("|").append(password);
        }
        sendMessage(message.toString());
    }

    public void sendLeaveRoomRequest(String roomId) {
        if (!isConnected()) {
            handleConnectionLost();
            return;
        }

        sendMessage("LEAVE_ROOM|" + roomId);
        currentRoom = null;

        if (eventListener != null) {
            eventListener.onGameEvent("ROOM_CLOSED", roomId, "방이 닫혔습니다.");
            sendMessage("ROOM_LIST");
        }
    }

    public void sendRoomChatMessage(String roomId, String message) {
        sendMessage("CHAT|" + roomId + "|" + message);
    }

    public void sendGameStartRequest(String roomId) {
        sendMessage("START_GAME|" + roomId);
    }

    // In-Game 관련 메서드
    public void sendGameAction(String roomId, String action, String... params) {
        StringBuilder message = new StringBuilder("GAME_ACTION|" + roomId + "|" + action);
        for (String param : params) {
            message.append("|").append(param);
        }
        sendMessage(message.toString());
    }

    public void sendGameEndRequest(String roomId, String winnerName, int myScore, int opponentScore) {
        sendMessage(String.format("GAME_END|%s|%s|%d|%d",
                roomId, winnerName, myScore, opponentScore));
    }

    public void sendUpdateSettingsRequest(String roomId, String settingType, String value) {
        sendMessage("UPDATE_SETTINGS|" + roomId + "|" + settingType + "|" + value);
    }

    // 리더보드(랭킹) 관련 메서드
    public void requestLeaderboardUpdate(String username, int score, GameMode mode, DifficultyLevel difficulty) {
        sendMessage(String.format("LEADERBOARD_UPDATE|%s|%d|%s|%s",
                username, score, mode.name(), difficulty.name()));
    }

    public void requestTopScores(GameMode mode, DifficultyLevel difficulty) {
        sendMessage(String.format("LEADERBOARD_REQUEST|TOP_SCORES|%s|%s",
                mode.name(), difficulty.name()));
    }

    public void requestUserRecords(String username) {
        sendMessage(String.format("LEADERBOARD_REQUEST|USER_RECORDS|%s", username));
    }

    // 이벤트 처리 메서드
    public void handleEvent(String eventType, Object... data) {
        if (eventListener != null) {
            try {
                eventListener.onGameEvent(eventType, data);
            } catch (Exception e) {
                logger.severe("이벤트 처리 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // 비정상적 연결 종료 처리 메서드
    private void handleConnectionLost() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        logger.warning("서버와의 연결이 끊어졌습니다.");
        handleEvent("CONNECTION_LOST");
        cleanup();
    }

    // 리소스 정리 메서드
    private void cleanup() {
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
            if (writer != null) {
                writer.close();
                writer = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            logger.severe("리소스 정리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (!executorService.isShutdown()) {
                executorService.shutdownNow();
            }
            eventListener = null;
            currentRoom = null;
        }
    }

    // 연결 종료 메서드
    public void disconnect() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        try {
            if (isConnected()) {
                sendMessage("LOGOUT");
                Thread.sleep(100); // 메시지 전송 대기
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("로그아웃 메시지 전송 중 인터럽트 발생");
        } finally {
            cleanup();
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    // Getter/Setter 메서드
    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
        logger.info("이벤트 리스너 설정됨: " + (listener != null ? listener.getClass().getSimpleName() : "null"));
    }

    public GameEventListener getEventListener() {
        return eventListener;
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && isRunning;
    }

    public GameRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(GameRoom room) {
        this.currentRoom = room;
    }
}