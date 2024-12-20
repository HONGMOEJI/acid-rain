/*
 * client.event.GameEvent.java
 * 게임 내에서 발생하는 이벤트를 정의하는 클래스, 모든 이벤트가 이용되진 않지만, 추후 확장을 위해 작성됨
 */
package client.event;

public class GameEvent {
    // 방 관련 이벤트
    public static final String ROOM_CREATED = "ROOM_CREATED";
    public static final String ROOM_JOINED = "ROOM_JOINED";
    public static final String ROOM_LEFT = "ROOM_LEFT";
    public static final String ROOM_LIST_UPDATED = "ROOM_LIST_UPDATED";
    public static final String ROOM_CLOSED = "ROOM_CLOSED";

    // 게임 상태 이벤트
    public static final String GAME_STARTED = "GAME_STARTED";
    public static final String GAME_ENDED = "GAME_ENDED";
    public static final String GAME_PAUSED = "GAME_PAUSED";
    public static final String GAME_RESUMED = "GAME_RESUMED";
    public static final String GAME_ACTION = "GAME_ACTION";

    // 플레이어 관련 이벤트
    public static final String PLAYER_JOINED = "PLAYER_JOINED";
    public static final String PLAYER_LEFT = "PLAYER_LEFT";
    public static final String PLAYER_UPDATED = "PLAYER_UPDATED";
    public static final String USERS_UPDATED = "USERS_UPDATED";
    public static final String HOST_LEFT = "HOST_LEFT";
    public static final String NEW_HOST = "NEW_HOST";

    // 채팅 이벤트
    public static final String CHAT_RECEIVED = "CHAT_RECEIVED";
    public static final String CHAT_SENT = "CHAT_SENT";

    // 설정 관련 이벤트
    public static final String SETTINGS_UPDATED = "SETTINGS_UPDATED";

    // 오류 이벤트
    public static final String ERROR_OCCURRED = "ERROR_OCCURRED";
    public static final String CONNECTION_LOST = "CONNECTION_LOST";

    // 리더보드 관련 이벤트
    public static final String LEADERBOARD_UPDATE = "LEADERBOARD_UPDATE";
    public static final String TOP_SCORES = "TOP_SCORES";
    public static final String USER_RECORDS = "USER_RECORDS";
}
