# 2학기 8주차 — 아이디/비밀번호를 포함시킨 인증

7주차에는 **회원가입 API**로 회원을 저장했다.
이번에는 그 회원의 아이디·비밀번호를 확인하고, **본인이 만든 단축 URL만 관리**하도록 구현했다.

- 학습 범위: **Chapter 09, 09-01~09-15** (강의 PDF 137~146쪽)
- 환경: Java 21 / Spring Boot 3.2.4 / Gradle 8.6
- 세부 과정: [Chapter 09 구현 기록](./docs/chapter09-progress.md)

## 이번에 구현한 것

| 기능 | 동작 |
| --- | --- |
| SURL 등록 | 요청한 회원을 작성자로 저장 |
| 목록 | 본인 자료만 최신 ID 순으로 조회 |
| 단건 조회·수정·삭제 | 작성자만 허용 |
| 공유 링크 | 누구나 원본 URL로 이동, 조회수 증가 |

핵심은 **회원 확인 → 소유권 확인 → SURL 처리**다.

## 1. 인증: 요청한 회원 확인

**인증은 “누가 요청했는가?”를 확인하는 과정이다.**

1. Controller가 `Rq.getMember()`를 호출한다.
2. Rq가 `actorUsername`·`actorPassword`를 읽는다.
3. `MemberService → MemberRepository`로 회원을 찾는다.
4. 저장된 비밀번호와 일치하면 해당 Member를 반환한다.

아이디만 보내거나 비밀번호가 다르면 **401**이다.
이전처럼 1번 회원을 고정해서 사용하지 않고, 실제 인증한 회원으로 작업한다.

## 2. 인가: 본인 자료인지 확인

**인가는 “이 자료를 다룰 권한이 있는가?”를 확인하는 과정이다.**
비밀번호가 맞아도 다른 회원의 SURL을 수정할 수는 없다.

단건 조회의 실행 순서는 다음과 같다.

1. **Controller → Rq:** 요청한 회원 인증
2. **SurlService → SurlRepository:** ID로 대상 SURL 조회
3. **SurlService → AuthService:** 회원 ID와 작성자 ID 비교
4. **SurlService → Controller:** SurlDto를 만들어 `data.item`으로 응답

대상이 없으면 **404**, 다른 회원의 자료이거나 작성자가 없으면 **403**이다.
수정·삭제도 대상을 먼저 찾고 권한을 검사한다.
등록은 인증한 회원을 작성자로 저장하고, 목록은 작성자 조건으로 조회한다.

코드: [Controller](./src/main/java/com/ll/demo03/ApiV1SurlController.java) · [SurlService](./src/main/java/com/ll/demo03/SurlService.java) · [AuthService](./src/main/java/com/ll/demo03/AuthService.java)

## 3. RequestScope: 같은 요청에서만 인증 결과 재사용

`Rq`는 `@RequestScope` 빈이다. **HTTP 요청마다 새 객체**를 사용한다.

| 호출 시점 | 처리 |
| --- | --- |
| 첫 getMember() | 회원 조회·비밀번호 검증 후 결과 저장 |
| 같은 요청에서 다시 호출 | 저장한 Member 반환 또는 같은 인증 예외 발생 |
| 다음 HTTP 요청 | 새 Rq에서 다시 인증 |

아래는 [Rq.getMember()](./src/main/java/com/ll/demo03/global/rq/Rq.java)의 핵심 부분이다.
파라미터 추출·필수값 검사·실패 캐시 처리는 생략했다.

```java
if (member != null) return member;
if (authenticationFailure != null) throw authenticationFailure;

Member candidate = memberService.findByUsername(username)
        .orElseThrow(() -> new GlobalException("401-3", "인증정보가 올바르지 않습니다."));
if (!password.equals(candidate.getPassword()))
    throw new GlobalException("401-4", "인증정보가 올바르지 않습니다.");
member = candidate;
return member;
```

- 비밀번호까지 일치해야 성공 캐시에 넣는다.
- 인증정보 누락·없는 회원·비밀번호 불일치도 캐시한다. 일반 DB 장애는 캐시하지 않는다.
- 캐시는 다른 요청과 공유되지 않는다. 다음 요청의 로그인 상태를 유지하는 기능은 아니다.

## 응답과 DB 처리

| 적용한 것 | 이유·동작 |
| --- | --- |
| SurlDto | 공개할 값만 응답하고 Member 객체·비밀번호 제외 |
| OSIV 비활성화 | 서비스 밖의 추가 DB 조회 없이 응답하도록 트랜잭션 안에서 DTO 완성 |
| 서비스 트랜잭션 | 조회는 읽기 전용, 등록·수정·삭제·조회수 증가는 쓰기 작업 |
| Dirty Checking | 관리 중인 엔티티의 값 변경을 DB에 반영. save 재호출 없이 수정 |

SurlDto는 ID·생성일·수정일·작성자 ID·작성자 이름(nickname)·설명·URL·조회수를 담는다.
`spring.jpa.open-in-view=false`로 설정했으며, 강의와 달리 기존 서비스 계층 트랜잭션을 유지했다.
수정은 `flush()` 후 갱신된 수정일까지 응답한다. HTTP 상태는 ResponseAspect가 반영한다.

## API 사용

기본 포트는 `8090`이다. **인증정보는 쿼리 파라미터**, 등록·수정 값은 JSON의 `body`·`url`로 보낸다.

| 메서드 | 경로 | 기능 |
| --- | --- | --- |
| POST | `/api/v1/members` | 회원가입, 인증 불필요 |
| POST | `/api/v1/surls` | 등록 |
| GET | `/api/v1/surls` | 본인 목록 |
| GET | `/api/v1/surls/{id}` | 본인 단건 조회 |
| PUT | `/api/v1/surls/{id}` | 본인 자료 수정 |
| DELETE | `/api/v1/surls/{id}` | 본인 자료 삭제 |

회원가입 성공은 **201**, SURL API 성공은 **200**이다.
등록·단건·수정 결과는 `data.item`, 목록은 `data.items`에 담는다.

**정상 예시 — 본인 목록 조회** (로컬 초기 계정, 등록한 SURL이 없는 경우)

```http
GET /api/v1/surls?actorUsername=user1&actorPassword=1234
```

```json
{"resultCode":"200-1","statusCode":200,"msg":"성공","data":{"items":[]}}
```

**오류 예시 — 비밀번호 누락**

```http
GET /api/v1/surls?actorUsername=user1
```

```json
{"resultCode":"401-2","statusCode":401,"msg":"인증정보(비밀번호)를 입력해주세요.","data":{}}
```

### 기존 경로

- `/add`, `/s/{body}/**`, `/all`에도 같은 인증 정책을 적용했다.
- `/s/**`는 원본 검색어를 보존하고, 인코딩된 이름을 포함한 인증 파라미터는 저장·응답·이동 URL에서 제거한다.
- `/g/{id}`는 공개 공유 링크다. 인증 없이 **302 이동·조회수 증가**만 처리한다.
- `/join`, `/article/list`, `/article/listJson`은 유지했다. 새 등록 요청에는 기존 GET 예제 대신 POST API를 사용한다.

## 실행과 검증

J2-week08 폴더에서 설치된 Java 21 경로를 지정해 실행한다.
IntelliJ에서는 Project SDK와 Gradle JVM을 Java 21로 맞춘다.

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.11"
.\gradlew.bat clean test build --console=plain
```

**검증 기록: 2026-11-18, Java 21.0.11에서 총 85개 통과.** 실패·오류·스킵은 0개다.
당시 `--offline --no-daemon`으로 빌드했으며, 캐시가 없는 PC에서는 기본 명령으로 의존성을 받는다.

- 인증 실패, 타인 자료 접근, 요청 간 캐시 분리, 기존 경로 우회를 검사했다.
- 비밀번호 비노출, OSIV를 끈 CRUD, 실제 HTTP 요청 후 DB 반영을 확인했다.
- DB 테스트는 test Profile·H2 메모리 DB를 사용했다. 개발·운영 DB는 변경하지 않았다.
- 테스트 결과: `build/reports/tests/test/index.html`

파일 DB 없이 앱을 실행하려면 `bootRun --args='--spring.profiles.active=test --server.address=127.0.0.1'`을 사용한다.
기본 dev 실행은 H2 파일 DB를 생성한다.

## 학습용 인증의 한계

- **비밀번호를 URL로 전달하고 평문으로 저장·비교한다.** 실제 계정이나 재사용하는 비밀번호를 넣으면 안 된다.
- DTO에서 비밀번호를 빼도 브라우저 기록·접근 로그의 노출 위험은 남는다.
- 없는 회원과 비밀번호 불일치는 메시지가 같지만 결과 코드가 달라 회원 존재 여부를 숨기지는 못한다.
- 운영용 해싱·인증 방어, URL 형식·길이 정책, 동시 조회수 증가 충돌은 해결하지 않았다.
- 쿠키 인증·Spring Security·JWT·API Key·외부 서버 배포는 이번 범위에 포함하지 않았다.

## 이번 주 핵심

- 회원가입으로 저장한 정보가 매 요청의 인증 기준이 된다.
- 인증한 회원이라도 본인 자료인지 별도로 검사한다.
- RequestScope 캐시는 같은 요청에서만 인증 결과를 재사용한다.
- DB 처리는 서비스 안에서 마치고, 응답에는 DTO만 보낸다.
