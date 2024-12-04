// client/network/MessageHandler.java
package client.network;

import client.app.GameClient;
import client.event.GameEvent;
import game.model.GameRoom;
import game.model.GameMode;
import game.model.DifficultyLevel;

public class MessageHandler {
    private final GameClient gameClient;

    public MessageHandler(GameClient gameClient) {
        this.gameClient = gameClient;
    }

    public void handleMessage(String message) {
        String[] parts = message.split("\\|");
        String messageType = parts[0];

        switch (messageType) {
            case "USERS":
                handleUsersUpdate(parts);
                break;
            case "ROOM_LIST_RESPONSE":
                handleRoomListResponse(parts);
                break;
            case "JOIN_ROOM_RESPONSE":
                handleJoinRoomResponse(parts);
                break;
            case "CHAT":
                handleChat(parts);
                break;
            case "PLAYER_UPDATE":
                handlePlayerUpdate(parts);
                break;
            case "SETTINGS_UPDATE":
                handleSettingsUpdate(parts);
                break;
            case "GAME_START":
                gameClient.handleEvent(GameEvent.GAME_STARTED);
                break;
        }
    }

    private void handleUsersUpdate(String[] parts) {
        if (parts.length >= 2) {
            int userCount = Integer.parseInt(parts[1]);
            gameClient.handleEvent("USERS_UPDATED", userCount);
        }
    }

    private void handleRoomListResponse(String[] parts) {
        // 방 목록 처리 로직
        gameClient.handleEvent(GameEvent.ROOM_LIST_UPDATED, parts);
    }

    private void handleJoinRoomResponse(String[] parts) {
        if (parts.length >= 3) {
            boolean success = Boolean.parseBoolean(parts[1]);
            String message = parts[2];
            if (success && parts.length >= 4) {
                GameRoom room = parseRoomInfo(parts[3]);
                gameClient.handleEvent(GameEvent.ROOM_JOINED, success, message, room);
            } else {
                gameClient.handleEvent(GameEvent.ROOM_JOINED, success, message, null);
            }
        }
    }

    private void handleChat(String[] parts) {
        if (parts.length >= 3) {
            gameClient.handleEvent(GameEvent.CHAT_RECEIVED, parts[1], parts[2]);
        }
    }

    private void handlePlayerUpdate(String[] parts) {
        if (parts.length >= 3) {
            String roomId = parts[1];
            int playerCount = Integer.parseInt(parts[2]);
            gameClient.handleEvent(GameEvent.PLAYER_UPDATED, roomId, playerCount);
        }
    }

    private void handleSettingsUpdate(String[] parts) {
        if (parts.length >= 4) {
            GameMode gameMode = GameMode.valueOf(parts[2]);
            DifficultyLevel difficulty = DifficultyLevel.valueOf(parts[3]);
            gameClient.handleEvent(GameEvent.SETTINGS_UPDATED, gameMode, difficulty);
        }
    }

    private GameRoom parseRoomInfo(String roomInfoStr) {
        String[] info = roomInfoStr.split(",");
        if (info.length >= 7) {
            GameRoom room = new GameRoom(
                    info[1],
                    GameMode.valueOf(info[4]),
                    DifficultyLevel.valueOf(info[5])
            );
            room.setRoomId(info[0]);
            room.setCurrentPlayers(Integer.parseInt(info[2]));
            room.setMaxPlayers(Integer.parseInt(info[3]));
            room.setHostName(info[6]);
            return room;
        }
        return null;
    }
}