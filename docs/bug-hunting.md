# Bug Hunting Report

## 기준

- `P1`: 게임 진행 또는 핵심 기능을 실제로 깨뜨리는 문제
- `P2`: 특정 흐름에서 잘못된 동작을 만드는 문제
- `P3`: 명세 불일치, 유지보수 리스크, UI 결함

## 현재 브랜치 반영 사항

아래 항목들은 현재 브랜치에서 우선 수정됐다.

- 다인전 허용 시 1:1 전용 로직으로 깨지던 문제
- 게임 종료 후 방 상태가 복구되지 않던 문제
- 정상 종료 시 `LEADERBOARD_UPDATE` 메시지가 잘못 조립되던 문제
- 비밀번호 방 입장 UI가 실제로 동작하지 않던 문제
- `CreateRoomDialog` 비밀번호 필드 레이아웃 오류
- 클라이언트/서버 UTF-8 불일치
- 플레이어 목록 요청 커맨드 오동작

아래 목록은 원본 분석에서 발견된 내용 중 아직 남아 있는 구조적 문제와 장기 대응이 필요한 항목 위주로 읽으면 된다.

## P1

### 1. 방은 4명까지 허용하지만 게임 로직은 2명만 지원함

- 관련 코드
  - `src/server/GameServer.java:111`
  - `src/server/game/ServerGameState.java:205`
  - `src/client/ui/game/GameLobby.java:500`
  - `src/client/ui/game/GameScreen.java:36`
- 설명
  - 방 생성은 `2~4명`을 허용하지만, 서버는 `getOpponentOf()`에서 두 번째 플레이어를 직접 참조하고, 클라이언트 UI도 상대 1명만 표시한다.
- 영향
  - 3명 이상 방에서 게임 시작 시 점수, pH, 승패 판정, UI 표시가 모두 불완전하거나 예외를 유발할 수 있다.
- 권장 조치
  - 우선 `maxPlayers`를 2로 고정하거나, 다인전 규칙을 별도 설계로 확장해야 한다.

### 2. 게임 종료 후 방 상태가 복구되지 않아 재시작이 막힘

- 관련 코드
  - `src/server/GameServer.java:261`
  - `src/server/game/ServerGameController.java:258`
- 설명
  - 게임 시작 시 `room.setGameStarted(true)`와 `room.setInGame(true)`를 설정하지만, 정상 종료 시 이를 되돌리는 코드가 없다.
- 영향
  - 한 판 끝난 뒤 같은 방에서 다시 시작할 수 없다.
- 권장 조치
  - 게임 종료 콜백 또는 서버 측 정리 메서드를 통해 `gameStarted=false`, `inGame=false`, 컨트롤러 정리를 보장해야 한다.

### 3. 정상 게임 종료 시 리더보드 갱신 메시지가 잘못 조립됨

- 관련 코드
  - `src/server/game/ServerGameController.java:249`
- 설명
  - `ServerMessage.LEADERBOARD_UPDATE + room.getRoomId() + ...`로 문자열을 붙여 `LEADERBOARD_UPDATER1|...` 형태가 된다.
- 영향
  - 클라이언트 `MessageHandler`가 메시지 타입을 인식하지 못한다.
- 권장 조치
  - `LEADERBOARD_UPDATE + "|" + roomId + ...`로 수정해야 한다.

### 4. 구분자 문자가 데이터에 들어오면 프로토콜이 깨짐

- 관련 코드
  - `src/server/ClientHandler.java:63`
  - `src/client/network/MessageHandler.java:31`
  - `src/server/GameServer.java:216`
  - `src/game/model/GameRoom.java:40`
- 설명
  - 메시지는 `|`, 방 정보는 `,`, 플레이어 목록은 `;`로 분리하지만 escape 규칙이 없다.
  - 채팅, 닉네임, 방 제목에 구분자가 들어오면 파싱이 망가진다.
- 영향
  - 채팅 손실, 방 목록 파손, 잘못된 필드 매핑 발생
- 권장 조치
  - 입력 제한을 두거나, 권장안대로 JSON line 프로토콜로 교체한다.

## P2

### 5. 비밀번호 방 입장 UI가 실제로는 비밀번호를 보내지 않음

- 관련 코드
  - `src/client/ui/dialog/RoomListDialog.java:244`
  - `src/client/ui/dialog/RoomListDialog.java:248`
- 설명
  - 비밀번호 입력 다이얼로그는 구현돼 있지만 호출되지 않는다.
- 영향
  - 비밀번호가 설정된 방은 클라이언트에서 정상 입장할 수 없다.

### 6. 클라이언트가 방에서 나갈 때 로컬에서 `ROOM_CLOSED` 이벤트를 강제로 발생시킴

- 관련 코드
  - `src/client/app/GameClient.java:160`
- 설명
  - 단순 퇴장인데도 `"방이 닫혔습니다."` 이벤트를 직접 발생시킨다.
- 영향
  - 참가자가 그냥 나가는 흐름과 방이 실제로 닫힌 흐름이 섞인다.
  - UI 메시지와 상태 전환이 잘못될 수 있다.

### 7. 플레이어 목록 요청 API가 잘못된 커맨드를 보냄

- 관련 코드
  - `src/client/app/GameClient.java:111`
- 설명
  - `sendPlayerListRequest()`가 `PLAYER_LIST`가 아니라 `ROOM_LIST`를 보낸다.
- 영향
  - 메서드 사용 시 의도와 전혀 다른 응답이 온다.

### 8. 호스트 위임 순서가 비결정적임

- 관련 코드
  - `src/server/GameServer.java:196`
- 설명
  - `HashSet` iterator 첫 원소를 새 방장으로 삼는다.
- 영향
  - 주석에는 "다음으로 들어온 사람"이라고 적혀 있지만 실제로는 순서가 보장되지 않는다.

### 9. 채팅 메시지도 `|` 포함 시 잘림

- 관련 코드
  - `src/server/ClientHandler.java:153`
  - `src/client/network/MessageHandler.java:315`
- 설명
  - 채팅은 `split("\\|")` 이후 `parts[2]`만 사용한다.
- 영향
  - 메시지 본문 중 `|` 뒤가 유실된다.

## P3

### 10. `CreateRoomDialog`에서 비밀번호 필드가 레이아웃에 겹쳐 추가됨

- 관련 코드
  - `src/client/ui/dialog/CreateRoomDialog.java:52`
  - `src/client/ui/dialog/CreateRoomDialog.java:56`
- 설명
  - 비밀번호 레이블은 주석 처리됐지만 비밀번호 필드는 같은 그리드 위치에 추가된다.
- 영향
  - UI 겹침 또는 예기치 않은 배치가 생길 수 있다.

### 11. 클라이언트/서버 문자 인코딩이 일관되지 않음

- 관련 코드
  - `src/server/ClientHandler.java:33`
  - `src/client/app/GameClient.java:63`
- 설명
  - 서버는 UTF-8을 명시하지만 클라이언트는 플랫폼 기본 인코딩을 사용한다.
- 영향
  - 한글 닉네임/채팅에서 환경별 깨짐 가능성이 있다.

### 12. 사용되지 않거나 반쯤 남은 프로토콜 상수가 있음

- 관련 코드
  - `src/client/event/GameEvent.java`
  - `src/client/network/MessageHandler.java:40`
  - `src/server/GameServer.java:265`
- 설명
  - `PLAYER_LIST_RESPONSE`, `MY_RECORDS_DATA`, `GAME_CONFIG`, `USERS_REQUEST` 등이 실제 흐름과 맞지 않거나 미사용 상태다.
- 영향
  - 명세 이해를 어렵게 하고 리팩터링 비용을 높인다.

## 즉시 수정 추천

1. `maxPlayers`를 2로 제한하거나 다인전 지원 범위를 명확히 잘라낸다.
2. 정상 게임 종료 시 방 상태를 초기화한다.
3. `LEADERBOARD_UPDATE` 메시지 문자열 조립 오류를 수정한다.
4. 메시지 포맷을 문서 기준으로 통일하고, 입력 제한 또는 JSON 직렬화로 구분자 문제를 제거한다.
