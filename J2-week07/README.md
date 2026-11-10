# 2학기 7주차 — REST API로 회원가입 구현

5주차에는 회원·게시물·단축 URL을 JPA로 저장했고, 6주차에는 개발·테스트·운영 환경을 나눴다.<br>
이번에는 기존 `MemberService.join()`을 HTTP 요청과 연결하고, 입력 검증부터 오류 응답까지 회원가입 API를 완성했다.

학습 범위: **Chapter 08 회원가입, 08-01–08-16**<br>
코드의 흐름과 자동 테스트·PowerShell 요청으로 확인한 결과를 정리했다.

## 이번 주 한눈에 보기

| 구분 | 내용 |
| --- | --- |
| 환경 | Java 21 / Spring Boot 3.2.4 / Gradle 8.6 |
| 최종 API | `POST /api/v1/members` |
| 입력 | JSON의 username·password·nickname |
| 성공 | HTTP 201, 회원 정보 저장·응답 DTO 반환 |
| 실패 | 필수값·중복·JSON·HTTP 요청·서버 오류 구분 |
| 검증 | 자동 테스트 44개 통과, 별도 서버에 PowerShell 요청 5개 통과 |

08-01–08-08에서 요청·응답과 HTTP 메서드를 다루고, 08-09부터 버전이 있는 JSON API로 바꿨다.
08-10–08-16에서는 상태 코드·응답 형식·Validation·DTO·AOP를 적용했다.
강의별 구현 과정은 [단계별 학습 기록](./docs/chapter08-progress.md)에 정리했다.

## 이전 주차에서 달라진 점

```text
3주차  배포 후 ArrayList 데이터 유실
4주차  SQL로 DB에 저장
5주차  Entity → Repository → DB
6주차  Profile로 실행 환경 분리
7주차  HTTP 요청 → 입력 검증 → 기존 가입 서비스 → 통일된 JSON 응답
```

| 유지한 코드 | 이번에 추가·변경한 부분 |
| --- | --- |
| Member·MemberRepository | 회원가입 요청을 받는 Controller |
| MemberService.join()·중복 검사 | Service의 필수값 방어, 가입 결과 코드 201 |
| RsData·GlobalException | 전역 예외 처리·응답 DTO·HTTP 상태 반영 |
| dev·test·prod 설정, 초기 데이터 | 설정 파일과 초기화 코드는 유지 |

## 요청부터 DB 저장까지

```text
POST /api/v1/members + JSON
  → Spring MVC: @RequestBody로 요청 객체 생성
  → @Valid: username·password·nickname 검사
  → ApiV1MemberController.join()
  → MemberService.join(): 필수값·중복 검사, Member 생성
  → MemberRepository.save()
  → JPA / Hibernate → INSERT → H2
  → Service 트랜잭션 커밋
  → MemberJoinRespBody로 응답 데이터 변환
  → ResponseAspect가 HTTP 상태를 201로 지정
  → Jackson이 RsData를 JSON으로 변환
```

| 핵심 클래스 | 역할 |
| --- | --- |
| [ApiV1MemberController](./src/main/java/com/ll/demo03/ApiV1MemberController.java) | 경로·POST·JSON 요청 매핑, Validation, 서비스 호출 |
| [MemberService](./src/main/java/com/ll/demo03/MemberService.java) | 가입 규칙·중복 검사·트랜잭션·저장 |
| [MemberRepository](./src/main/java/com/ll/demo03/MemberRepository.java) | username 조회, JPA 저장 |
| [MemberJoinRespBody](./src/main/java/com/ll/demo03/MemberJoinRespBody.java) | 공개할 회원 정보만 응답 |
| [GlobalExceptionHandler](./src/main/java/com/ll/demo03/global/exceptionHandlers/GlobalExceptionHandler.java) | 예외 종류별 JSON·HTTP 상태 결정 |
| [ResponseAspect](./src/main/java/com/ll/demo03/global/aspect/ResponseAspect.java) | 정상 반환된 RsData의 상태를 HTTP 응답에 반영 |
| [Rq](./src/main/java/com/ll/demo03/global/rq/Rq.java) | 현재 요청 경로·응답 상태 접근 |

### Controller는 요청을 받고, Service는 가입을 처리한다

```java
@PostMapping(consumes = "application/json")
public RsData<MemberJoinRespBody> join(@RequestBody @Valid MemberJoinReqBody reqBody) {
    RsData<Member> joinRs = memberService.join(reqBody.username(), reqBody.password(), reqBody.nickname());
    return joinRs.newDataOf(MemberJoinRespBody.from(joinRs.getData()));
}
```

`@RestController`는 메서드의 반환값을 응답본문으로 보낸다.
`@RequestBody`는 JSON을 요청 객체로 바꾸고, `@Valid`는 그 객체에 붙은 검증 규칙을 실행한다.
요청 클래스는 세 필드에 `@NotBlank`를 붙인 record다.

Service에도 필수값 검사를 남겼다. HTTP를 거치지 않는 초기화 코드나 다른 서비스에서도 `join()`을 호출하기 때문이다.
중복이면 `GlobalException`을 던지고 저장 코드는 실행하지 않는다.
`@Transactional`은 가입 저장을 하나의 작업으로 묶는다.

### 엔티티와 응답은 분리한다

```java
public record MemberJoinRespBody(MemberItem item) {
    public record MemberItem(Long id, String username, String nickname) {}
}
```

DB에는 비밀번호가 있어도 응답에는 필요 없다. DTO에 공개할 세 필드만 두었다.
`newDataOf()`는 기존 결과 코드·상태·메시지를 유지하면서 `data`만 DTO로 바꾼다.
`Member.password`의 `@JsonIgnore`도 남겨 엔티티가 직렬화되는 경우를 방어한다.

## 입력 오류와 예외 처리

```text
JSON을 읽을 수 없음 → HttpMessageNotReadableException → 400-JSON
필수값 누락·공백   → MethodArgumentNotValidException → 400-NotBlank
중복 username     → GlobalException → 400-1
기타 예외         → 안전한 공통 오류 응답
```

`@ControllerAdvice` 안의 `@ExceptionHandler`가 예외 종류에 맞는 메서드를 선택한다.
가장 넓은 `Exception` 핸들러가 있어도 Validation·GlobalException에는 각각의 전용 핸들러가 적용된다.
여러 필드가 틀리면 필드 이름순 첫 오류를 반환한다.

| 상황 | HTTP | resultCode |
| --- | ---: | --- |
| 가입 완료 | 201 | `201-1` |
| 중복 아이디 | 400 | `400-1` |
| 필수값 누락·null·빈 문자열·공백 | 400 | `400-NotBlank` |
| 깨진 JSON·본문 없음 | 400 | `400-JSON` |
| 없는 경로 | 404 | `404-1` |
| GET으로 가입 요청 | 405 | `405-1` |
| 잘못된 Content-Type | 415 | `415-1` |
| 예상하지 못한 오류 | 500 | `500-1` |

성공·실패 모두 `resultCode`, `statusCode`, `msg`, `data`를 사용한다.
Validation·중복 오류의 `data`는 `{}`, 기타 오류는 `data.path`에 요청 경로를 담는다.
예외 메시지·스택 추적·쿼리 문자열은 기타 오류 응답에 넣지 않는다.

### JSON의 상태 코드와 HTTP 상태 코드는 별개다

```java
Object result = joinPoint.proceed();
if (result instanceof RsData<?> rsData) {
    rq.setStatusCode(rsData.getStatusCode());
}
```

본문에 `"statusCode": 201`만 적으면 HTTP 상태가 자동으로 바뀌지는 않는다.
08-16의 Aspect가 Controller 실행 후 실제 응답 상태를 설정한다.
예외가 나면 `proceed()` 이후로 진행하지 않으므로, 예외 핸들러가 `ResponseEntity`로 상태를 지정한다.
HTML·List·리다이렉트는 기존 응답 처리를 따른다.

## API 요청·응답

기본 개발 서버는 `http://localhost:8090`이다.
개발 모드로 실행하면 현재 프로젝트의 H2 파일 DB에 가입 정보가 저장된다.
데이터를 남기지 않고 연습하려면 아래 `test` Profile 서버를 사용한다.

```http
POST http://localhost:8090/api/v1/members
Content-Type: application/json

{"username":"study-member","password":"example-only","nickname":"새 회원"}
```

정상 응답 — **HTTP 201**. id는 DB 상태에 따라 달라진다.

```json
{
  "resultCode": "201-1",
  "statusCode": 201,
  "msg": "회원가입이 완료되었습니다.",
  "data": {"item": {"id": 5, "username": "study-member", "nickname": "새 회원"}}
}
```

같은 username으로 다시 보내면 **HTTP 400**이다.

```json
{
  "resultCode": "400-1",
  "statusCode": 400,
  "msg": "study-member(은)는 이미 존재하는 아이디입니다.",
  "data": {}
}
```

nickname을 빼고 `{"username":"another-member","password":"example-only"}`를 보내면 **HTTP 400**이다.

```json
{
  "resultCode": "400-NotBlank",
  "statusCode": 400,
  "msg": "nickname : 닉네임을 입력해주세요.",
  "data": {}
}
```

### Postman·PowerShell로 재현하기

[Postman Collection](./docs/chapter08.postman_collection.json)을 Import하고 `baseUrl`을 실행 서버 주소로 맞춘다.
정상·중복·누락·깨진 JSON·GET 요청과 상태 검사가 들어 있다.
정상 가입은 임의의 username을 사용하고, 중복 예제는 초기 회원 `user1`을 사용한다.
**Postman 앱에서 직접 실행하지는 않았다.** 같은 동작은 자동 테스트와 PowerShell로 확인했다.

```powershell
# 터미널 1: Java 21, test Profile, H2 메모리 DB
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.11"
.\gradlew.bat bootRun --args='--spring.profiles.active=test --server.address=127.0.0.1 --server.port=18091 --logging.level.org.hibernate.SQL=INFO --logging.level.org.hibernate.orm.jdbc.bind=INFO --logging.level.org.hibernate.orm.jdbc.extract=INFO --logging.level.org.springframework.transaction.interceptor=INFO'

# 터미널 2: 현재 프로젝트 폴더에서 실행
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\docs\test-member-api.ps1 -BaseUrl http://127.0.0.1:18091
```

실행 정책 옵션은 해당 PowerShell 프로세스에만 적용된다.
스크립트는 새 회원 한 명을 만들고 다섯 요청을 확인하며, 기존 행을 삭제하지 않는다.
test 서버를 종료하면 그 JVM의 메모리 데이터도 사라진다.

## 실행 검증과 문제 해결

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.11"
.\gradlew.bat clean test build --console=plain
```

2026-11-10, Java 21.0.11에서 `clean test build`를 다시 실행해 `BUILD SUCCESSFUL`을 확인했다.

| 검증 | 결과 |
| --- | --- |
| 전체 자동 테스트 | 44개 통과, 실패·오류·스킵 0개 |
| MockMvc | 가입·필수값 12경우·응답 형식·오류 상태·AOP |
| 실제 HTTP 통합 테스트 | Tomcat 임의 포트 → DB 커밋 → 중복 거부 |
| 6주차 회귀 | 기존 7개 테스트 통과, 초기 데이터·Profile·롤백 보존 |
| URL 회귀 | 등록·목록·리다이렉트·조회수·없는 ID 확인 |
| 별도 실행 서버 | 2026-11-09: test Profile / 18091 / PowerShell 5개 요청 통과 후 종료 |

테스트 소스는 [src/test/java/com/ll/demo03](./src/test/java/com/ll/demo03)에 있다.
자동 테스트는 모두 test Profile을 사용하며, 서로 다른 컨텍스트의 DB가 충돌하지 않도록 새 API 테스트에는 별도 이름의 H2 메모리 DB를 지정했다.

| 문제·주의할 점 | 처리 |
| --- | --- |
| 기본 Java가 25 | JAVA_HOME을 설치된 Java 21로 지정 |
| 필드 없는 Empty 직렬화 | `@JsonValue`로 `{}` 반환, 기존 Profile 설정 보존 |
| 결과 코드만으로 HTTP 상태가 바뀌지 않음 | 예외는 ResponseEntity, 정상 반환은 Aspect |

## 헷갈리기 쉬운 부분

| 구분 | 기억할 점 |
| --- | --- |
| `@RequestBody` / `@Valid` | JSON 변환과 값 검사는 다른 단계 |
| `@NotNull` / `@NotBlank` | NotBlank는 null·빈 문자열·공백을 모두 거부 |
| return / throw | throw 이후 저장 코드는 실행하지 않고 호출자 쪽으로 예외 전달 |
| 중복 조회 / unique 제약 | Service는 친절한 오류를, DB 제약은 중복 저장 방지를 담당 |
| DTO / Entity | 외부 응답 계약과 DB 저장 구조를 분리 |
| `Ut.str.hasLength()` | 강의 구현상 공백만 있으면 false |
| `Rq.getMember()` | 여전히 1번 회원을 가정하며 실제 로그인 기능이 아님 |

[MemberController](./src/main/java/com/ll/demo03/MemberController.java)의 `/join`은 수동 검증을 비교하는 POST 예제다.
강의 초반 GET 가입은 단계별로 확인한 뒤 최종 코드에서 POST로 바꿨다.
[ArticleController](./src/main/java/com/ll/demo03/ArticleController.java)의 `/article/list`·`/article/listJson`은 HTML과 JSON 표현을 비교한다.
기존 URL 생성 GET 경로는 회귀 확인을 위해 유지했다.

비밀번호 저장은 강의의 기존 평문 방식을 유지했다. 응답에서 숨기는 것은 암호화나 해싱이 아니다.
실제 서비스에는 비밀번호 해싱·HTTPS·초기 계정 정책·로그의 민감정보 관리가 필요하다.
동시 가입 충돌 처리와 운영 DB 연결도 별도 확인할 부분이다.
로그인·JWT·Spring Security·배포는 이번 범위에 포함하지 않았다.

## 다음 학습과 참고 자료

다음 인증 학습에서는 저장된 회원을 어떻게 확인하고 요청자의 신원을 연결하는지 이어서 볼 수 있다.
복습할 때는 [단계별 학습 기록](./docs/chapter08-progress.md)을 따라 정상 가입과 입력 오류의 실행 경로를 비교해 보면 된다.

- [강의 Chapter 08](https://www.slog.gg/p/13485#f) — 08-01–08-16, 강의 PDF 126–137쪽 확인
- [강사 Chapter 08 마지막 커밋 b0601d0](https://github.com/jhs512/demo03-2024/tree/b0601d0) — 이후 인증 코드는 가져오지 않음
- [5주차 기록](../J2-week05/README.md) · [6주차 기록](../J2-week06/README.md)

## 이번 주 핵심

- Controller는 HTTP 요청·응답을, Service는 가입 규칙과 트랜잭션을 맡는다.
- Repository를 통해 회원을 DB에 저장하고, 중복·필수값 오류는 저장 전에 막는다.
- Validation과 예외 핸들러로 입력 오류도 일정한 JSON 형식으로 응답한다.
- DTO에는 공개할 정보만 담고, AOP로 RsData의 상태를 실제 HTTP 응답에 반영한다.
