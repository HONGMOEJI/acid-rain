package client.network;

import client.app.GameClient;
import client.event.GameEvent;
import java.util.Arrays;
import java.util.logging.Logger;

public class MessageHandler {
    private static final Logger logger = Logger.getLogger(MessageHandler.class.getName());
    private final GameClient gameClient;

    public MessageHandler(GameClient gameClient) {
        this.gameClient = gameClient;
    }

    public void handleMessage(String message) {
        try {
            logger.info("수신된 메시지: " + message);
            String[] parts = message.split("\\|");
            String messageType = parts[0];

            switch (messageType) {
                // 사용자 관련
                case "USERS":
                    handleUsers(parts);
                    break;

                // 방 목록 업데이트
                case "ROOM_LIST_RESPONSE":
                    handleRoomList(parts);
                    break;

                // 플레이어 목록 응답
                case "PLAYER_LIST_RESPONSE":
                    handlePlayerList(parts);
                    break;

                // 방 생성 응답
                case "CREATE_ROOM_RESPONSE":
                    handleCreateRoom(parts);
                    break;

                // 방 입장 응답
                case "JOIN_ROOM_RESPONSE":
                    handleJoinRoom(parts);
                    break;

                // 채팅
                case "CHAT":
                    handleChat(parts);
                    break;

                // 플레이어 업데이트
                case "PLAYER_UPDATE":
                    handlePlayerUpdate(parts);
                    break;

                // 설정 업데이트
                case "SETTINGS_UPDATE":
                    handleSettingsUpdate(parts);
                    break;

                // 게임 시작
                case "GAME_START":
                    handleGameStart();
                    break;

                // 방장 퇴장
                case "HOST_LEFT":
                    handleHostLeft(parts);
                    break;

                // 방 닫힘
                case "ROOM_CLOSED":
                    handleRoomClosed(parts);
                    break;

                // 새 방장
                case "NEW_HOST":
                    handleNewHost(parts);
                    break;

                // 에러
                case "ERROR":
                    handleError(parts);
                    break;

                // 게임 관련 메시지
                case "WORD_SPAWNED":
                    handleWordSpawned(parts);
                    break;

                case "WORD_MATCHED":
                    handleWordMatched(parts);
                    break;

                case "WORD_MISSED":
                    handleWordMissed(parts);
                    break;

                case "BLIND_EFFECT":
                    handleBlindEffect(parts);
                    break;

                case "GAME_OVER":
                    handleGameOver(parts);
                    break;

                default:
                    logger.warning("알 수 없는 메시지 타입: " + messageType);
                    break;
            }
        } catch (Exception e) {
            logger.severe("메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUsers(String[] parts) {
        if (parts.length >= 2) {
            try {
                int userCount = Integer.parseInt(parts[1]);
                gameClient.handleEvent(GameEvent.USERS_UPDATED, userCount);
            } catch (NumberFormatException e) {
                logger.severe("사용자 수 파싱 오류: " + parts[1]);
            }
        }
    }

    private void handleRoomList(String[] parts) {
        if (parts.length > 1) {
            String[] roomInfos = Arrays.copyOfRange(parts, 1, parts.length);
            gameClient.handleEvent(GameEvent.ROOM_LIST_UPDATED, (Object[]) roomInfos);
        } else {
            gameClient.handleEvent(GameEvent.ROOM_LIST_UPDATED, new String[0]);
        }
    }

    private void handlePlayerList(String[] parts) {
        if (parts.length >= 4) {
            String roomId = parts[1];
            try {
                int playerCount = Integer.parseInt(parts[2]);
                String[] players = parts[3].split(";");
                gameClient.handleEvent(GameEvent.PLAYER_UPDATED, roomId, playerCount, players);
            } catch (NumberFormatException e) {
                logger.severe("플레이어 수 파싱 오류: " + parts[2]);
            }
        }
    }

    private void handleCreateRoom(String[] parts) {
        if (parts.length >= 3) {
            boolean success = Boolean.parseBoolean(parts[1]);
            String msg = parts[2];
            if (success && parts.length >= 5) {
                String roomInfoStr = parts[3];
                String createdRoomId = parts[4];
                gameClient.handleEvent(GameEvent.ROOM_CREATED, success, msg, roomInfoStr, createdRoomId);
                gameClient.handleEvent(GameEvent.ROOM_JOINED, true, "방에 입장했습니다.", roomInfoStr, createdRoomId);
            } else {
                gameClient.handleEvent(GameEvent.ROOM_CREATED, success, msg, null, null);
            }
        }
    }

    private void handleJoinRoom(String[] parts) {
        if (parts.length >= 3) {
            boolean success = Boolean.parseBoolean(parts[1]);
            String joinMsg = parts[2];
            if (success && parts.length >= 4) {
                String roomInfoStr = parts[3];
                gameClient.handleEvent(GameEvent.ROOM_JOINED, success, joinMsg, roomInfoStr);
            } else {
                gameClient.handleEvent(GameEvent.ROOM_JOINED, success, joinMsg, null);
            }
        }
    }

    private void handleChat(String[] parts) {
        if (parts.length >= 3) {
            String username = parts[1];
            String chatMsg = parts[2];
            gameClient.handleEvent(GameEvent.CHAT_RECEIVED, username, chatMsg);
        }
    }

    private void handlePlayerUpdate(String[] parts) {
        if (parts.length >= 4) {
            String roomId = parts[1];
            try {
                int playerCount = Integer.parseInt(parts[2]);
                String[] players = parts[3].split(";");
                gameClient.handleEvent(GameEvent.PLAYER_UPDATED, roomId, playerCount, players);
            } catch (NumberFormatException e) {
                logger.severe("플레이어 수 파싱 오류: " + parts[2]);
            }
        }
    }

    private void handleSettingsUpdate(String[] parts) {
        if (parts.length >= 4) {
            String roomId = parts[1];
            String mode = parts[2];
            String diff = parts[3];
            gameClient.handleEvent(GameEvent.SETTINGS_UPDATED, roomId, mode, diff);
        }
    }

    private void handleGameStart() {
        gameClient.handleEvent(GameEvent.GAME_STARTED);
    }

    private void handleHostLeft(String[] parts) {
        if (parts.length >= 3) {
            String roomId = parts[1];
            String hostMsg = parts[2];
            gameClient.handleEvent(GameEvent.HOST_LEFT, roomId, hostMsg);
        }
    }

    private void handleRoomClosed(String[] parts) {
        if (parts.length >= 3) {
            String roomId = parts[1];
            String reason = parts[2];
            gameClient.handleEvent(GameEvent.ROOM_CLOSED, roomId, reason);
        }
    }

    private void handleNewHost(String[] parts) {
        if (parts.length >= 3) {
            String roomId = parts[1];
            String newHostName = parts[2];
            gameClient.handleEvent(GameEvent.NEW_HOST, roomId, newHostName);
        }
    }

    private void handleError(String[] parts) {
        if (parts.length >= 2) {
            String errorMessage = parts[1];
            gameClient.handleEvent(GameEvent.ERROR_OCCURRED, errorMessage);
        }
    }

    private void handleWordSpawned(String[] parts) {
        if (parts.length >= 4) {
            String wordText = parts[2];
            try {
                int xPos = Integer.parseInt(parts[3]);
                gameClient.handleEvent("WORD_SPAWNED", wordText, xPos);
            } catch (NumberFormatException e) {
                logger.severe("단어 위치 파싱 오류: " + parts[3]);
            }
        }
    }

    private void handleWordMatched(String[] parts) {
        if (parts.length >= 5) {
            String wordText = parts[2];
            String playerName = parts[3];
            try {
                int newScore = Integer.parseInt(parts[4]);
                gameClient.handleEvent("WORD_MATCHED", wordText, playerName, newScore);
            } catch (NumberFormatException e) {
                logger.severe("점수 파싱 오류: " + parts[4]);
            }
        }
    }

    private void handleWordMissed(String[] parts) {
        if (parts.length >= 5) {
            String missedWord = parts[2];
            String playerName = parts[3];
            try {
                double newPH = Double.parseDouble(parts[4]);
                gameClient.handleEvent("WORD_MISSED", missedWord, playerName, newPH);
            } catch (NumberFormatException e) {
                logger.severe("PH 값 파싱 오류: " + parts[4]);
            }
        }
    }

    private void handleBlindEffect(String[] parts) {
        if (parts.length >= 4) {
            String targetPlayer = parts[2];
            try {
                int durationMs = Integer.parseInt(parts[3]);
                gameClient.handleEvent("BLIND_EFFECT", targetPlayer, durationMs);
            } catch (NumberFormatException e) {
                logger.severe("효과 지속시간 파싱 오류: " + parts[3]);
            }
        }
    }

    private void handleGameOver(String[] parts) {
        if (parts.length >= 5) {
            String winnerName = parts[2];
            try {
                int myScore = Integer.parseInt(parts[3]);
                int opponentScore = Integer.parseInt(parts[4]);
                gameClient.handleEvent("GAME_OVER", winnerName, myScore, opponentScore);
            } catch (NumberFormatException e) {
                logger.severe("점수 파싱 오류: " + Arrays.toString(parts));
            }
        }
    }
}