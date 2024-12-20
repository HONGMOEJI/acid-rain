/*
 * client.event.GameEvent.java
 * 게임 내에서 발생하는 이벤트 상수를 정의하는 클래스, 일관성을 유지하기 위해 사용,
 * 현재 코드에서는 좀 로직이 꼬여 있어서, 리팩토링 필요가 있는 상태
 */

package client.event;

public class GameEvent {

    /*
     * ============================
     * =       방 관련 이벤트      =
     * ============================
     */
    public static final String ROOM_CREATED = "ROOM_CREATED";
    public static final String ROOM_JOINED = "ROOM_JOINED";
    public static final String ROOM_LEFT = "ROOM_LEFT";
    public static final String ROOM_LIST_UPDATED = "ROOM_LIST_UPDATED";
    public static final String ROOM_CLOSED = "ROOM_CLOSED";
    public static final String HOST_LEFT = "HOST_LEFT";
    public static final String NEW_HOST = "NEW_HOST";

    /*
     * ============================
     * =       게임 상태 이벤트    =
     * ============================
     */
    public static final String GAME_STARTED = "GAME_STARTED";
    public static final String GAME_ENDED = "GAME_ENDED";
    public static final String GAME_PAUSED = "GAME_PAUSED";
    public static final String GAME_RESUMED = "GAME_RESUMED";
    public static final String GAME_ACTION = "GAME_ACTION";

    /*
     * ============================
     * =      플레이어 관련 이벤트  =
     * ============================
     */
    public static final String PLAYER_JOINED = "PLAYER_JOINED";
    public static final String PLAYER_LEFT = "PLAYER_LEFT";
    public static final String PLAYER_UPDATED = "PLAYER_UPDATED";
    public static final String USERS_UPDATED = "USERS_UPDATED";

    /*
     * ============================
     * =       채팅 관련 이벤트     =
     * ============================
     */
    public static final String CHAT_RECEIVED = "CHAT_RECEIVED";
    public static final String CHAT_SENT = "CHAT_SENT";

    /*
     * ============================
     * =     설정(세팅) 관련 이벤트 =
     * ============================
     */
    public static final String SETTINGS_UPDATED = "SETTINGS_UPDATED";

    /*
     * ============================
     * =      오류 및 연결 관련     =
     * ============================
     */
    public static final String ERROR_OCCURRED = "ERROR_OCCURRED";
    public static final String CONNECTION_LOST = "CONNECTION_LOST";

    /*
     * ============================
     * =    리더보드 관련 이벤트    =
     * ============================
     */
    public static final String LEADERBOARD_TOP_SCORES = "LEADERBOARD_TOP_SCORES";
    public static final String LEADERBOARD_USER_RECORDS = "LEADERBOARD_USER_RECORDS";
    public static final String LEADERBOARD_ERROR = "LEADERBOARD_ERROR";
    public static final String LEADERBOARD_RANK_UPDATED = "LEADERBOARD_RANK_UPDATED";


    /*
     * ============================
     * =       서버 메시지 타입     =
     * ============================
     * 서버로부터 수신되는 메시지 타입 상수들
     */
    public static final String MESSAGE_USERS = "USERS";
    public static final String MESSAGE_ROOM_LIST_RESPONSE = "ROOM_LIST_RESPONSE";
    public static final String MESSAGE_PLAYER_LIST_RESPONSE = "PLAYER_LIST_RESPONSE";
    public static final String MESSAGE_CREATE_ROOM_RESPONSE = "CREATE_ROOM_RESPONSE";
    public static final String MESSAGE_JOIN_ROOM_RESPONSE = "JOIN_ROOM_RESPONSE";
    public static final String MESSAGE_ROOM_CLOSED = "ROOM_CLOSED";
    public static final String MESSAGE_HOST_LEFT = "HOST_LEFT";
    public static final String MESSAGE_NEW_HOST = "NEW_HOST";

    // 채팅 메시지
    public static final String MESSAGE_CHAT = "CHAT";

    // 게임 상태 및 설정 관련
    public static final String MESSAGE_PLAYER_UPDATE = "PLAYER_UPDATE";
    public static final String MESSAGE_SETTINGS_UPDATE = "SETTINGS_UPDATE";
    public static final String MESSAGE_GAME_START = "GAME_START";

    // 게임 플레이 관련
    public static final String MESSAGE_WORD_SPAWNED = "WORD_SPAWNED";
    public static final String MESSAGE_WORD_MATCHED = "WORD_MATCHED";
    public static final String MESSAGE_WORD_MISSED = "WORD_MISSED";
    public static final String MESSAGE_BLIND_EFFECT = "BLIND_EFFECT";
    public static final String MESSAGE_GAME_OVER = "GAME_OVER";
    public static final String MESSAGE_PH_UPDATE = "PH_UPDATE";

    // 리더보드 관련
    public static final String MESSAGE_LEADERBOARD_DATA = "LEADERBOARD_DATA";
    public static final String MESSAGE_LEADERBOARD_UPDATE = "LEADERBOARD_UPDATE";
    public static final String MESSAGE_MY_RECORDS_DATA = "MY_RECORDS_DATA";

    // 비정상 종료시 접속자 수 업데이트를 위한 요청 정의, but -> 현재 기존 로직으로 구현이 가능할텐데,,
    // 일단은 로직을 재사용하기가 여의치 않아서, 새로 정의하여 적용, 단, 추후 리팩토링 필수.
    public static final String CMD_USERS_REQUEST = "USERS_REQUEST";


    // 에러 메시지
    public static final String MESSAGE_ERROR = "ERROR";
}
