package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private GameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String currentRoomId;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
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

        switch (messageType) {
            case "LOGIN":
                this.username = parts[1];
                server.broadcastUserCount();
                break;

            case "CREATE_ROOM":
                server.createRoom(parts, this);
                break;

            case "JOIN_ROOM":
                String joinRoomId = parts[1];
                server.joinRoom(joinRoomId, this);
                currentRoomId = joinRoomId;
                break;

            case "ROOM_LIST":
                server.sendRoomList(this);
                break;

            case "LEAVE_ROOM":
                if (currentRoomId != null) {
                    server.leaveRoom(currentRoomId, this);
                    currentRoomId = null;
                }
                break;
        }
    }

    private void cleanup() {
        if (currentRoomId != null) {
            server.leaveRoom(currentRoomId, this);
        }
        server.removeClient(this);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getUsername() {
        return username;
    }
}