// game/model/GameRoom.java
package game.model;

public class GameRoom {
    private String roomId;
    private String roomName;
    private String hostName;
    private String password;
    private int maxPlayers;
    private int currentPlayers;
    private GameMode gameMode;
    private DifficultyLevel difficulty;
    private boolean gameStarted;
    private boolean passwordRequired;

    public GameRoom(String roomName, GameMode gameMode, DifficultyLevel difficulty) {
        this.roomName = roomName;
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.maxPlayers = 2; // 기본값 설정
        this.currentPlayers = 0;
        this.gameStarted = false;
    }

    // 방 상태 관련 메서드
    public boolean isFull() {
        return currentPlayers >= maxPlayers;
    }

    public boolean canStart() {
        return currentPlayers == maxPlayers && !gameStarted;
    }

    public boolean isPasswordValid(String inputPassword) {
        if (!passwordRequired) return true;
        return password != null && password.equals(inputPassword);
    }

    // 플레이어 관리
    public boolean addPlayer() {
        if (isFull()) return false;
        currentPlayers++;
        return true;
    }

    public boolean removePlayer() {
        if (currentPlayers > 0) {
            currentPlayers--;
            return true;
        }
        return false;
    }

    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        this.passwordRequired = password != null && !password.isEmpty();
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public boolean isPasswordRequired() {
        return passwordRequired;
    }

    // 게임 설정 변경 검증 메서드
    public boolean canModifySettings(String username) {
        return hostName != null && hostName.equals(username) && !gameStarted;
    }

    @Override
    public String toString() {
        return String.format("GameRoom[id=%s, name=%s, players=%d/%d, mode=%s, difficulty=%s]",
                roomId, roomName, currentPlayers, maxPlayers, gameMode, difficulty);
    }
}