// server/ClientHandler.java
package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String currentRoomId;
    private boolean running = true;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("클라이언트 핸들러 생성 중 에러: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                processMessage(message);
            }
        } catch (IOException e) {
            System.out.println("클라이언트 연결 종료: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split("\\|");
        String messageType = parts[0];

        try {
            switch (messageType) {
                case "LOGIN":
                    handleLogin(parts);
                    break;
                case "CREATE_ROOM":
                    server.createRoom(parts, this);
                    break;
                case "JOIN_ROOM":
                    handleJoinRoom(parts);
                    break;
                case "LEAVE_ROOM":
                    handleLeaveRoom(parts);
                    break;
                case "CHAT":
                    handleChat(parts);
                    break;
                case "UPDATE_SETTINGS":
                    handleSettingsUpdate(parts);
                    break;
                case "START_GAME":
                    handleGameStart(parts);
                    break;
                case "ROOM_LIST":
                    server.broadcastRoomList();
                    break;
            }
        } catch (Exception e) {
            System.err.println("메시지 처리 중 에러: " + e.getMessage());
            sendMessage("ERROR|" + e.getMessage());
        }
    }

    private void handleLogin(String[] parts) {
        if (parts.length >= 2) {
            this.username = parts[1];
            System.out.println("새로운 사용자 로그인: " + username);
            server.broadcastUserCount();
        }
    }

    private void handleJoinRoom(String[] parts) {
        if (parts.length >= 3) {
            String roomId = parts[1];
            String password = parts.length >= 4 ? parts[3] : "";
            server.joinRoom(roomId, this, password);
            this.currentRoomId = roomId;
        }
    }

    private void handleLeaveRoom(String[] parts) {
        if (currentRoomId != null) {
            server.leaveRoom(currentRoomId, this);
            currentRoomId = null;
        }
    }

    private void handleChat(String[] parts) {
        if (parts.length >= 3 && currentRoomId != null) {
            server.handleChat(currentRoomId, this, parts[2]);
        }
    }

    private void handleSettingsUpdate(String[] parts) {
        if (parts.length >= 4 && currentRoomId != null) {
            server.updateGameSettings(currentRoomId, parts[2], parts[3], this);
        }
    }

    private void handleGameStart(String[] parts) {
        if (currentRoomId != null) {
            server.startGame(currentRoomId, this);
        }
    }

    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("클라이언트 핸들러 종료 중 에러: " + e.getMessage());
        }
    }

    private void cleanup() {
        if (currentRoomId != null) {
            server.leaveRoom(currentRoomId, this);
        }
        server.removeClient(this);
        shutdown();
    }

    public String getUsername() {
        return username;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }
}