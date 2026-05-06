# 2주차 - Java Beginner

##  학습 내용
- Java Beginner - 변수와 자료형
- Java Beginner - 연산자
- Java Beginner - 조건문과 반복문
- Java Beginner - 자료구조
- Java Next Level - 함수

## 실습 코드
| 파일명 | 학습 내용 |
|--------|----------|
| `Variables.java` | 변수, 자료형, 상수, 타입 캐스팅 |
| `StringPractice.java` | 문자열 비교(equals), StringBuilder, 문자열 메서드 |
| `LoopPractice.java` | for문, while문, 배열 + 반복문 |
| `MethodPractice.java` | 함수 4가지 형태, ArrayList, HashMap |

## ️새로 알게 된 것

### 변수와 자료형
- `float` 선언 시 숫자 끝에 `F` 안 붙이면 에러 발생
- `boolean`은 `true` / `false`만 가능 (1, 0 대입 불가)
- 참조형(String)은 힙 메모리에 저장되고 변수엔 주소값이 들어감

### 타입 캐스팅
- 작은 → 큰 타입은 자동, 큰 → 작은 타입은 강제 형변환 필요
- `byte b = (byte)128;` → 오버플로우로 -128 출력됨

### 문자열
- `==`은 주소 비교, 내용 비교는 `.equals()` 사용
- 많은 문자열 결합엔 `StringBuilder` 권장
- `trim()`은 양쪽 끝만, 중간 공백은 `replace()`로 제거

### 조건문과 반복문
- `switch`에 `break` 없으면 아래 case들 다 실행됨
- while문에 증감식 빠뜨리면 무한 루프 발생
- `do-while`은 조건 거짓이어도 무조건 1회 실행

### 자료구조
- 배열 크기는 `.length`, ArrayList는 `.size()`
- 배열은 크기 고정, ArrayList는 유동적
- HashMap은 키 중복 불가, 값 중복은 허용

### 함수 (메서드)
- 반환값 없으면 `void`
- main이 `static`이면 호출하는 메서드도 `static`이어야 함