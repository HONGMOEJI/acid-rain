package game;

public class GameRoom {
    private String roomId;
    private String roomName;
    private String password;
    private int currentPlayers;
    private int maxPlayers;
    private GameMode gameMode;
    private DifficultyLevel difficulty;
    private String hostName;

    public GameRoom(String roomName, GameMode gameMode, DifficultyLevel difficulty) {
        this.roomName = roomName;
        this.gameMode = gameMode;
        this.difficulty = difficulty;
        this.currentPlayers = 1;
        this.maxPlayers = 2;  // 기본값 2인용
    }

    // Getters and Setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getCurrentPlayers() { return currentPlayers; }
    public void setCurrentPlayers(int currentPlayers) { this.currentPlayers = currentPlayers; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public GameMode getGameMode() { return gameMode; }
    public void setGameMode(GameMode gameMode) { this.gameMode = gameMode; }

    public DifficultyLevel getDifficulty() { return difficulty; }
    public void setDifficulty(DifficultyLevel difficulty) { this.difficulty = difficulty; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public boolean isPasswordRequired() {
        return password != null && !password.isEmpty();
    }

    public boolean isFull() {
        return currentPlayers >= maxPlayers;
    }

    @Override
    public String toString() {
        return String.format("%s [%d/%d] %s %s %s",
                roomName,
                currentPlayers,
                maxPlayers,
                gameMode,
                difficulty,
                isPasswordRequired() ? "🔒" : "");
    }
}