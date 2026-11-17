# 2학기 8주차 — 아이디/비밀번호를 포함시킨 인증

7주차에는 회원가입 요청을 받아 회원을 DB에 저장했다.<br>
이번에는 그 회원의 아이디와 비밀번호를 매 요청에서 확인하고, 본인이 만든 단축 URL만 관리하도록 연결했다.

학습 범위: **Chapter 09, 09-01–09-15**<br>
강의 PDF 137–146쪽과 연결된 강사 커밋 `9f1a706`까지를 기준으로 구현했다.

## 이번 주 한눈에 보기

| 구분 | 내용 |
| --- | --- |
| 환경 | Java 21 / Spring Boot 3.2.4 / Gradle 8.6 |
| SURL REST API | 등록·단건 조회·목록·수정·삭제 |
| 인증 | actorUsername·actorPassword로 매 요청의 회원 확인 |
| 인가 | 작성자만 단건 조회·수정·삭제, 목록도 본인 자료만 반환 |
| 응답 | Entity 대신 SurlDto, 기존 RsData 응답 형식 유지 |
| DB 접근 | OSIV 비활성화, 서비스 트랜잭션 안에서 DTO 변환 |
| 캐싱 | RequestScope Rq에 인증 성공·실패 결과 보관 |
| 검증 | 기존 44개 + 신규 41개 = 85개 통과, 실패·오류·스킵 0개 |

[단계별 구현 기록](./docs/chapter09-progress.md)에 15단계와 대응 파일을 정리했다.

## 7주차 회원가입에서 이어진 부분

`POST /api/v1/members`의 회원가입과 `/join`의 폼 가입 예제는 유지했다.
가입한 회원은 이후 SURL 요청에 자신의 아이디와 비밀번호를 전달한다. 가입했다고 다음 요청까지 로그인 상태가 남는 구조는 아니다.

```text
7주차: 가입 JSON → MemberService.join() → MemberRepository → 회원 저장
8주차: 요청 파라미터 → Rq.getMember() → 회원 조회·비밀번호 확인
                       → SurlService → AuthService의 소유권 검사 → SURL 처리
```

`Rq.getMember()`가 항상 1번 회원의 프록시를 반환하던 코드는 제거했다.
Member, MemberRepository, 기존 가입 DTO, 예외 처리와 ResponseAspect는 활용한다.
패키지도 기존 `com.ll.demo03` 구조를 유지했다.

## 요청부터 DB까지

```text
HTTP 요청 + 인증 파라미터
  → ApiV1SurlController: JSON 매핑·Validation, Rq 호출
  → Rq.getMember(): 회원 조회·비밀번호 확인, 요청 범위 캐싱
  → SurlService: 트랜잭션 시작, 대상 조회
  → AuthService: 현재 회원과 작성자의 ID 비교
  → SurlRepository → JPA / Hibernate → H2
  → 서비스 안에서 SurlDto 생성, 트랜잭션 종료
  → Controller: item 또는 items로 응답 구성
  → ResponseAspect: RsData의 상태를 HTTP 응답에 반영
```

| 클래스 | 맡은 일 |
| --- | --- |
| [ApiV1SurlController](./src/main/java/com/ll/demo03/ApiV1SurlController.java) | REST 경로, 요청·응답 record, 입력 검증 |
| [SurlService](./src/main/java/com/ll/demo03/SurlService.java) | CRUD 흐름·트랜잭션·DTO 변환·조회수 변경 |
| [SurlRepository](./src/main/java/com/ll/demo03/SurlRepository.java) | JPA 저장·조회, 작성자별 ID 역순 목록 |
| [Surl](./src/main/java/com/ll/demo03/Surl.java) | 작성자·설명·URL·조회수와 JPA 매핑 |
| [SurlDto](./src/main/java/com/ll/demo03/SurlDto.java) | 공개할 값만 복사한 응답 데이터 |
| [Rq](./src/main/java/com/ll/demo03/global/rq/Rq.java) | 요청 파라미터 인증과 요청별 캐시 |
| [AuthService](./src/main/java/com/ll/demo03/AuthService.java) | 단건 조회·수정·삭제의 공통 소유권 검사 |
| [SurlController](./src/main/java/com/ll/demo03/SurlController.java) | 기존 URL 경로 유지, 같은 인증 정책 적용 |

Controller는 DB 객체를 직접 수정하지 않는다. ID와 입력값을 서비스에 전달하고, 서비스가 같은 트랜잭션 안에서 조회·권한 검사·변경을 수행한다.

## Entity를 그대로 응답하지 않는 이유

Entity에는 DB 관계와 내부 메서드가 있다. 그대로 응답하면 컬럼이나 관계 변경이 API 응답에도 영향을 줄 수 있다.
`@JsonIgnore`만으로 숨기는 대신 공개할 필드를 DTO에 정했다.

```java
public record SurlDto(Long id, LocalDateTime createDate, LocalDateTime modifyDate,
                      Long authorId, String authorName, String body, String url, long count) {}
```

실제 생성자는 Surl을 받아 위 값들을 복사한다. `authorName`에는 기존 회원의 `nickname`을 사용한다.
Member 객체와 비밀번호는 응답에 넣지 않는다. 가입 응답도 기존 MemberJoinRespBody를 사용한다.
DTO는 작성자가 없는 옛 데이터를 처리할 수 있지만, 작성자 없는 SURL에 소유권을 부여하지는 않는다.

## OSIV와 트랜잭션

```yaml
spring:
  jpa:
    open-in-view: false
```

OSIV는 웹 요청 처리 동안 영속성 컨텍스트를 열어 두는 기능이다. 켜져 있으면 서비스 밖에서도 지연 로딩이 가능하지만 DB 접근 범위가 웹 계층까지 넓어진다.
DB 연결을 얻는 정확한 시점은 설정과 쿼리 실행에 따라 달라지므로, 요청 시작부터 무조건 연결을 점유한다고 이해하지는 않았다.

강의는 Controller에 트랜잭션을 둔다. 이 프로젝트는 이전 주차부터 서비스에 트랜잭션을 두었으므로 그 구조를 이어 갔다.
조회·권한 검사·DTO 변환을 서비스 안에서 마치면 Controller에서 지연 로딩을 할 필요가 없다.
`Surl.author`는 LAZY로 설정했고, 목록은 `@EntityGraph`로 작성자를 함께 조회해 반복적인 작성자 조회를 줄였다.

| 작업 | 트랜잭션 |
| --- | --- |
| 단건·목록 조회 | 클래스의 `@Transactional(readOnly = true)` |
| 등록·수정·삭제 | 메서드의 `@Transactional` |
| `/g/{id}` 이동 | 같은 쓰기 트랜잭션에서 조회와 조회수 증가 |

수정은 관리 중인 엔티티의 body·url을 바꾼다. `save()`를 다시 호출하지 않아도 Dirty Checking으로 UPDATE가 반영된다.
수정 응답에 Auditing의 갱신된 수정일까지 담기 위해 `flush()` 후 DTO를 만든다. flush는 SQL 반영 시점이고 최종 커밋은 서비스 종료 시점이다.
리다이렉트도 조회와 증가를 하나의 서비스 메서드로 묶었다. 분리된 트랜잭션에서 조회한 객체를 나중에 변경하면 OSIV를 끈 환경에서 DB에 반영되지 않을 수 있기 때문이다.

## 인증과 인가는 다르다

- **인증:** 요청자가 누구인지 확인한다. Rq가 아이디로 회원을 찾고 비밀번호를 비교한다.
- **인가:** 인증된 회원이 이 SURL을 다룰 수 있는지 확인한다. AuthService가 작성자 ID와 요청자 ID를 비교한다.

`checkCanGetSurl()`, `checkCanModifySurl()`, `checkCanDeleteSurl()`을 서비스에서 호출한다.
비밀번호가 맞아도 남의 SURL이면 403이다. 단건 관리 조회도 작성자에게만 허용하며, 목록은 `findByAuthorOrderByIdDesc()`로 자신의 자료만 최신순으로 가져온다.

| 상황 | HTTP | resultCode |
| --- | ---: | --- |
| 아이디 누락·공백 | 401 | `401-1` |
| 비밀번호 누락·공백, 아이디만 전달 | 401 | `401-2` |
| 없는 회원 | 401 | `401-3` |
| 잘못된 비밀번호 | 401 | `401-4` |
| 다른 회원의 자료, 작성자 없는 자료 | 403 | `403-1` |
| 인증 후 없는 SURL 조회·수정·삭제 | 404 | `404-0` |
| body·url 필수값 오류 | 400 | `400-NotBlank` |
| 잘못된 JSON | 400 | `400-JSON` |

강사 코드는 없는 회원·비밀번호 불일치에도 403을 사용하지만, 여기서는 인증 실패(401)와 소유권 부족(403)을 구분했다.
두 인증 실패의 메시지는 `인증정보가 올바르지 않습니다.`로 통일했다. 다만 결과 코드가 달라 회원 존재 여부까지 숨기는 설계는 아니다.
전역 예외 처리와 ResponseAspect가 JSON의 statusCode와 실제 HTTP 상태를 맞춘다.

## Rq와 요청 범위 캐싱

Rq는 `@RequestScope` 빈이다. 같은 HTTP 요청에서는 같은 Rq를 사용하고, 다음 요청에서는 새 객체를 사용한다.

```text
getMember() 호출
  → 성공 캐시가 있으면 같은 Member 반환
  → 인증 실패 캐시가 있으면 같은 예외 전달
  → actorUsername·actorPassword 필수값 확인
  → MemberService.findByUsername()
  → 저장된 비밀번호와 비교
  → 성공한 Member 또는 인증 실패를 이 요청에 보관
```

회원이 존재한다는 이유만으로 통과하지 않는다. 비밀번호를 확인한 뒤에만 성공 캐시에 넣는다.
누락·없는 회원·비밀번호 불일치도 캐시해 같은 요청에서 반복 확인하지 않는다.
DB 장애 같은 일반 예외는 인증 실패로 저장하지 않는다. 전역·static 캐시나 세션을 사용하지 않으므로 다른 회원이나 다음 요청과 인증 상태를 공유하지 않는다.

## API 요청·응답

기본 포트는 8090이다. 아래 아이디와 비밀번호는 초기 데이터의 **로컬 학습용 예시**다.
인증정보는 JSON 본문이 아니라 쿼리 파라미터로 전달한다. 등록·수정 본문에는 body와 url을 넣는다.

| 메서드 | 경로 | 기능 | 성공 |
| --- | --- | --- | ---: |
| POST | `/api/v1/members` | 회원가입, 인증 불필요 | 201 |
| POST | `/api/v1/surls` | SURL 등록 | 200 |
| GET | `/api/v1/surls/{id}` | 본인 SURL 단건 조회 | 200 |
| GET | `/api/v1/surls` | 본인 SURL 목록 | 200 |
| PUT | `/api/v1/surls/{id}` | 본인 SURL 수정 | 200 |
| DELETE | `/api/v1/surls/{id}` | 본인 SURL 삭제 | 200 |

SURL 등록은 강의의 SurlService.add()와 같은 200을 유지했다. 회원가입의 201과 다르다.
다음 요청의 id는 등록 응답에서 받은 값을 사용한다.

```http
POST /api/v1/surls?actorUsername=user1&actorPassword=1234
Content-Type: application/json

{"body":"스프링 문서","url":"https://spring.io/projects/spring-boot"}
```

등록 응답 형식 예시다. ID와 날짜는 실행 시점에 따라 달라진다.

```json
{
  "resultCode": "200-1",
  "statusCode": 200,
  "msg": "1번 SURL이 생성되었습니다.",
  "data": {
    "item": {
      "id": 1,
      "createDate": "2026-11-18T01:00:00",
      "modifyDate": "2026-11-18T01:00:00",
      "authorId": 3,
      "authorName": "유저 1",
      "body": "스프링 문서",
      "url": "https://spring.io/projects/spring-boot",
      "count": 0
    }
  }
}
```

```http
GET /api/v1/surls/1?actorUsername=user1&actorPassword=1234

GET /api/v1/surls?actorUsername=user1&actorPassword=1234

PUT /api/v1/surls/1?actorUsername=user1&actorPassword=1234
Content-Type: application/json

{"body":"수정한 설명","url":"https://spring.io"}

DELETE /api/v1/surls/1?actorUsername=user1&actorPassword=1234
```

단건·수정은 data.item, 목록은 data.items에 같은 SurlDto를 담는다. 빈 목록은 `[]`다.
삭제 성공은 `{"resultCode":"200-1","statusCode":200,"msg":"성공","data":{}}`이다.

아이디만 보냈을 때 응답은 다음과 같다.

```json
{"resultCode":"401-2","statusCode":401,"msg":"인증정보(비밀번호)를 입력해주세요.","data":{}}
```

### 유지한 이전 경로

| 경로 | 처리 |
| --- | --- |
| `POST /join` | 기존 폼 회원가입 |
| `GET /add?body=...&url=...&actorUsername=...&actorPassword=...` | 인증 후 등록, data에 SurlDto |
| `GET /s/{body}/**?actorUsername=...&actorPassword=...` | 인증 후 경로 뒤 원본 URL 등록, data에 SurlDto |
| `GET /all?actorUsername=...&actorPassword=...` | 본인 SurlDto 목록 배열 |
| `GET /g/{id}` | 공개 단축 링크, 302 이동과 조회수 증가 |
| `GET /article/list`, `/article/listJson` | 기존 HTML·JSON 비교 예제 |

`/g/{id}`는 공유 링크를 열기 위한 공개 경로다. 소유자만 보는 관리용 상세 API와 목적이 다르다. 관리용 DTO를 반환하거나 URL을 수정·삭제하지 않는다.
기존 등록·목록 경로에는 인증을 적용해 REST 경로의 인증 정책을 우회하지 못하게 했다.
`/s/**`의 원본 검색어는 유지하지만 인증 파라미터는 이름이 URL 인코딩된 경우까지 제거한다. 비밀번호가 목적지 URL이나 Location 헤더로 전달되지 않도록 한 변경이다.
기존 GET 등록은 호환 학습용 경로이며, 새 코드에서는 POST REST API를 사용한다.

## 실행과 검증

IntelliJ에서는 이 폴더의 build.gradle을 열고 Project SDK와 Gradle JVM을 Java 21로 지정한다.
터미널에서도 설치된 Java 21을 명시했다. 아래 명령은 J2-week08 폴더에서 실행한다.

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.11"
.\gradlew.bat clean test build --console=plain
```

로컬에서 데이터를 남기지 않고 실행하려면 test Profile을 사용한다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=test --server.address=127.0.0.1'
```

DB를 사용하는 자동 테스트는 모두 test Profile과 H2 메모리 DB를 사용한다. 컨텍스트별 DB 이름도 분리했다.
ProdProfileTests는 prod 설정·빈 조건만 검사하고 DataSource를 만들거나 운영 DB에 접속하지 않는다.
개발 H2 파일 DB와 운영 PostgreSQL은 변경하지 않았다. 기본 dev 실행은 이 프로젝트 폴더에 H2 파일을 만들므로, 일회성 확인에는 위 test 실행을 사용한다.

2026-11-18, Java 21.0.11에서 구현 전 기준 테스트 **44개 통과**를 먼저 확인했다.
구현 후 `clean test build --offline --console=plain --no-daemon`으로 **BUILD SUCCESSFUL**을 확인했다.
`--offline`은 이 PC에 이미 있는 Gradle·의존성 캐시를 활용한 옵션이다. 처음 실행하는 PC에서는 위 기본 명령으로 의존성을 받는다.

| 테스트 클래스 | 실행 개수 | 검증 내용 |
| --- | ---: | --- |
| Demo03ApplicationTests | 6 | 초기 데이터·Profile·트랜잭션 롤백 |
| MemberApiTests | 23 | 기존 가입·Validation·예외·DTO·SURL 회귀 |
| MemberHttpTests | 1 | 실제 HTTP 가입 커밋·중복 거부 |
| MemberServiceTests | 7 | 가입 필수값·중복 방어 |
| ProdProfileTests | 1 | 운영 설정과 초기화 빈 조건, DB 연결 없음 |
| UtTests | 6 | 기존 문자열 검사 |
| RqTests | 5 | 성공·실패 캐시, 비밀번호 비교 횟수, DB 장애 구분 |
| SurlApiTests | 35 | CRUD·401/403/404·DTO·OSIV·Dirty Checking·요청 분리·우회 차단 |
| SurlHttpTests | 1 | 실제 Tomcat에서 가입→인증→CRUD→공개 링크→삭제, DB 커밋 |
| **합계** | **85** | **실패 0 / 오류 0 / 스킵 0** |

기존 44개 테스트는 유지했고, 인증 필수로 바뀐 기존 SURL 회귀 호출에만 인증 파라미터를 추가했다. 신규 테스트는 41개다.
개수는 build/test-results/test/TEST-*.xml을 합산했으며 매개변수별 실행을 각각 센다.
HTML 결과는 실행 후 build/reports/tests/test/index.html에서 볼 수 있다. 빌드 결과물은 Git에 넣지 않는다.

MockMvc SURL 테스트에는 테스트 메서드의 `@Transactional`을 붙이지 않았다. 테스트 트랜잭션이 서비스 밖의 지연 로딩이나 미커밋 문제를 가리지 않도록 하기 위해서다.
SurlHttpTests는 임의 포트의 실제 Tomcat을 띄우고 Java HttpClient로 요청한다. 가입한 회원으로 CRUD를 수행하고 JDBC로 커밋된 수정·삭제·조회수를 확인한다.
Rq 단위 테스트에서는 회원 조회와 비밀번호 getter 호출 횟수를 확인한다. 테스트 전용 반복 호출 경로는 src/test에만 존재한다.
Postman이나 수동 시연 도구를 사용했다고 가정하지 않았다.

## 구현하면서 확인한 문제

| 문제 | 처리 |
| --- | --- |
| Git·Java가 기본 PATH에 없음 | 실행 경로와 Java 21 JAVA_HOME 명시 |
| Gradle 다운로드·캐시 접근 제한 | 설치된 Gradle 8.6과 기존 의존성 캐시로 offline 검증 |
| 1번 회원으로 고정되던 작성자 | Rq에서 실제 요청 회원과 비밀번호 검사 |
| OSIV를 끄면 서비스 밖 지연 로딩·변경 반영에 의존할 수 없음 | DTO 변환을 서비스 안으로 이동, 리다이렉트 조회·증가도 한 트랜잭션으로 처리 |
| `/s/**`가 인증 쿼리까지 원본 URL로 저장 | 인증 파라미터 제거, 원본 검색어 보존 테스트 추가 |
| MockMvc 문자열 URL의 이중 슬래시 정규화 | 해당 테스트에 원문 URI 전달 |
| Windows 파이프로 작성한 한글이 물음표로 저장 | UTF-8로 재작성, 문서·소스 검사와 한글 응답 메시지 검증 추가 |

강의와 다르게 선택한 부분은 서비스 계층 트랜잭션, 인증 실패의 401 구분, 실패 캐싱, 이전 경로의 인증 보강이다.
기존 코드와 이번 주 정책을 맞추기 위한 변경이며, 별도 인증 프레임워크는 추가하지 않았다.

## 교육용 인증의 한계

이 방식은 **로컬 학습용**이다. 쿼리의 비밀번호는 브라우저 기록이나 프록시·접근 로그 등에 남을 수 있다.
예외 응답에는 쿼리를 넣지 않고 Hibernate 바인딩·추출 값 로그도 껐지만, URL로 비밀번호를 보내는 방식 자체가 안전해지는 것은 아니다.

비밀번호도 기존처럼 평문 저장·비교를 유지했다. DTO와 JsonIgnore는 응답 노출을 막을 뿐 해싱이나 암호화가 아니다.
실제 비밀번호와 개인 계정은 사용하지 않았다. 초기 계정의 예시 비밀번호를 운영 계정에 사용해서는 안 된다.
쿠키 인증, Spring Security, JWT, API Key는 이번 범위에 넣지 않았고 외부 서버·AWS에 배포하지 않았다.
URL 형식·길이 정책, 동시 조회수 증가 충돌, 운영용 비밀번호 해싱과 인증 방어는 이번 구현이 해결한 부분이 아니다.

## 이번 주 핵심

- 회원가입으로 저장한 회원을 매 요청의 인증정보와 연결했다.
- 인증이 성공해도 소유권 검사는 별도로 필요하다.
- DTO와 서비스 트랜잭션으로 DB 객체와 외부 응답을 분리했다.
- Dirty Checking은 쓰기 트랜잭션 안의 관리 엔티티에서 동작한다.
- RequestScope 캐시는 같은 요청의 중복 인증을 줄이지만 다음 요청을 로그인시키지는 않는다.

## 참고 자료

- [강의 Chapter 09](https://www.slog.gg/p/13485#f): 로컬 강의 PDF 137–146쪽의 설명과 Git 링크 확인
- [강사 Chapter 09 마지막 커밋](https://github.com/jhs512/demo03-2024/tree/9f1a706): API·DTO·소유권·Rq 인증·캐시 대조
- [7주차 기록](../J2-week07/README.md) · [6주차 기록](../J2-week06/README.md) · [5주차 기록](../J2-week05/README.md)
