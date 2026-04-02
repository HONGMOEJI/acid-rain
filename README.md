# ACID RAIN

Java Swing UI와 소켓 통신으로 만든 타자 배틀 게임입니다. 로비에서 프로그래밍 언어와 난이도를 고른 뒤, 떨어지는 키워드를 빠르게 입력해 점수를 올리고 리더보드 기록을 겨루는 구조입니다.

## Overview

- `Java Swing` 기반 UI와 `TCP Socket` 통신으로 구현한 네트워크 타자 게임입니다.
- 메인 메뉴, 방 로비, 실시간 게임 화면, 리더보드까지 한 흐름으로 이어지는 구조입니다.
- Java, Python, Kotlin, C 키워드를 테마로 사용하며, 난이도와 모드를 바꿔가며 플레이할 수 있습니다.

## Features

- 실시간 멀티플레이: 여러 플레이어가 같은 방에 입장해 동시에 플레이할 수 있습니다.
- 방 로비 시스템: 방 생성, 입장, 퇴장, 참가자 목록 갱신, 채팅 기능을 제공합니다.
- 게임 설정 변경: 방장이 프로그래밍 언어 모드와 난이도를 변경할 수 있습니다.
- 인게임 상태 표시: 점수, pH, 참가자 수, 실시간 점수판을 한 화면에서 보여줍니다.
- 특수 효과 단어: 추가 점수 또는 상대 화면 블라인드 같은 요소가 포함됩니다.
- 리더보드: 모드와 난이도 기준으로 상위 기록과 내 기록을 분리해 볼 수 있습니다.

## Game Flow

1. 서버 실행 후 클라이언트에서 닉네임과 주소를 입력해 접속합니다.
2. 메인 메뉴에서 방 목록으로 이동하거나 새 방을 만들어 로비에 입장합니다.
3. 방장이 모드와 난이도를 설정하고, 참가자가 모이면 게임을 시작합니다.
4. 인게임 화면에서 떨어지는 키워드를 빠르게 입력해 점수와 pH를 관리합니다.
5. 게임 종료 후 결과를 확인하고, 리더보드에서 기록을 비교할 수 있습니다.

## Project Structure

- `src/client`
  클라이언트 앱 진입점, 네트워크 처리, Swing UI 컴포넌트가 들어 있습니다.
- `src/server`
  서버 진입점, 클라이언트 연결 처리, 게임 상태 및 리더보드 관리 로직이 들어 있습니다.
- `src/game/model`
  게임 모드, 난이도, 방, 단어, 리더보드 엔트리 같은 공용 모델을 정의합니다.
- `resources`
  폰트와 단어 목록 같은 런타임 리소스를 포함합니다.
- `docs`
  구조 분석과 프로토콜 정리 문서를 담고 있습니다.

## Run

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -cp out:resources server.ServerMain
```

다른 터미널에서 클라이언트를 실행합니다:

```bash
java -cp out:resources client.app.ClientMain
```

기본 서버 포트는 `12345`입니다.

## Docs

- 구조 분석: [`docs/architecture-analysis.md`](docs/architecture-analysis.md)
- 버그 헌팅 메모: [`docs/bug-hunting.md`](docs/bug-hunting.md)
- 프로토콜 개요: [`docs/protocols/README.md`](docs/protocols/README.md)
