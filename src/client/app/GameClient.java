// client/app/GameClient.java
package client.app;

import client.network.NetworkManager;
import client.network.MessageHandler;
import client.event.GameEventListener;
import client.ui.dialog.RoomListDialog;
import game.model.GameRoom;

public class GameClient {
    private final NetworkManager networkManager;
    private final MessageHandler messageHandler;
    private final String username;
    private GameEventListener eventListener;
    private RoomListDialog roomListDialog;

    public GameClient(String host, int port, String username) {
        this.username = username;
        this.messageHandler = new MessageHandler(this);
        this.networkManager = new NetworkManager(host, port, messageHandler);

        // 서버 연결 및 로그인
        connect(host, port);
    }

    private void connect(String host, int port) {
        try {
            networkManager.connect(host, port);
            // 로그인 메시지 전송
            sendMessage("LOGIN|" + username);
        } catch (Exception e) {
            throw new RuntimeException("서버 연결 실패: " + e.getMessage());
        }
    }

    // 이벤트 리스너 관리
    public void setEventListener(GameEventListener listener) {
        this.eventListener = listener;
    }

    public void handleEvent(String eventType, Object... data) {
        if (eventListener != null) {
            eventListener.onGameEvent(eventType, data);
        }
    }

    // 방 관련 메서드
    public void sendCreateRoomRequest(GameRoom room) {
        String roomInfo = String.format("CREATE_ROOM|%s|%s|%s|%s|%d",
                room.getRoomName(),
                room.getPassword() != null ? room.getPassword() : "",
                room.getGameMode(),
                room.getDifficulty(),
                room.getMaxPlayers()
        );
        sendMessage(roomInfo);
    }

    public void sendJoinRoomRequest(String roomId, String password) {
        StringBuilder message = new StringBuilder("JOIN_ROOM|")
                .append(roomId)
                .append("|")
                .append(username);

        if (password != null) {
            message.append("|").append(password);
        } else {
            message.append("|");
        }

        sendMessage(message.toString());
    }

    public void sendRoomListRequest() {
        sendMessage("ROOM_LIST");
    }

    public void setRoomListDialog(RoomListDialog dialog) {
        this.roomListDialog = dialog;
    }

    // 채팅 메시지 전송
    public void sendChatMessage(String roomId, String message) {
        sendMessage("CHAT|" + roomId + "|" + message);
    }

    // 게임 설정 업데이트
    public void updateGameSettings(String roomId, String settingType, String value) {
        sendMessage("UPDATE_SETTINGS|" + roomId + "|" + settingType + "|" + value);
    }

    // 게임 시작 요청
    public void requestGameStart(String roomId) {
        sendMessage("START_GAME|" + roomId);
    }

    // 메시지 전송 공통 메서드
    public void sendMessage(String message) {
        networkManager.sendMessage(message);
    }

    // 게터 메서드
    public String getUsername() {
        return username;
    }

    public RoomListDialog getRoomListDialog() {
        return roomListDialog;
    }

    // 연결 종료
    public void disconnect() {
        networkManager.disconnect();
    }
}
