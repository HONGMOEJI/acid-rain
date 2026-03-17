# Protocol Overview

## 1. 전송 방식

현재 프로젝트는 TCP 소켓 위에서 `개행(\n)` 단위 문자열 메시지를 주고받는다.

- 서버 수신 파서: `src/server/ClientHandler.java`
- 클라이언트 수신 파서: `src/client/network/MessageHandler.java`

## 2. 현재 공통 포맷

기본 메시지 형태:

```text
TYPE|field1|field2|field3
```

부가 포맷:

- 방 정보 묶음: `roomId,roomName,currentPlayers,maxPlayers,gameMode,difficulty,hostName`
- 플레이어 목록: `playerA;playerB;playerC`

## 3. 현재 공통 제약

- `|`, `,`, `;`에 대한 escape 규칙이 없다.
- 요청/응답/브로드캐스트 이벤트가 같은 네임스페이스에 섞여 있다.
- 클라이언트와 서버가 같은 문자열 상수를 공유하지만, 의미 구분은 느슨하다.

## 4. 프로토콜 분류

- 연결/로비 프로토콜
  - 로그인, 유저 수, 방 목록, 방 생성/입장/퇴장, 설정 변경, 채팅
- 게임 플레이 프로토콜
  - 게임 시작, 단어 생성, 입력, 미스, pH 변경, 블라인드, 게임 종료
- 리더보드 프로토콜
  - 상위 기록 조회, 내 기록 조회, 랭크 갱신

## 5. 문서 읽는 법

각 문서는 다음 관점으로 정리했다.

- 현재 구현에서 실제 오가는 메시지
- 현재 구현의 문제점
- 유지보수를 위한 권장 명세
