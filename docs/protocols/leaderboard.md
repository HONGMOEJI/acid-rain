# Leaderboard Protocol

## 1. 조회 요청

### 클라이언트 -> 서버

전체 순위:

```text
LEADERBOARD_ACTION|GET_TOP|JAVA|EASY
```

내 기록:

```text
LEADERBOARD_ACTION|GET_MY_RECORDS|JAVA|EASY
```

주의:

- 여기서도 `ClientEvent.LEADERBOARD_ACTION` 상수를 요청 커맨드처럼 사용한다.
- 의미상 `ClientCommand`가 더 맞다.

## 2. 조회 응답

### 서버 -> 클라이언트

전체 순위:

```text
LEADERBOARD_DATA|TOP|entry1|entry2|...
```

내 기록:

```text
LEADERBOARD_DATA|USER|entry1|entry2|...
```

각 `entry` 포맷:

```text
username,score,mode,difficulty,yyyy-MM-dd HH:mm:ss
```

예시:

```text
LEADERBOARD_DATA|TOP|moeji,1200,JAVA,EASY,2026-03-18 21:30:00
```

## 3. 게임 종료 후 갱신 알림

의도된 메시지:

```text
LEADERBOARD_UPDATE|roomId|playerName|rank
```

현재 상태:

- 몰수승 처리 흐름에서는 정상 조립된다.
- 일반 게임 종료 흐름에서는 `LEADERBOARD_UPDATER1|...`처럼 깨질 수 있다.

## 4. 서버 저장 규칙

저장 담당:

- `src/server/game/LeaderboardManager.java`

저장 특성:

- 모드/난이도별 파일 저장
- 동일 유저의 이전 기록 제거 후 최신 기록 하나만 유지
- 난이도별 최소 점수 기준 존재
  - Easy: 500
  - Medium: 750
  - Hard: 1000

## 5. 현재 문제점

- `MY_RECORDS_DATA` 상수는 있으나 현재 흐름에서는 사실상 `LEADERBOARD_DATA|USER|...`만 사용한다.
- `entry`도 `,` 구분자 기반이라 닉네임 정책을 엄격히 두지 않으면 파싱 위험이 있다.
- 전체 순위와 내 기록을 같은 메시지 타입 안에서 서브타입으로 나누고 있어, 메시지 의미가 다소 흐릿하다.

## 6. 권장 명세

다음 둘 중 하나로 정리하는 것이 좋다.

### 선택지 A: 문자열 기반 최소 수정

```text
LEADERBOARD.GET_TOP|mode|difficulty
LEADERBOARD.GET_ME|mode|difficulty
LEADERBOARD.TOP|entry1|entry2|...
LEADERBOARD.ME|entry1|entry2|...
LEADERBOARD.RANK|roomId|player|rank
```

### 선택지 B: JSON line 기반 권장안

```json
{"type":"leaderboard.getTop","mode":"JAVA","difficulty":"EASY"}
{"type":"leaderboard.top","entries":[...]}
```

리더보드는 구조화 데이터 비중이 높아서 JSON 전환 효과가 특히 크다.
