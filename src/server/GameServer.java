package server;

import game.GameRoom;
import game.GameMode;
import game.DifficultyLevel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private boolean running;
    private final int port;
    private Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private Map<String, Set<ClientHandler>> roomPlayers = new ConcurrentHashMap<>();  // 방별 플레이어 관리
    private int roomIdCounter;

    public GameServer(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        this.roomIdCounter = 1;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("서버가 시작되었습니다. 포트: " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                new Thread(handler).start();

                // 접속자 수 업데이트 브로드캐스트
                broadcastUserCount();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        broadcastUserCount();
    }

    public void broadcastUserCount() {
        broadcast("USERS|" + clients.size());
    }

    // 모든 클라이언트에게 메시지 전송
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // 방 생성 처리
    public synchronized void createRoom(String[] roomInfo, ClientHandler creator) {
        try {
            // 배열 크기 검증
            if (roomInfo.length < 6) {
                creator.sendMessage("CREATE_ROOM_RESPONSE|false|방 정보가 부족합니다.");
                System.out.println("방 생성 실패: 정보 부족");
                return;
            }

            String roomName = roomInfo[1];
            String password = roomInfo[2];
            GameMode gameMode = GameMode.valueOf(roomInfo[3]);
            DifficultyLevel difficulty = DifficultyLevel.valueOf(roomInfo[4]);
            int maxPlayers = Integer.parseInt(roomInfo[5]);

            if (roomName.isEmpty()) {
                creator.sendMessage("CREATE_ROOM_RESPONSE|false|방 이름은 필수입니다.");
                System.out.println("방 생성 실패: 방 이름 없음");
                return;
            }

            if (maxPlayers < 1) {
                creator.sendMessage("CREATE_ROOM_RESPONSE|false|최대 플레이어 수는 1 이상이어야 합니다.");
                System.out.println("방 생성 실패: 최대 플레이어 수 설정 오류");
                return;
            }

            String roomId = "R" + roomIdCounter++;
            GameRoom room = new GameRoom(roomName, gameMode, difficulty);
            room.setRoomId(roomId);
            room.setPassword(password.isEmpty() ? null : password);
            room.setMaxPlayers(maxPlayers);
            room.setHostName(creator.getUsername());
            room.setCurrentPlayers(1); // 방장은 자동 참가

            rooms.put(roomId, room);
            roomPlayers.put(roomId, new HashSet<>());
            roomPlayers.get(roomId).add(creator);

            creator.sendMessage("CREATE_ROOM_RESPONSE|true|방이 생성되었습니다.");
            broadcastRoomList();

            System.out.println("방 생성 성공: " + room);
        } catch (IllegalArgumentException e) {
            creator.sendMessage("CREATE_ROOM_RESPONSE|false|게임 모드 또는 난이도가 잘못되었습니다.");
            System.out.println("방 생성 실패: 잘못된 게임 모드 또는 난이도");
        } catch (Exception e) {
            creator.sendMessage("CREATE_ROOM_RESPONSE|false|방 생성에 실패했습니다.");
            System.out.println("방 생성 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 방 입장 처리
    public void joinRoom(String roomId, ClientHandler client) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            client.sendMessage("JOIN_ROOM_RESPONSE|false|존재하지 않는 방입니다.");
            return;
        }

        if (room.isFull()) {
            client.sendMessage("JOIN_ROOM_RESPONSE|false|방이 가득 찼습니다.");
            return;
        }

        if (room.isPasswordRequired()) {
            client.sendMessage("JOIN_ROOM_RESPONSE|false|비밀번호가 필요한 방입니다.");
            return;
        }

        Set<ClientHandler> players = roomPlayers.get(roomId);
        players.add(client);
        room.setCurrentPlayers(players.size());

        client.sendMessage("JOIN_ROOM_RESPONSE|true|방에 입장했습니다.");
        broadcastRoomList();
    }

    // 방 나가기 처리
    public void leaveRoom(String roomId, ClientHandler client) {
        GameRoom room = rooms.get(roomId);
        Set<ClientHandler> players = roomPlayers.get(roomId);

        if (room != null && players != null) {
            players.remove(client);
            room.setCurrentPlayers(players.size());

            if (players.isEmpty()) {
                rooms.remove(roomId);
                roomPlayers.remove(roomId);
            }

            broadcastRoomList();
        }
    }

    // 방 목록 전송
    public void sendRoomList(ClientHandler requester) {
        StringBuilder response = new StringBuilder("ROOM_LIST_RESPONSE");

        for (GameRoom room : rooms.values()) {
            response.append("|")
                    .append(room.getRoomId()).append(",")
                    .append(room.getRoomName()).append(",")
                    .append(room.getCurrentPlayers()).append(",")
                    .append(room.getMaxPlayers()).append(",")
                    .append(room.getGameMode()).append(",")
                    .append(room.getDifficulty());
        }

        requester.sendMessage(response.toString());
    }

    // 모든 클라이언트에게 방 목록 업데이트 전송
    public void broadcastRoomList() {
        StringBuilder response = new StringBuilder("ROOM_LIST_RESPONSE");

        for (GameRoom room : rooms.values()) {
            response.append("|")
                    .append(room.getRoomId()).append(",")
                    .append(room.getRoomName()).append(",")
                    .append(room.getCurrentPlayers()).append(",")
                    .append(room.getMaxPlayers()).append(",")
                    .append(room.getGameMode()).append(",")
                    .append(room.getDifficulty());
        }

        // 디버깅 로그 추가
        System.out.println("Broadcasting room list: " + response);
        broadcast(response.toString());
    }
}