# 3주차 - 회원가입 프로그램

## 실습 코드
- `UserRegister.java` : 회원가입 프로그램 (Y/N 처리, 데이터 입력/저장, 반복 가입)

## 새로 알게 된 것
- `boolean register = false` → while 조건으로 반복 제어
- `equalsIgnoreCase()` → 대소문자 구분 없이 Y/y, N/n 처리
- `System.exit(0)` → 프로그램 정상 종료 (0 = 정상)
- 패스워드 확인은 while 안에 while 중첩 구조로 처리
- `birth_date.length() == 6` → 입력값 길이 검증
- `users.add(user)` → HashMap을 ArrayList에 저장
- `user.get("name")` → HashMap에서 값 불러와 메시지 조합