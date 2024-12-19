package client.app;

import client.event.GameEventListener;
import client.network.MessageHandler;
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

    public GameClient(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.messageHandler = new MessageHandler(this);
        this.executorService = Executors.newSingleThreadExecutor();
        this.isRunning = false;
    }

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

    public void sendMessage(String message) {
        if (writer != null && isConnected()) {
            writer.println(message);
            logger.info("메시지 전송: " + message);
        } else {
            logger.warning("메시지 전송 실패 (연결 없음): " + message);
        }
    }

    public void sendPlayerListRequest(String roomId) {
        sendMessage("PLAYER_LIST|" + roomId);
    }

    public void sendCreateRoomRequest(GameRoom room) {
        String message = String.format("CREATE_ROOM|%s|%s|%s|%s|%d",
                room.getRoomName(),
                room.getPassword(),
                room.getGameMode().getDisplayName(),
                room.getDifficulty().getDisplayName(),
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

        // 방 나가기 메시지 전송
        sendMessage("LEAVE_ROOM|" + roomId);

        // 방장이 혼자일 때는 클라이언트에서도 즉시 처리
        if (eventListener != null) {
            eventListener.onGameEvent("ROOM_CLOSED", roomId, "방이 닫혔습니다.");
            // ROOM_LIST 요청도 바로 보내서 최신 상태 확인
            sendMessage("ROOM_LIST");
        }
    }

    public void sendRoomChatMessage(String roomId, String message) {
        sendMessage("CHAT|" + roomId + "|" + message);
    }

    public void sendGameStartRequest(String roomId) {
        sendMessage("START_GAME|" + roomId);
    }

    public void sendGameAction(String roomId, String action, String... params) {
        StringBuilder message = new StringBuilder("GAME_ACTION|" + roomId + "|" + action);
        for (String param : params) {
            message.append("|").append(param);
        }
        sendMessage(message.toString());
    }

    public void sendUpdateSettingsRequest(String roomId, String settingType, String value) {
        sendMessage("UPDATE_SETTINGS|" + roomId + "|" + settingType + "|" + value);
    }

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

    private void handleConnectionLost() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        logger.warning("서버와의 연결이 끊어졌습니다.");
        handleEvent("CONNECTION_LOST");
        cleanup();
    }

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
        }
    }

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

    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
        logger.info("이벤트 리스너 설정됨: " + (listener != null ? listener.getClass().getSimpleName() : "null"));
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && isRunning;
    }
}