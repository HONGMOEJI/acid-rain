package server;

import game.model.GameRoom;
import game.model.GameMode;
import game.model.DifficultyLevel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class GameServer {
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, Set<ClientHandler>> roomPlayers = new ConcurrentHashMap<>();
    private boolean running;
    private final int port;
    private int roomIdCounter = 1;

    public GameServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("서버가 시작되었습니다. 포트: " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("새로운 클라이언트 연결: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                new Thread(handler).start();

                broadcastUserCount();
            }
        } catch (IOException e) {
            System.err.println("서버 에러: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            // 모든 클라이언트 연결 종료
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.shutdown();
                }
                clients.clear();
            }

            // 모든 방 정리
            rooms.clear();
            roomPlayers.clear();

            System.out.println("서버가 종료되었습니다.");
        } catch (IOException e) {
            System.err.println("서버 종료 중 에러: " + e.getMessage());
        }
    }

    public int getCurrentClientCount() {
        return clients.size();
    }

    public synchronized void createRoom(String[] roomInfo, ClientHandler creator) {
        try {
            String roomName = roomInfo[1];
            String password = roomInfo[2];
            GameMode gameMode = GameMode.fromDisplayName(roomInfo[3]);
            DifficultyLevel difficulty = DifficultyLevel.fromDisplayName(roomInfo[4]);
            int maxPlayers = Integer.parseInt(roomInfo[5]);

            String roomId = "R" + roomIdCounter++;
            GameRoom room = new GameRoom(roomName, password, gameMode, difficulty, maxPlayers);
            room.setRoomId(roomId);
            room.setPassword(password.isEmpty() ? null : password);
            room.setMaxPlayers(maxPlayers);
            room.setHostName(creator.getUsername());
            room.setCurrentPlayers(1);

            rooms.put(roomId, room);
            Set<ClientHandler> players = Collections.synchronizedSet(new HashSet<>());
            players.add(creator);
            roomPlayers.put(roomId, players);

            String roomInfoStr = formatRoomInfo(room);
            // 방 생성 응답과 함께 자동 입장 처리
            creator.sendMessage("CREATE_ROOM_RESPONSE|true|방이 생성되었습니다.|" + roomInfoStr + "|" + roomId);
            broadcastRoomList();

        } catch (IllegalArgumentException e) {
            creator.sendMessage("CREATE_ROOM_RESPONSE|false|잘못된 설정값입니다: " + e.getMessage());
        } catch (Exception e) {
            creator.sendMessage("CREATE_ROOM_RESPONSE|false|방 생성 실패: " + e.getMessage());
        }
    }

    public synchronized void joinRoom(String roomId, ClientHandler client, String password) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            client.sendMessage("JOIN_ROOM_RESPONSE|false|존재하지 않는 방입니다.");
            return;
        }

        if (room.isPasswordRequired() && !room.isPasswordValid(password)) {
            client.sendMessage("JOIN_ROOM_RESPONSE|false|비밀번호가 일치하지 않습니다.");
            return;
        }

        if (room.isFull()) {
            client.sendMessage("JOIN_ROOM_RESPONSE|false|방이 가득 찼습니다.");
            return;
        }

        Set<ClientHandler> players = roomPlayers.get(roomId);
        players.add(client);
        room.setCurrentPlayers(players.size());

        String roomInfoStr = formatRoomInfo(room);
        client.sendMessage("JOIN_ROOM_RESPONSE|true|방에 입장했습니다.|" + roomInfoStr);

        // 방의 다른 플레이어들에게 새 플레이어 입장 알림
        broadcastToRoom(roomId, "PLAYER_UPDATE|" + roomId + "|" + room.getCurrentPlayers());
        broadcastRoomList();
    }

    public synchronized void leaveRoom(String roomId, ClientHandler client) {
        Set<ClientHandler> players = roomPlayers.get(roomId);
        GameRoom room = rooms.get(roomId);

        if (room != null && players != null && players.remove(client)) {
            room.setCurrentPlayers(players.size());

            if (players.isEmpty()) {
                // 방에 아무도 없으면 방 삭제
                rooms.remove(roomId);
                roomPlayers.remove(roomId);
            } else if (client.getUsername().equals(room.getHostName())) {
                // 방장이 나간 경우 새로운 방장 지정
                ClientHandler newHost = players.iterator().next();
                room.setHostName(newHost.getUsername());
                broadcastToRoom(roomId, "NEW_HOST|" + newHost.getUsername());
            }

            broadcastToRoom(roomId, "PLAYER_UPDATE|" + roomId + "|" + room.getCurrentPlayers());
            broadcastRoomList();
        }
    }

    private String formatRoomInfo(GameRoom room) {
        return String.format("%s,%s,%d,%d,%s,%s,%s",
                room.getRoomId(),
                room.getRoomName(),
                room.getCurrentPlayers(),
                room.getMaxPlayers(),
                room.getGameMode().getDisplayName(), // 수정된 부분
                room.getDifficulty().getDisplayName(), // 수정된 부분
                room.getHostName());
    }

    public void broadcastToRoom(String roomId, String message) {
        Set<ClientHandler> players = roomPlayers.get(roomId);
        if (players != null) {
            synchronized (players) {
                for (ClientHandler player : players) {
                    player.sendMessage(message);
                }
            }
        }
    }

    public void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    public void broadcastRoomList() {
        StringBuilder response = new StringBuilder("ROOM_LIST_RESPONSE");
        for (GameRoom room : rooms.values()) {
            response.append("|").append(formatRoomInfo(room));
        }
        broadcast(response.toString());
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);

        // 방에서도 제거
        for (Map.Entry<String, Set<ClientHandler>> entry : roomPlayers.entrySet()) {
            if (entry.getValue().remove(client)) {
                leaveRoom(entry.getKey(), client);
                break;
            }
        }
        broadcastUserCount();
    }

    public void broadcastUserCount() {
        synchronized (clients) {
            int userCount = clients.size();
            System.out.println("현재 접속자 수 브로드캐스트: " + userCount); // 디버깅 로그 추가
            broadcast("USERS|" + userCount);
        }
    }

    public void handleChat(String roomId, ClientHandler sender, String message) {
        broadcastToRoom(roomId, "CHAT|" + sender.getUsername() + "|" + message);
    }


    public void updateGameSettings(String roomId, String settingType, String newValue, ClientHandler updater) {
        GameRoom room = rooms.get(roomId);
        if (room != null && updater.getUsername().equals(room.getHostName())) {
            try {
                switch (settingType) {
                    case "MODE":
                        room.setGameMode(GameMode.fromDisplayName(newValue)); // 수정된 부분
                        break;
                    case "DIFFICULTY":
                        room.setDifficulty(DifficultyLevel.fromDisplayName(newValue)); // 수정된 부분
                        break;
                }
                broadcastToRoom(roomId, "SETTINGS_UPDATE|" + roomId + "|" +
                        room.getGameMode().getDisplayName() + "|" + room.getDifficulty().getDisplayName()); // 수정된 부분
                broadcastRoomList();
            } catch (IllegalArgumentException e) {
                updater.sendMessage("ERROR|잘못된 설정값입니다.");
            }
        }
    }



    public void startGame(String roomId, ClientHandler starter) {
        GameRoom room = rooms.get(roomId);
        if (room != null && starter.getUsername().equals(room.getHostName()) &&
                room.getCurrentPlayers() == room.getMaxPlayers()) {
            room.setGameStarted(true);
            broadcastToRoom(roomId, "GAME_START");
        }
    }
}
