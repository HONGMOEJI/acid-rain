# Protocol V2 Simplification Proposal

## 목표

현재 프로토콜을 전면 재설계하지 않더라도 아래 네 가지는 해결해야 한다.

1. 메시지 타입의 역할 혼선 제거
2. 구분자 충돌 제거
3. 현재/미래 명세의 확장성 확보
4. 디버깅 가능한 일관된 포맷 제공

## 권장안

가장 실용적인 방법은 `JSON Lines`로 바꾸는 것이다.

- 한 줄에 JSON 객체 하나
- TCP의 `readLine()` 구조를 그대로 유지 가능
- escape 문제 해결
- 신규 필드 추가가 쉬움

예시:

```json
{"type":"auth.login","username":"moeji"}
{"type":"room.list.request"}
{"type":"room.list","rooms":[{"id":"R1","name":"test","currentPlayers":1,"maxPlayers":2,"mode":"JAVA","difficulty":"EASY","host":"moeji"}]}
```

## 공통 envelope

```json
{
  "type": "room.join",
  "requestId": "c-102",
  "roomId": "R1",
  "payload": {}
}
```

필드 규칙:

- `type`: 메시지 타입
- `requestId`: 요청-응답 추적용, 브로드캐스트면 생략 가능
- `roomId`: 룸 범위 메시지일 때 사용
- `payload`: 타입별 데이터

## 타입 네이밍 규칙

- 요청: `domain.action`
- 응답: `domain.action.ok`, `domain.action.error`
- 서버 브로드캐스트 이벤트: `domain.event`

예시:

- `auth.login`
- `room.create`
- `room.create.ok`
- `room.playerSync`
- `game.start`
- `game.wordSpawn`
- `leaderboard.getTop`

## 최소 메시지 세트

### 인증/로비

```json
{"type":"auth.login","payload":{"username":"moeji"}}
{"type":"lobby.users","payload":{"count":3}}
{"type":"room.list"}
{"type":"room.create","payload":{"name":"Room A","password":"","mode":"JAVA","difficulty":"EASY","maxPlayers":2}}
{"type":"room.join","roomId":"R1","payload":{"password":""}}
{"type":"room.leave","roomId":"R1"}
{"type":"room.playerSync","roomId":"R1","payload":{"host":"moeji","players":["moeji","guest"]}}
{"type":"room.settings","roomId":"R1","payload":{"mode":"JAVA","difficulty":"EASY"}}
{"type":"room.chat","roomId":"R1","payload":{"username":"moeji","message":"hello"}}
```

### 게임

```json
{"type":"game.start","roomId":"R1","payload":{"mode":"JAVA","difficulty":"EASY","players":["moeji","guest"]}}
{"type":"game.wordSpawn","roomId":"R1","payload":{"wordId":"w-10","text":"class","x":320,"effect":"NONE"}}
{"type":"game.wordMatch","roomId":"R1","payload":{"wordId":"w-10","player":"moeji","score":150}}
{"type":"game.phSync","roomId":"R1","payload":{"player":"guest","ph":6.4}}
{"type":"game.over","roomId":"R1","payload":{"winner":"moeji","reason":"PH_ZERO","scores":{"moeji":150,"guest":120}}}
```

### 리더보드

```json
{"type":"leaderboard.getTop","payload":{"mode":"JAVA","difficulty":"EASY"}}
{"type":"leaderboard.top","payload":{"entries":[...]}}
{"type":"leaderboard.getMine","payload":{"mode":"JAVA","difficulty":"EASY"}}
{"type":"leaderboard.mine","payload":{"entries":[...]}}
```

## 현실적인 이행 순서

### 1단계

- 게임 규칙을 `2인 전용`으로 명세 고정
- 기존 문자열 프로토콜을 문서 기준으로 정리

### 2단계

- 서버/클라이언트에 `MessageCodec` 계층 도입
- 기존 문자열 생성/파싱 코드를 여기로 이동

### 3단계

- 내부 표현은 DTO로 바꾸고 바깥만 문자열 유지

### 4단계

- 실제 전송 포맷을 JSON line으로 전환

## 만약 문자열 포맷을 유지해야 한다면

그 경우에도 아래는 반드시 적용해야 한다.

1. 메시지 타입을 `ROOM_CREATE`, `ROOM_JOIN_OK`, `ROOM_PLAYER_SYNC`처럼 역할별로 분리한다.
2. 사용자 입력 필드에서 `|`, `,`, `;`를 금지한다.
3. `roomId`, `wordId` 같은 식별자를 항상 포함한다.
4. 요청/응답/이벤트 명명 규칙을 문서로 고정한다.
