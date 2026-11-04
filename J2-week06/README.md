# 2학기 6주차 — AWS, 테라폼으로 EC2 구축하고 서비스 배포

5주차에는 Spring Data JPA로 회원·게시물·단축 URL을 MySQL에 저장했다.<br>
이번에는 PostgreSQL과 H2로 연결 대상을 바꾸고, 개발·테스트·운영 환경을 나눴다.

학습 범위: **1–10강 (07-01–07-10)**<br>
**EC2 생성과 실제 서비스 배포는 아직 미실습**이며, 11강 이후에 이어서 진행한다.

## 이번 주 한눈에 보기

| 주제 | 진행 상태 |
| --- | --- |
| PostgreSQL 연결 | 2–3강에서 직접 실습 |
| H2 파일 DB·콘솔 | 연결 실습, 별도 DB로 실행 검증 |
| Fly.io 운영 DB | 원격 DB·Proxy 개념 학습 |
| Spring Profile | 환경 분리·초기화·자동 테스트 검증 |
| 도메인·DNS | 연결 절차·자원 관리 개념 학습 |
| AWS·Terraform | IAM·API·IaC 기초 학습 |

- **환경:** Windows / Java 21 / Spring Boot 3.2.4 / Gradle 8.6
- **기본 서버 포트:** `8090`
- **현재 개발 DB:** H2 파일 DB
- **실제 검증 포트:** `18090` — 8090 사용 중으로 별도 실행

## 이전 주차와의 연결

```text
3주차  Fly.io 배포 → 재시작 후 메모리 데이터 유실
4주차  MySQL·SQL·테이블 관계 학습
5주차  JPA로 Java 객체를 DB에 저장
6주차  DB 연결 대상과 실행 환경 분리
```

1강의 전체 목표는 Terraform으로 AWS 자원을 관리하고 CI/CD를 구성하는 것이다.
이번에는 그 준비 단계인 DB·환경 분리와 인프라 기초까지 진행했다.

5주차의 Entity·Repository·Service와 `com.ll.demo03` 패키지는 유지했다.

## 1. MySQL → PostgreSQL → H2

### 같은 저장 코드, 다른 연결 대상

[build.gradle](./build.gradle)에 세 DB의 JDBC 드라이버가 등록되어 있다.

```groovy
runtimeOnly 'com.mysql:mysql-connector-j'
runtimeOnly 'org.postgresql:postgresql'
runtimeOnly 'com.h2database:h2'
```

| DB | 사용 목적 |
| --- | --- |
| MySQL | 5주차 JPA 실습의 기준 DB |
| PostgreSQL | Docker·DBeaver·Spring Boot 연결 |
| H2 파일 DB | 개발 데이터를 파일에 보관 |
| H2 메모리 DB | 개발 DB와 분리한 자동 테스트 |

드라이버가 세 개 있어도 현재 datasource가 지정한 DB 하나에 연결한다.
JPA 코드를 유지할 수 있지만, DB마다 접속 주소·자료형·SQL 동작은 확인해야 한다.

### PostgreSQL 연결 실습

기존 2–3강 실습에서는 다음 환경을 사용했다.

| 항목 | 설정 |
| --- | --- |
| 컨테이너 | `postgres_1` / PostgreSQL 17 |
| 포트 연결 | PC `15432` → 컨테이너 `5432` |
| DB | `surl_dev` |

```text
jdbc:postgresql://localhost:15432/surl_dev
```

DBeaver에서 `post`·`post_practice` 테이블을 만들고 SQL을 실행했다.
JDBC URL 오타를 고친 뒤 Spring Boot 연결과 JPA 테이블 생성도 확인했다.

H2로 변경한 뒤에는 `/h2-console`에 접속했다.
**DB 연결 대상을 바꿔도 기존 데이터가 새 DB로 자동 복사되지는 않는다.**

### Fly.io 원격 DB와 Proxy — 5강

강의에서는 원격 PostgreSQL에 Proxy로 연결하는 흐름을 다뤘다.

```text
내 PC의 DBeaver
  → 로컬 Proxy 포트 5434
  → Fly.io PostgreSQL 포트 5432
```

```bash
# 강의 예시이며 실행하지 않음
flyctl proxy 5434:5432 -a <내-DB-앱-이름>
```

Proxy는 원격 DB로 이어지는 통로이며 데이터를 복사하는 기능이 아니다.
개발용 `surl_dev`와 운영용 `surl_prod`도 별도로 관리한다.

**원격 DB 생성·Proxy 연결은 비용 문제로 미실습**이다.

## 2. Profile로 개발·테스트·운영 분리

개발이나 테스트 작업이 운영 데이터에 영향을 주지 않도록 설정을 나눴다.

| 설정 파일 | 역할 |
| --- | --- |
| [application.yml](./src/main/resources/application.yml) | 공통 설정·기본 dev·포트 8090 |
| [application-dev.yml](./src/main/resources/application-dev.yml) | H2 파일 DB·웹 콘솔 |
| [application-test.yml](./src/main/resources/application-test.yml) | H2 메모리 DB·create-drop |
| [application-prod.yml](./src/main/resources/application-prod.yml) | 운영 PostgreSQL·INFO 로그 |

```yaml
# 별도로 Profile을 지정하지 않으면 dev 사용
spring:
  profiles:
    default: dev
```

`test`나 `prod`를 명시하면 기본 `dev`는 활성화되지 않는다.
H2 웹 콘솔은 `dev`에서만 켜고 `test`·`prod`에서는 끈다.

운영 DB 접속 정보는 `PROD_DB_URL`·`PROD_DB_USERNAME`·`PROD_DB_PASSWORD` 환경변수로 받는다.
실제 접속 값은 설정 파일에 넣지 않았다.

**운영 PostgreSQL 연결은 미실습**이다.
운영의 `ddl-auto: update`는 강의 설정을 유지했으며, 실제 운영 스키마 변경은 검증하지 않았다.

H2 파일과 로컬 비밀 설정은 [.gitignore](./.gitignore)로 제외했다.

## 3. 회원을 먼저 만들고 게시물 초기화

### All → NotProd 순서

```text
Spring Boot 시작 / JPA 준비
  → initAll      @Order(3): 필요한 회원 준비
  → initNotProd  @Order(4): 샘플 게시물 생성
```

| 설정 | 적용 범위 |
| --- | --- |
| [All](./src/main/java/com/ll/demo03/All.java) | 모든 Profile에서 초기 회원 확인 |
| [NotProd](./src/main/java/com/ll/demo03/NotProd.java) | prod를 제외한 환경에서 게시물 초기화 |

```java
@Profile("!prod")
@Configuration
public class NotProd {
    // prod에서는 등록하지 않는 초기화 클래스
}
```

`!prod`에는 개발뿐 아니라 테스트도 포함된다.

- `@Bean`으로 등록한 `ApplicationRunner`가 시작 시 초기 작업을 실행한다.
- `@Order`는 **Runner 호출 순서**를 정하며 모든 Bean의 생성 순서를 정하지는 않는다.
- `self.work1()`은 Spring 프록시를 거쳐 `@Transactional`이 적용되도록 호출한다.

### 일부 회원만 있는 DB도 보존하기

기존에는 `memberService.count() > 0`이면 회원 초기화를 전부 생략했다.
회원이 한 명만 있어도 나머지 초기 회원이 빠질 수 있는 구조였다.

지금은 `system`·`admin`·`user1`·`user2`를 **username별로 확인해 없는 회원만 생성**한다.
이미 있는 회원의 ID·암호·닉네임은 바꾸지 않는다.

[MemberService](./src/main/java/com/ll/demo03/MemberService.java)의 `findByUsername()`은 public으로 열었다.
`NotProd`에서는 재가입 대신 기존 회원을 조회해 작성자로 사용한다.

```java
Member member1 = memberService.findByUsername("user1").orElseThrow();
Member member2 = memberService.findByUsername("user2").orElseThrow();
```

게시물은 `articleService.count() > 0`이면 샘플 생성을 건너뛴다.
따라서 **게시물 4개 생성은 빈 DB 기준**이며, 일부 샘플을 삭제해도 자동 보충하지 않는다.

`All`은 `prod`에도 등록된다.
실제 배포 전에는 강의용 초기 계정과 고정 예제 암호를 운영용 정책으로 바꿔야 한다.

### 테스트 DB와 로그

```java
@SpringBootTest
@ActiveProfiles("test")
class Demo03ApplicationTests {
    // 개발 파일 DB와 분리해 검증
}
```

```yaml
# application-test.yml 핵심 설정
spring:
  datasource:
    url: jdbc:h2:mem:db_test;DB_CLOSE_DELAY=-1
  jpa:
    hibernate:
      ddl-auto: create-drop
```

| 설정 | 의미 |
| --- | --- |
| `@ActiveProfiles("test")` | 테스트용 Profile 적용 |
| `mem` | 파일 대신 JVM 메모리에 저장 |
| `DB_CLOSE_DELAY=-1` | 같은 JVM 안에서 연결 종료 후에도 DB 유지 |
| `create-drop` | 컨텍스트 시작·종료 시 테스트 테이블 생성·삭제 |

[SurlController](./src/main/java/com/ll/demo03/SurlController.java)의 출력문도 `log.debug()`로 바꿨다.
`@Slf4j`가 `log` 필드를 만들어주며, 운영에서는 INFO 설정으로 DEBUG·TRACE 출력을 줄인다.

## 4. 도메인·DNS·자원 관리 — 7–8강

### 도메인 연결 흐름

```text
도메인 확보
  → DNS를 관리할 네임서버 지정
  → Fly 앱에 호스트 이름 등록
  → CNAME 등 DNS 설정
  → 인증서·HTTPS 확인
```

| 개념 | 역할 |
| --- | --- |
| 기본 도메인 | Fly.io의 `<앱 이름>.fly.dev` |
| DNS | 도메인의 접속 대상을 찾는 시스템 |
| 네임서버 | 도메인의 DNS 정보를 관리하는 서버 |
| DNSZI | 강의에서 사용한 DNS 관리 서비스 |
| 서브도메인 | `www`·`api.www`처럼 붙이는 이름 |
| CNAME | 다른 도메인 이름을 가리키는 별칭 |
| HTTPS 인증서 | 서버 이름 확인·암호화 통신에 사용 |

강의 예시는 `www.surl.oa.gg`와 `api.www.surl.oa.gg`다.
**내 서비스 주소가 아니며 아래 명령도 실행하지 않았다.**

```bash
fly certs add www.surl.oa.gg
fly certs show www.surl.oa.gg

fly certs add api.www.surl.oa.gg
fly certs show api.www.surl.oa.gg
```

CNAME 설정과 HTTPS 인증서 검증은 각각 필요하다.
실제 연결 시에는 강사의 주소를 복사하지 않고 앱이 안내하는 DNS 대상을 확인한다.
([Fly.io 공식 문서](https://fly.io/docs/networking/custom-domain/))

### 무중단 배포·자원 해제

무중단 배포는 새 버전의 정상 여부를 확인하면서 요청을 전환하는 방식이다.
머신 수·상태 확인·배포 전략이 맞아야 하며, 이번에는 요청 연속성을 검증하지 않았다.

자원 해제는 **대상 확인 → 데이터 보관 확인 → 해제 → 남은 자원·청구 확인** 순서로 정리했다.
앱 하나를 삭제해도 별도 DB·볼륨까지 모두 정리됐다고 볼 수는 없다.

**도메인 구매·DNS 변경·인증서 발급·배포·자원 삭제는 모두 미실습**이다.

## 5. AWS·IAM·Terraform — 9–10강

### 서비스와 권한 구분

| 용어 | 역할 |
| --- | --- |
| AWS | 서버·네트워크 등 클라우드 인프라 제공 |
| EC2 | 가상 서버 실행 |
| VPC | 가상 네트워크 구성 |
| IAM | 사용자·역할의 접근 권한 관리 |
| IAM 정책 | 허용·거부할 작업과 대상 정의 |
| 계정 별칭 | IAM 로그인 주소에 쓸 계정 이름 |

9강은 다음 절차를 소개한다.

```text
계정 가입 → 콘솔 접속 → IAM 사용자 생성
  → AdministratorAccess 연결
  → 계정 별칭 지정 → IAM 사용자 로그인
```

루트 사용자는 계정의 최상위 사용자이고, IAM 사용자는 권한을 부여받는 사용자다.
`AdministratorAccess`를 연결해도 루트 전용 작업까지 같아지지는 않는다.

강의 예제와 구분해 확인한 현재 권장사항은 **루트 보호·MFA·최소 권한·임시 자격 증명**이다.
사람은 IAM Identity Center 등의 연동을, 워크로드는 IAM 역할을 우선한다.
([AWS 공식 안내](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html))

**계정 가입·IAM 사용자 생성·정책 연결·로그인은 미실습**이다.

### Console·CLI·Terraform과 API

```text
AWS Console ─────────────┐
AWS CLI ────────────────┼→ AWS API → EC2 / VPC 등
Terraform → AWS Provider┘
```

| 도구 | 관리 방식 |
| --- | --- |
| AWS Console | 웹 화면에서 조작 |
| AWS CLI | 터미널 명령어로 요청 |
| Terraform | 원하는 인프라 상태를 코드로 선언 |

Terraform은 **AWS Provider를 통해 API와 통신**한다.
AWS CLI를 반드시 실행해야 작동하는 구조는 아니다.

인프라를 코드로 관리하는 방식을 **IaC**(Infrastructure as Code)라고 한다.
([HashiCorp 공식 문서](https://developer.hashicorp.com/terraform/language/providers))

## 6. 문제 해결과 실행 검증

### 확인한 문제

| 문제 | 처리 |
| --- | --- |
| PostgreSQL JDBC URL 오타 | 기존 실습에서 수정 후 연결·테이블 생성 확인 |
| 일부 초기 회원 누락 | username별로 없는 회원만 생성 |
| 회원 중복 가입 위험 | NotProd에서 기존 회원 조회 |
| 테스트가 개발 DB를 쓸 수 있음 | test Profile·메모리 H2로 격리 |
| 검증 서버의 8090 포트 사용 중 | 기본 설정은 유지하고 18090으로 검증 |

[Rq.getMember()](./src/main/java/com/ll/demo03/global/rq/Rq.java)는 여전히 `getReferenceById(1L)`을 사용한다.
현재 로그인 기능이 아니라 1번 회원을 가정한 실습 코드다.
회원 생성 순서에 따라 다른 계정일 수 있어 추후 로그인 구현에서 보완해야 한다.

### 빌드·테스트 결과

Java 21에서 다음 명령으로 빌드와 테스트를 실행했다.

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.11"
.\gradlew.bat clean test build --console=plain
```

| 검증 항목 | 결과 |
| --- | --- |
| Gradle build | 성공 |
| 자동 테스트 | 7개 통과, 실패·오류·스킵 0개 |
| 테스트 DB | H2 메모리 DB·JPA 테이블 3개 확인 |
| 초기 데이터 | 빈 DB에서 회원 4명·게시물 4개 생성 |
| 기존 데이터 | 일부 회원·기존 게시물 보존 |
| 반복 초기화 | 회원·게시물 중복 생성 없음 |
| 트랜잭션 | 예외 시 회원·게시물 함께 롤백 |
| prod 조건 | DB 접속 없이 NotProd 제외·설정 확인 |

테스트 코드는 다음 두 파일에 있다.

- [Demo03ApplicationTests](./src/test/java/com/ll/demo03/Demo03ApplicationTests.java)
- [ProdProfileTests](./src/test/java/com/ll/demo03/ProdProfileTests.java)

### 별도 H2 파일로 개발 실행

기존 `db_dev.mv.db`는 다른 프로세스가 사용 중이었다.
파일을 삭제하거나 잠금을 해제하지 않고 별도 검증 DB를 사용했다.

| 구분 | 사용 값 |
| --- | --- |
| 기본 개발 DB | `jdbc:h2:file:./db_dev` |
| 검증 DB | `jdbc:h2:file:./.gradle/verification/db_dev_check` |
| 검증 포트 | `18090` |

- 기본 `dev` 적용과 H2 파일 연결을 확인했다.
- `/all`·`/h2-console/`·`/add`는 HTTP 200으로 응답했다.
- 재시작 후 회원 4명·게시물 4개·등록 URL 1개가 유지됐다.
- 재시작 때 초기 데이터가 다시 삽입되지 않았다.

위 결과는 **별도 검증 DB 기준**이다.
기존 개발 DB를 그대로 실행해 검증한 것이 아니며, 운영 PostgreSQL 연결도 아직 확인하지 않았다.

## 이번 주 핵심

- DB 연결을 바꿔도 데이터가 자동으로 옮겨지지는 않는다.
- Profile은 환경별 설정을, `@Profile`은 Bean 등록 범위를 나눈다.
- 회원을 먼저 준비해야 작성자 있는 게시물을 만들 수 있다.
- CNAME 연결과 HTTPS 인증서 검증은 별도 작업이다.
- Console·CLI·Terraform의 요청은 AWS API로 이어진다.

## 다음에 해볼 것

11강 AWS CLI 설치 → 12강 Terraform 설치 → 13강 EC2 생성 순서로 이어서 진행한다.

**CLI·Terraform 설치·EC2 구축·실제 서비스 배포는 아직 하지 않았다.**
이후 11–20강의 실습과 검증 결과를 이 README에 추가한다.

## 참고 자료

- [강의 Chapter 07](https://www.slog.gg/p/13485#f) — 07-01–07-10, PDF 93–105쪽
- [강사 GitHub — Fly.io 단계](https://github.com/jhs512/demo03-2024/tree/chapter-07-fly.io)
- [강사 GitHub — AWS 단계](https://github.com/jhs512/demo03-2024/tree/chapter-07-aws)
- [5주차 학습 기록](../J2-week05/README.md)
