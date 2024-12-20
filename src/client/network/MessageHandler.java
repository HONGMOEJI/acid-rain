/*
 * client.network.MessageHandler.java
 * 서버로부터 수신된 메시지를 처리하는 클래스
 */

package client.network;

import client.app.GameClient;
import client.event.GameEvent;
import game.model.Word;

import java.util.Arrays;
import java.util.logging.Logger;

public class MessageHandler {
    // 디버깅을 위해 Logger 사용, 개발과정에서 디버그가 필요한 부분은 Log를 남기도록 함.
    private static final Logger logger = Logger.getLogger(MessageHandler.class.getName());

    private final GameClient gameClient;

    public MessageHandler(GameClient gameClient) {
        this.gameClient = gameClient;
    }

    // 서버로부터 수신된 메시지를 처리하는 메서드
    public void handleMessage(String message) {
        try {
            logger.info("수신된 메시지: " + message);
            String[] parts = message.split("\\|");
            String messageType = parts[0];

            switch (messageType) {
                // 유저 관련 메시지
                case "USERS" -> handleUsers(parts);

                // 방 관련 메시지
                case "ROOM_LIST_RESPONSE" -> handleRoomList(parts);
                case "PLAYER_LIST_RESPONSE" -> handlePlayerList(parts);
                case "CREATE_ROOM_RESPONSE" -> handleCreateRoom(parts);
                case "JOIN_ROOM_RESPONSE" -> handleJoinRoom(parts);
                case "ROOM_CLOSED" -> handleRoomClosed(parts);
                case "HOST_LEFT" -> handleHostLeft(parts);
                case "NEW_HOST" -> handleNewHost(parts);

                // 채팅 메시지
                case "CHAT" -> handleChat(parts);

                // 게임 상태 메시지
                case "PLAYER_UPDATE" -> handlePlayerUpdate(parts);
                case "SETTINGS_UPDATE" -> handleSettingsUpdate(parts);
                case "GAME_START" -> handleGameStart();

                // 게임 플레이 메시지
                case "WORD_SPAWNED" -> handleWordSpawned(parts);
                case "WORD_MATCHED" -> handleWordMatched(parts);
                case "WORD_MISSED" -> handleWordMissed(parts);
                case "BLIND_EFFECT" -> handleBlindEffect(parts);
                case "GAME_OVER" -> handleGameOver(parts);

                case "PH_UPDATE" -> handlePHUpdate(parts);

                // 리더보드 관련 메시지
                case "LEADERBOARD_DATA" -> handleLeaderboardData(parts);
                case "LEADERBOARD_UPDATE" -> handleLeaderboardUpdate(parts);
                case "MY_RECORDS_DATA" -> handleMyRecordsData(parts);

                // 에러 메시지
                case "ERROR" -> handleError(parts);

                default -> logger.warning("알 수 없는 메시지 타입: " + messageType);
            }
        } catch (Exception e) {
            logger.severe("메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 유저 관련 핸들러
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

    // 방 관련 핸들러
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

    // 게임 상태 핸들러
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

    // 게임 플레이 핸들러
    private void handleWordSpawned(String[] parts) {
        if (parts.length >= 4) {
            String wordText = parts[2];
            try {
                int xPos = Integer.parseInt(parts[3]);
                if (parts.length >= 5) {
                    // 특수 효과가 있는 경우
                    Word.SpecialEffect effect = Word.SpecialEffect.valueOf(parts[4]);
                    gameClient.handleEvent("WORD_SPAWNED", wordText, xPos, effect);
                } else {
                    // 일반 단어인 경우
                    gameClient.handleEvent("WORD_SPAWNED", wordText, xPos);
                }
            } catch (NumberFormatException e) {
                logger.severe("단어 위치 파싱 오류: " + parts[3]);
            } catch (IllegalArgumentException e) {
                logger.severe("특수 효과 파싱 오류: " + parts[4]);
            }
        }
    }

    private void handlePHUpdate(String[] parts) {
        if (parts.length >= 4) {
            String roomId = parts[1];
            String playerName = parts[2];
            try {
                double newPH = Double.parseDouble(parts[3]);
                gameClient.handleEvent("PH_UPDATE", playerName, newPH);
            } catch (NumberFormatException e) {
                logger.severe("pH 값 파싱 오류: " + parts[3]);
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
                logger.severe("pH 값 파싱 오류: " + parts[4]);
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
            String roomId = parts[1];
            String winnerName = parts[2];
            try {
                int myScore = Integer.parseInt(parts[3]);
                int opponentScore = Integer.parseInt(parts[4]);
                boolean isForfeit = parts.length >= 6 && "FORFEIT".equals(parts[5]);

                gameClient.handleEvent("GAME_OVER", winnerName, myScore, opponentScore, isForfeit);
            } catch (NumberFormatException e) {
                logger.severe("점수 파싱 오류: " + Arrays.toString(parts));
            }
        }
    }

    // 리더보드 관련 핸들러
    private void handleLeaderboardData(String[] parts) {
        if (parts.length >= 3) {
            String type = parts[1];
            String[] entries = Arrays.copyOfRange(parts, 2, parts.length);
            gameClient.handleEvent("TOP_SCORES", (Object) entries);
        }
    }

    private void handleLeaderboardUpdate(String[] parts) {
        if (parts.length >= 4) {
            String roomId = parts[1];
            String playerName = parts[2];
            try {
                int rank = Integer.parseInt(parts[3]);
                gameClient.handleEvent("LEADERBOARD_UPDATE", roomId, playerName, rank);
            } catch (NumberFormatException e) {
                logger.severe("리더보드 순위 파싱 오류: " + parts[3]);
            }
        }
    }

    private void handleMyRecordsData(String[] parts) {
        if (parts.length >= 2) {
            String[] records = Arrays.copyOfRange(parts, 1, parts.length);
            gameClient.handleEvent("USER_RECORDS", (Object) records);
        }
    }

    // 기타 핸들러
    private void handleChat(String[] parts) {
        if (parts.length >= 3) {
            String username = parts[1];
            String chatMsg = parts[2];
            gameClient.handleEvent(GameEvent.CHAT_RECEIVED, username, chatMsg);
        }
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
}