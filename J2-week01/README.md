# 2학기 1주차 — 자바로 구현하는 간단한 할 일 관리 서비스

`12가지 기술로 구현하는 APP서비스` 강의를 따라 Java로 간단한 콘솔 할 일 관리 서비스를 구현했습니다.

## 학습 내용

- 콘솔에서 명령어와 할 일 입력받기
- `Todo` 객체를 목록에 저장하고 조회하기
- 기능별 메서드와 Controller의 책임 나누기

## 구현 기능

| 명령어 | 기능 |
|--------|------|
| `add` | 할 일 추가 |
| `list` | 할 일 목록 조회 |
| `del` | 할 일 삭제 |
| `modify` | 할 일 수정 |
| `exit` | 프로그램 종료 |

## 프로젝트 구조

```text
com.ll
├── Main.java
├── App.java
├── Todo.java
├── TodoController.java
└── SystemController.java
```

- `Main`: 프로그램 실행
- `App`: 명령어 입력과 실행 흐름 관리
- `Todo`: 할 일 데이터 관리
- `TodoController`: 할 일 추가·조회·수정·삭제 처리
- `SystemController`: 프로그램 종료 처리

## 핵심 개념

- `Scanner`를 이용한 입력 처리
- `while` 반복문과 조건문
- 객체와 생성자, `this`, Getter·Setter
- `List`와 `ArrayList`
- `removeIf`
- Stream의 `filter`, `findFirst`, `orElse`
- 메서드 분리와 `App`·`TodoController`·`SystemController`의 책임 분리

## 배운 점

- 기능이 늘어날수록 `App`에 모두 작성하지 않고 Controller별로 역할을 나누는 이유를 알게 됐다.
- `this.content = content`처럼 객체의 필드와 매개변수를 구분하는 부분을 이번 실습을 통해 다시 확인했다.
- `removeIf`와 Stream을 사용해봤지만 아직 익숙하지 않아 이후에 다시 복습할 예정이다.

## 실행 예시

```text
명령) add
할 일 : Java 공부
1번 할일이 생성되었습니다.
명령) list
번호 / 내용
1 / Java 공부
명령) modify
수정할 할일의 번호:
1
기존 할일: Java 공부
새 할일: Java 복습
1번 할일이 수정되었습니다.
명령) del
삭제할 할일의 번호:
1
1번 할일이 삭제되었습니다.
명령) exit
앱 종료 명령이 입력되었습니다.
프로그램이 곧 종료합니다.
```
