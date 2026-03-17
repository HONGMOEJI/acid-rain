# Gameplay Protocol

## 1. 게임 시작

### 클라이언트 -> 서버

```text
START_GAME|roomId
```

### 서버 -> 클라이언트

실제 구현:

```text
GAME_START|roomId|playerA;playerB;playerC
```

주의:

- 현재 브랜치에서는 `GAME_CONFIG`를 제거하고 `GAME_START`에 참가자 목록을 포함한다.

## 2. 단어 생성

### 서버 -> 클라이언트

일반 단어:

```text
WORD_SPAWNED|roomId|word|x
```

특수 효과 단어:

```text
WORD_SPAWNED|roomId|word|x|SCORE_BOOST
WORD_SPAWNED|roomId|word|x|BLIND_OPPONENT
```

## 3. 단어 입력

### 클라이언트 -> 서버

```text
GAME_ACTION|roomId|WORD_INPUT|typedWord
```

### 서버 -> 클라이언트

```text
WORD_MATCHED|roomId|word|playerName|newScore
PH_UPDATE|roomId|playerName|newPH
```

특수 효과 발생 시:

```text
BLIND_EFFECT|roomId|targetPlayer|5000
```

## 4. 단어 미스

### 클라이언트 -> 서버

```text
GAME_ACTION|roomId|WORD_MISSED|word
```

클라이언트는 화면 아래로 떨어진 단어를 감지하면 직접 미스 이벤트를 보낸다.

### 서버 -> 클라이언트

```text
WORD_MISSED|roomId|word|playerName|newPH
```

현재 서버는 한 단어를 놓치면 `모든 플레이어`의 pH를 동시에 감소시킨다.

## 5. 인게임 퇴장

### 클라이언트 -> 서버

```text
GAME_ACTION|roomId|PLAYER_LEAVE_GAME|myName
LEAVE_ROOM|roomId
```

주의:

- 클라이언트는 게임 종료 버튼을 누르면 `GAME_ACTION`과 `LEAVE_ROOM`을 연속 전송한다.
- 서버는 첫 메시지로 몰수패 처리하고, 두 번째 메시지로 방 퇴장까지 수행한다.

### 서버 -> 클라이언트

```text
GAME_OVER|roomId|winner|playerA:120;playerB:90;playerC:70|FORFEIT
```

## 6. 일반 게임 종료

### 서버 -> 클라이언트

```text
GAME_OVER|roomId|winner|playerA:120;playerB:90;playerC:70|NORMAL
```

리더보드 등록 성공 시 의도된 메시지:

```text
LEADERBOARD_UPDATE|roomId|winner|rank
```

현재 브랜치에서는 리더보드 문자열 조립 버그도 함께 수정됐다.

## 7. 현재 게임 프로토콜의 구조적 문제

### 7.1 서버와 클라이언트가 모두 단어 낙하를 관리함

- 서버는 단어 생성만 하고 Y축 진행은 모른다.
- 클라이언트는 로컬 타이머로 단어를 떨어뜨린다.
- 따라서 미스 판정 시점은 서버가 아니라 각 클라이언트 로컬 렌더링 타이밍에 좌우된다.

즉, 현재 구조는 서버 authoritative 게임이라기보다 `서버 생성 + 클라이언트 시뮬레이션` 구조다.

### 7.2 점수판과 pH가 2인전을 전제로 함

- `opponentScoreLabel`이 하나뿐이다.
- `getOpponentOf()`는 상대 1명만 찾는다.

### 7.3 `GAME_CONFIG`는 미완성 상태

- 서버는 보내지만 클라이언트는 받지 않는다.
- 플레이어 구성, 모드, 난이도를 게임 화면이 명시적으로 동기화하지 않는다.

## 8. 권장 명세

게임 프로토콜은 아래 세 단계로 정리하는 것이 좋다.

### 단계 A: 방 시작 스냅샷

```text
GAME.STARTED|roomId|mode|difficulty|playerA;playerB
```

### 단계 B: 플레이 이벤트

```text
GAME.WORD_SPAWN|roomId|wordId|text|x|effect
GAME.WORD_MATCH|roomId|wordId|player|score|ph
GAME.WORD_MISS|roomId|wordId|player|ph
GAME.EFFECT.BLIND|roomId|target|durationMs
GAME.PH_SYNC|roomId|player|ph
```

### 단계 C: 종료

```text
GAME.OVER|roomId|winner|reason|scoreSummary
```

최소 수정으로 갈 경우에도 `wordId` 추가는 강하게 권장한다. 현재는 단어 텍스트만 키로 쓰기 때문에 같은 단어가 동시에 여러 개 나오면 식별이 애매해진다.
