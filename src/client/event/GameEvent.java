// client/event/GameEvent.java
package client.event;

public class GameEvent {
    // 방 관련 이벤트
    public static final String ROOM_CREATED = "ROOM_CREATED";
    public static final String ROOM_JOINED = "ROOM_JOINED";
    public static final String ROOM_LEFT = "ROOM_LEFT";
    public static final String ROOM_LIST_UPDATED = "ROOM_LIST_UPDATED";

    // 게임 상태 이벤트
    public static final String GAME_STARTED = "GAME_STARTED";
    public static final String GAME_ENDED = "GAME_ENDED";
    public static final String GAME_PAUSED = "GAME_PAUSED";
    public static final String GAME_RESUMED = "GAME_RESUMED";

    // 플레이어 관련 이벤트
    public static final String PLAYER_JOINED = "PLAYER_JOINED";
    public static final String PLAYER_LEFT = "PLAYER_LEFT";
    public static final String PLAYER_UPDATED = "PLAYER_UPDATED";
    public static final String USERS_UPDATED = "USERS_UPDATED";

    // 채팅 이벤트
    public static final String CHAT_RECEIVED = "CHAT_RECEIVED";
    public static final String CHAT_SENT = "CHAT_SENT";

    // 설정 관련 이벤트
    public static final String SETTINGS_UPDATED = "SETTINGS_UPDATED";

    // 오류 이벤트
    public static final String ERROR_OCCURRED = "ERROR_OCCURRED";
    public static final String CONNECTION_LOST = "CONNECTION_LOST";
}