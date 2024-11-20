package client;

import client.components.RoomListDialog;
import game.GameRoom;
import game.GameMode;
import game.DifficultyLevel;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private MainMenu mainMenu;
    private final String username;
    private int pendingUserCount = 0;
    private RoomListDialog roomListDialog;

    public GameClient(String host, int port, String username) {
        this.username = username;
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 서버에 사용자 정보 전송
            out.println("LOGIN:" + username);

            // 메시지 수신 스레드 시작
            startReceiving();
        } catch (IOException e) {
            throw new RuntimeException("서버 연결 실패: " + e.getMessage());
        }
    }

    private void startReceiving() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    processMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processMessage(String message) {
        String[] parts = message.split("\\|");
        String messageType = parts[0];

        switch (messageType) {
            case "USERS":
                int count = Integer.parseInt(parts[1]);
                if (mainMenu != null) {
                    SwingUtilities.invokeLater(() -> mainMenu.updateConnectedUsers(count));
                } else {
                    pendingUserCount = count;
                }
                break;

            case "ROOM_LIST_RESPONSE":
                if (parts.length > 1 && roomListDialog != null) {
                    List<GameRoom> rooms = new ArrayList<>();
                    for (int i = 1; i < parts.length; i++) {
                        String[] roomInfo = parts[i].split(",");
                        if (roomInfo.length >= 6) {
                            try {
                                GameRoom room = new GameRoom(
                                        roomInfo[1],
                                        GameMode.valueOf(roomInfo[4]),
                                        DifficultyLevel.valueOf(roomInfo[5])
                                );
                                room.setRoomId(roomInfo[0]);
                                room.setCurrentPlayers(Integer.parseInt(roomInfo[2]));
                                room.setMaxPlayers(Integer.parseInt(roomInfo[3]));
                                rooms.add(room);
                            } catch (Exception e) {
                                System.out.println("Error parsing room info: " + Arrays.toString(roomInfo));
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("Invalid room info: " + Arrays.toString(roomInfo));
                        }
                    }
                    roomListDialog.updateRoomList(rooms);
                    System.out.println("Updated room list with " + rooms.size() + " rooms.");
                } else {
                    System.out.println("No rooms to update or roomListDialog is null.");
                }
                break;

            case "CREATE_ROOM_RESPONSE":
                if (parts.length >= 3 && roomListDialog != null) {
                    boolean success = Boolean.parseBoolean(parts[1]);
                    roomListDialog.handleCreateRoomResponse(success, parts[2]);
                }
                break;

            case "JOIN_ROOM_RESPONSE":
                if (parts.length >= 3 && roomListDialog != null) {
                    boolean success = Boolean.parseBoolean(parts[1]);
                    if (success) {
                        roomListDialog.dispose();
                        // TODO: 게임 화면으로 전환
                    } else {
                        roomListDialog.handleJoinRoomResponse(false, parts[2]);
                    }
                }
                break;

            case "ROOM_UPDATE":
                sendRoomListRequest();
                break;
        }
    }

    public void sendCreateRoomRequest(GameRoom room) {
        String roomInfo = String.format("CREATE_ROOM|%s|%s|%s|%s|%d",
                room.getRoomName(),
                room.getPassword() != null ? room.getPassword() : "",
                room.getGameMode(),
                room.getDifficulty(),
                room.getMaxPlayers()
        );
        System.out.println("Sending create room request: " + roomInfo);
        sendMessage(roomInfo);
    }

    public void sendJoinRoomRequest(String roomId) {
        sendMessage("JOIN_ROOM|" + roomId + "|" + username);
    }

    public void sendRoomListRequest() {
        sendMessage("ROOM_LIST");
    }

    public void setMainMenu(MainMenu mainMenu) {
        this.mainMenu = mainMenu;
        if (pendingUserCount > 0) {
            final int count = pendingUserCount;
            SwingUtilities.invokeLater(() -> mainMenu.updateConnectedUsers(count));
        }
    }

    public void setRoomListDialog(RoomListDialog dialog) {
        this.roomListDialog = dialog;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getUsername() {
        return username;
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null,
                        message,
                        "오류",
                        JOptionPane.ERROR_MESSAGE)
        );
    }
}