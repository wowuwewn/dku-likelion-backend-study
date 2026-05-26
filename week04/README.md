# 4주차 - Java Next Level (객체지향 + 예외처리)

## 학습 내용
- Java Next Level - 객체지향 (1) 객체 지향 개념
- Java Next Level - 객체지향 (2) 클래스 구현
- Java Next Level - 객체지향 (3) 생성자
- Java Next Level - 객체지향 (4) 상속
- Java Next Level - 객체지향 (5) 오버로딩 & 오버라이딩
- Java Next Level - 객체지향 (6) 접근 제어자
- Java Next Level - 객체지향 (7) Getter & Setter
- Java Next Level - 객체지향 (8) 인터페이스
- Java Next Level - 예외 처리

## 실습 코드
| 파일명 | 학습 내용 |
|--------|----------|
| `BankAccount.java` | 클래스, 생성자, 접근 제어자, Getter & Setter |
| `ClassExample.java` | 인스턴스 생성, 메서드 호출 |
| `DollarAccount.java` | 상속, 오버로딩, 오버라이딩 |
| `SavingsAccount.java` | 상속, 인터페이스 구현 |
| `SubscriptionAccount.java` | 상속 |
| `Withdrawable.java` | 인터페이스 선언 |
| `ExceptionExample.java` | 예외 처리 (try-catch-finally) |

## 새로 알게 된 것

### 클래스와 생성자
- Java는 클래스명과 파일명이 반드시 일치해야 함
- 커스텀 생성자를 만들면 기본 생성자가 자동 생성되지 않음 → 빈 생성자 따로 만들어야 함
- `this.멤버변수 = 파라미터` → 왼쪽은 멤버변수, 오른쪽은 입력받은 값

### 접근 제어자와 Getter & Setter
- `private` → 동일 클래스에서만 접근 가능
- `private` 변수는 외부에서 직접 접근 불가 → Getter & Setter로만 접근

### 상속과 다형성
- `extends` → 부모 클래스의 속성과 메서드를 물려받음
- 오버로딩 → 같은 메서드명, 파라미터 변경
- 오버라이딩 → 같은 메서드명, 내용 변경

### 인터페이스
- `interface` → 메서드 규격만 정의, 구현 없음
- `implements` → 인터페이스의 모든 메서드를 반드시 구현해야 함

### 예외 처리
- `try-catch-finally` → finally는 예외 발생 여부와 관계없이 항상 실행
- `catch` 블록은 구체적인 예외부터, `Exception`은 마지막에 작성