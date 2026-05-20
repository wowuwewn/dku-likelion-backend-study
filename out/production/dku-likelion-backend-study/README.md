# 3주차 - 회원가입 프로그램

## 학습 내용
- Java Next Level - 회원가입 프로그램 (1)
- Java Next Level - 회원가입 프로그램 (2)
- Java Next Level - 회원가입 프로그램 (3)

## 실습 코드
| 파일명 | 학습 내용 |
|--------|----------|
| `UserRegister.java` | Y/N 입력 처리, 데이터 입력 및 저장, 반복 가입 |

## 새로 알게 된 것

### 조건 분기 처리
- `boolean register = false` → while 조건으로 반복 제어
- `equalsIgnoreCase()` → 대소문자 구분 없이 Y/y, N/n 처리
- `System.exit(0)` → 프로그램 정상 종료 (0 = 정상)

### 중첩 반복문
- 패스워드 확인은 while 안에 while 중첩 구조로 처리
- 일치하면 `break`로 내부 while 탈출

### 입력값 검증
- `birth_date.length() == 6` → 입력값 길이 검증
- 검증 실패 시 다시 입력받도록 while 반복

### 자료구조 활용
- `HashMap` → 한 명의 회원 데이터 저장 (키-값 쌍)
- `ArrayList` → 여러 회원 데이터 저장
- `user.get("name")` → HashMap에서 값 불러와 메시지 조합
- `users.add(user)` → HashMap을 ArrayList에 저장