# Connection And Lobby Protocol

## 1. 로그인과 초기 동기화

### 클라이언트 -> 서버

```text
LOGIN|username
ROOM_LIST
USERS_REQUEST
```

### 서버 -> 클라이언트

```text
USERS|count
ROOM_LIST_RESPONSE|roomInfo1|roomInfo2|...
```

### 비고

- `USERS_REQUEST`는 메인 메뉴에서만 쓰이며 주석상 제거 예정이다.
- 로그인 직후 서버도 `broadcastUserCount()`와 `broadcastRoomList()`를 호출한다.

## 2. 방 목록

### 서버 -> 클라이언트

```text
ROOM_LIST_RESPONSE|R1,RoomA,1,2,Java,Easy,hostUser|R2,RoomB,2,2,C,Hard,otherUser
```

`roomInfo` 필드 형식:

```text
roomId,roomName,currentPlayers,maxPlayers,gameMode,difficulty,hostName
```

## 3. 방 생성

### 클라이언트 -> 서버

```text
CREATE_ROOM|roomName|password|mode|difficulty|maxPlayers
```

실제 클라이언트 전송값:

- `mode`: `JAVA`, `PYTHON`, `KOTLIN`, `C`
- `difficulty`: `EASY`, `MEDIUM`, `HARD`

서버는 `fromDisplayName()`로 처리하지만 현재는 대소문자 무시 덕분에 우연히 동작한다.

### 서버 -> 클라이언트

성공:

```text
CREATE_ROOM_RESPONSE|true|방이 생성되었습니다.|roomInfo|roomId
```

실패:

```text
CREATE_ROOM_RESPONSE|false|실패 사유
```

## 4. 방 입장

### 클라이언트 -> 서버

```text
JOIN_ROOM|roomId
JOIN_ROOM|roomId|password
```

### 서버 -> 클라이언트

성공:

```text
JOIN_ROOM_RESPONSE|true|방에 입장했습니다.|roomInfo
```

실패:

```text
JOIN_ROOM_RESPONSE|false|실패 사유
```

## 5. 플레이어 목록/방 인원 갱신

### 클라이언트 -> 서버

```text
PLAYER_LIST|roomId
```

### 서버 -> 클라이언트

실제 구현은 아래 메시지를 사용한다.

```text
PLAYER_UPDATE|roomId|playerCount|playerA;playerB
```

주의:

- 상수로는 `PLAYER_LIST_RESPONSE`도 존재하지만 실제 서버는 `PLAYER_UPDATE`를 보낸다.
- 즉, 현재 명세와 구현이 어긋나 있다.

## 6. 방 설정 변경

### 클라이언트 -> 서버

```text
SETTINGS_UPDATED|roomId|MODE|Java
SETTINGS_UPDATED|roomId|DIFFICULTY|Easy
```

주의:

- 클라이언트는 `ServerMessage.SETTINGS_UPDATE` 상수를 이용해 요청을 보내지만 값은 `SETTINGS_UPDATED`다.
- 의미상 `요청`인데 이름은 `서버 메시지` 쪽 상수를 재사용하고 있다.

### 서버 -> 클라이언트

```text
SETTINGS_UPDATED|roomId|JAVA|EASY
```

## 7. 채팅

### 클라이언트 -> 서버

```text
CHAT|roomId|message
```

### 서버 -> 클라이언트

```text
CHAT|username|message
```

주의:

- 메시지에 `|`가 들어가면 파싱이 깨진다.

## 8. 방 퇴장/폐쇄

### 클라이언트 -> 서버

```text
LEAVE_ROOM|roomId
```

실제 서버는 `currentRoomId`를 사용하므로 payload의 `roomId`는 거의 신뢰하지 않는다.

### 서버 -> 클라이언트

호스트 퇴장:

```text
HOST_LEFT|roomId|이전 방장이 퇴장했습니다.
NEW_HOST|roomId|newHostName
PLAYER_UPDATE|roomId|playerCount|...
```

방이 완전히 닫힘:

```text
ROOM_CLOSED|roomId|방이 닫혔습니다.
```

## 9. 현재 프로토콜 문제 요약

- `PLAYER_LIST_RESPONSE`와 `PLAYER_UPDATE`의 역할이 겹친다.
- `SETTINGS_UPDATED`가 요청과 응답 양쪽에 모두 쓰인다.
- 퇴장과 방 닫힘 이벤트가 클라이언트 로컬 이벤트와 서버 이벤트에서 혼재한다.
- 입력값 escape 규칙이 없다.

## 10. 권장 정리 방향

최소 수정 버전이라면 아래처럼 역할을 명확히 나누는 것이 좋다.

- 요청
  - `ROOM.CREATE`
  - `ROOM.JOIN`
  - `ROOM.LEAVE`
  - `ROOM.SETTINGS`
  - `ROOM.CHAT`
- 응답
  - `ROOM.CREATE.OK`
  - `ROOM.JOIN.OK`
  - `ROOM.LIST`
- 이벤트
  - `ROOM.PLAYER_SYNC`
  - `ROOM.CLOSED`
  - `ROOM.HOST_CHANGED`
