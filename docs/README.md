# Acid Rain Documentation

이 디렉터리는 현재 코드베이스를 다시 읽으면서 정리한 분석 문서 모음이다.

## 문서 목록

- `architecture-analysis.md`
  - 전체 구조, 실행 흐름, 설계 부채, 리팩터링 우선순위
- `bug-hunting.md`
  - 코드 기반 버그 헌팅 결과와 우선순위
- `protocols/README.md`
  - 전체 프로토콜 개요와 공통 규칙
- `protocols/connection-and-lobby.md`
  - 로그인, 방 생성/입장/퇴장, 채팅, 설정 변경 프로토콜
- `protocols/gameplay.md`
  - 인게임 시작, 단어 생성, 입력, pH, 게임 종료 프로토콜
- `protocols/leaderboard.md`
  - 리더보드 조회/갱신 프로토콜
- `protocols/v2-simplification.md`
  - 현재 문자열 기반 프로토콜을 정리하기 위한 간소화 제안 명세

## 빠른 결론

- 현재 프로젝트는 `문자열 구분자 기반 프로토콜`, `단일 이벤트 리스너`, `서버 상태와 UI 상태의 직접 결합`이 핵심 구조다.
- 가장 먼저 바로잡아야 하는 것은 `2인 전용 게임 로직`과 `최대 4인 방 설정`의 불일치다.
- 그 다음 우선순위는 `프로토콜 명세 통일`, `게임 종료 후 방 상태 복구`, `메시지 포맷 파손 방지`다.
