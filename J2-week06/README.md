# 2학기 6주차 — AWS, 테라폼으로 EC2 구축하고 서비스 배포

5주차에는 Spring Data JPA로 회원·게시물·단축 URL을 MySQL에 저장했다. 이번에는 같은 Spring Boot 코드를 PostgreSQL과 H2에 연결해 보고, 개발·테스트·운영 환경에 따라 DB와 초기 데이터를 다르게 사용하는 방법을 배웠다.

학습은 [Chapter 07](https://www.slog.gg/p/13485#f)의 **1~10강(07-01~07-10)** 범위까지 진행했다. 주차 제목의 **EC2 구축과 실제 서비스 배포는 아직 진행하지 않았다.** 이 부분은 11강 이후에 이어서 실습할 예정이다.

1강에서 소개한 챕터 전체 목표는 Terraform으로 AWS 자원을 관리하고 CI/CD를 구성하는 것이다. 이번에는 그 준비 단계인 DB·환경 분리와 인프라 기초까지 정리했다.

## 이번 주 한눈에 보기

| 구분 | 배운 내용과 진행 상태 |
| --- | --- |
| PostgreSQL (2~3강) | 기존에 Docker·DBeaver·Spring Boot 연결 **실습 완료**, 이번 점검에서는 재접속하지 않음 |
| H2 (4강) | 파일 DB 연결과 H2 콘솔 접속 **완료** |
| Fly.io 운영 DB (5강) | 로컬 DB와 원격 DB, 프록시 구조 **개념 학습** |
| Spring Profile (6강) | `dev`·`test`·`prod` 분리, 초기 데이터·자동 테스트 **로컬 검증** |
| 도메인·DNS (7~8강) | CNAME·HTTPS·도메인 추가·자원 해제 **개념 학습** |
| AWS·Terraform (9~10강) | IAM·EC2·VPC·AWS CLI·IaC **개념 학습** |

실습 환경은 **Java 21 / Spring Boot 3.2.4 / Gradle 8.6 / Spring Data JPA**다. 기본 서버 포트는 `8090`이고, PostgreSQL은 Docker의 `postgres_1` 컨테이너를 로컬 `15432` 포트로 연결했다.

## 이전 주차와의 연결

```text
3주차: Fly.io 배포 → 재시작 후 ArrayList 데이터가 사라지는 문제
4주차: MySQL로 SQL과 테이블 구조 학습
5주차: JPA로 Java 객체를 DB에 저장
6주차: 사용할 DB와 실행 환경을 나누고, 운영 인프라의 기본 구조 학습
```

5주차의 Entity·Repository·Service와 `com.ll.demo03` 패키지를 유지했다. 이번에는 저장 기능을 다시 만드는 대신, 연결할 DB와 환경별 초기화 설정을 바꿨다.

## MySQL → PostgreSQL → H2

MySQL에서 사용한 JPA 저장 코드를 유지하면서 PostgreSQL과 H2로 연결 대상을 바꿨다. DBMS마다 드라이버와 접속 주소가 다르고, 자료형이나 SQL 동작도 확인해야 한다.

[build.gradle](./build.gradle)에는 세 DB의 JDBC 드라이버를 추가했다.

```groovy
runtimeOnly 'com.mysql:mysql-connector-j'
runtimeOnly 'org.postgresql:postgresql'
runtimeOnly 'com.h2database:h2'
```

| DB | 이번 실습에서의 역할 | 확인한 점 |
| --- | --- | --- |
| MySQL | 5주차 기준 DB | 기존 JPA 코드와 회원·게시물 저장 |
| PostgreSQL | Docker에서 실행하는 DB | 포트 `15432:5432`, DB `surl_dev` |
| H2 파일 DB | 현재 개발용 DB | 별도 서버 없이 실행하고 파일에 데이터 저장 |
| H2 메모리 DB | 자동 테스트용 DB | 개발 DB를 건드리지 않고 테스트 가능 |

기존 PostgreSQL 실습에서는 DBeaver로 `post`, `post_practice` 테이블을 만들고 SQL을 실행했다. JDBC URL 오타를 고쳐 `jdbc:postgresql://localhost:15432/surl_dev`로 연결한 뒤 JPA 테이블 생성도 확인했다.

`15432:5432`는 PC의 15432 포트를 컨테이너 내부의 5432 포트로 연결한다는 뜻이다. PC의 Spring Boot와 DBeaver에서는 15432로 접속한다. 드라이버가 세 개 등록되어 있어도 현재 datasource가 지정한 DB 하나를 사용한다.

H2로 바꾼 뒤에는 `/h2-console`에 접속했다. **DB 연결을 바꾼다고 MySQL·PostgreSQL의 데이터가 H2로 자동 복사되는 것은 아니다.**

## Fly.io의 원격 DB는 어떻게 연결할까? (5강)

로컬 PostgreSQL은 내 PC의 `localhost:15432`로 접속했다. 운영에서는 개발 DB와 분리된 DB가 필요하며, 강의에서는 Fly.io에 PostgreSQL 앱을 만든 뒤 Proxy로 접속하는 흐름을 소개했다.

```text
내 PC의 DBeaver → 로컬 Proxy 포트 5434 → Fly.io PostgreSQL 5432
```

```bash
# 강의 예시 명령어. 실제 실행하지 않음
flyctl proxy 5434:5432 -a <내-DB-앱-이름>
```

프록시는 **원격 DB로 이어지는 통로**이지 DB 데이터를 내 PC로 복사하는 기능이 아니다. 개발용 `surl_dev`와 운영용 `surl_prod`도 별도로 관리한다. **이번에는 비용 문제로 원격 DB를 만들지 않고 연결 구조만 공부했다.**

## 개발·테스트·운영 환경 분리

실제 서비스의 데이터와 연습 데이터를 같은 DB에서 관리하면 개발 중 실수가 사용자 데이터에 영향을 줄 수 있다. 그래서 Spring Boot의 **Profile**로 실행 환경을 나눴다.

| 파일 | 역할 |
| --- | --- |
| [application.yml](./src/main/resources/application.yml) | 공통 JPA·로그 설정, 기본 프로파일 `dev`, 포트 `8090` |
| [application-dev.yml](./src/main/resources/application-dev.yml) | 파일 기반 H2, 개발용 H2 콘솔 |
| [application-test.yml](./src/main/resources/application-test.yml) | 메모리 H2, 테스트 후 테이블 정리 |
| [application-prod.yml](./src/main/resources/application-prod.yml) | PostgreSQL 접속 정보, 운영용 로그 설정 |

```yaml
# application.yml: 프로파일을 따로 지정하지 않으면 dev 사용
spring:
  profiles:
    default: dev
```

```yaml
# application-prod.yml: 운영 DB 정보는 환경변수에서 가져옴
spring:
  datasource:
    url: ${PROD_DB_URL}
    username: ${PROD_DB_USERNAME}
    password: ${PROD_DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
```

`prod`를 지정해 실행하면 운영용 설정이 적용된다. **현재 운영 PostgreSQL은 만들지 않았으므로 실제 `prod` 서버 연결은 테스트하지 않았다.** 운영 DB 비밀번호도 Git에 올리지 않도록 환경변수로 분리했다.

`test`나 `prod`를 명시하면 기본 `dev`는 활성화되지 않는다. 개발용 H2 콘솔은 `dev`에서만 켜고, `test`·`prod`에서는 끈다. 운영의 `ddl-auto: update`는 강의 설정을 유지했으며 실제 운영 스키마 변경은 검증하지 않았다. H2 파일과 로컬 비밀 설정은 [.gitignore](./.gitignore)로 제외했다.

### `All`과 `NotProd`는 왜 나눴을까?

회원은 게시물을 쓰기 전에 먼저 존재해야 한다. 그래서 Spring Boot가 시작될 때 초기화 작업의 **실행 순서**를 정했다.

```text
Spring Boot 시작
  → All.initAll()       @Order(3): 필요한 기본 회원 생성
  → NotProd.initNotProd() @Order(4): 기존 회원을 찾아 샘플 게시물 생성
```

[All.java](./src/main/java/com/ll/demo03/All.java)는 모든 프로파일에 등록된다. 반면 [NotProd.java](./src/main/java/com/ll/demo03/NotProd.java)는 아래 설정 때문에 운영 환경에서는 등록되지 않는다. `!prod`는 개발만 뜻하는 것이 아니라 테스트 환경도 포함한다.

```java
@Profile("!prod")
@Configuration
public class NotProd {
    // 운영 환경에서는 등록하지 않는 초기화 클래스
}
```

- `@Bean`으로 등록한 `ApplicationRunner`는 Spring Boot 시작 과정에서 초기 작업을 실행한다.
- `@Order(3)`과 `@Order(4)`는 **Runner 실행 순서**를 정한다. 모든 Spring 객체의 생성 순서를 정하는 것은 아니다.
- `@Transactional`은 회원·게시물 저장 작업을 트랜잭션으로 묶는다. 강사 코드의 `self.work1()`은 Spring 프록시를 거쳐 이 기능을 적용하기 위한 호출 방식이다.

기존 `All`은 `memberService.count() > 0`이면 전체 초기화를 건너뛰었다. 회원이 한 명만 있어도 나머지 회원이 생성되지 않는 문제가 있어서, **username별로 존재 여부를 확인하고 없는 회원만 생성**하도록 보완했다.

```java
private void joinIfMissing(String username, String nickname) {
    if (memberService.findByUsername(username).isEmpty()) {
        memberService.join(username, "1234", nickname);
    }
}
```

여기서 `1234`는 강의의 예제 비밀번호다. 실제 운영 서비스에서는 고정된 관리자 비밀번호를 사용하면 안 된다. 특히 `All`은 `prod`에도 등록되므로, **실제 운영 배포 전에는 초기 계정과 비밀번호 정책을 별도로 설계해야 한다.**

[MemberService](./src/main/java/com/ll/demo03/MemberService.java)의 `findByUsername()`은 초기화 클래스에서도 호출하도록 public으로 열었다. `NotProd`는 회원을 다시 가입시키지 않고 조회해 작성자로 사용한다.

```java
Member member1 = memberService.findByUsername("user1").orElseThrow();
Member member2 = memberService.findByUsername("user2").orElseThrow();
```

기존 게시물을 보존하기 위해 `articleService.count() > 0`이면 샘플 생성을 건너뛰는 조건은 유지했다. 따라서 게시물 4개 생성은 빈 DB 기준이며, 일부 샘플을 삭제했다고 자동으로 채워주지는 않는다.

### 테스트는 왜 H2 메모리 DB로 할까?

테스트를 실행할 때 개발 DB에 저장된 데이터를 지우거나 바꾸지 않기 위해서다.

```java
@SpringBootTest
@ActiveProfiles("test")
class Demo03ApplicationTests {
    // 테스트 코드
}
```

```yaml
# application-test.yml의 핵심 설정
spring:
  datasource:
    url: jdbc:h2:mem:db_test;DB_CLOSE_DELAY=-1
  jpa:
    hibernate:
      ddl-auto: create-drop
```

`@ActiveProfiles("test")`는 테스트용 설정을 적용하고, `mem`은 데이터를 메모리에 저장한다는 뜻이다. `create-drop`은 **테스트 DB의 테이블**을 생성하고 테스트 컨텍스트가 종료될 때 삭제하는 설정이다.

`DB_CLOSE_DELAY=-1`은 같은 JVM 안에서 연결이 잠시 끊겨도 메모리 DB를 유지하는 옵션이며 파일 저장을 뜻하지 않는다.

`@Slf4j`는 로그에 사용하는 `log` 필드를 만들어준다. [SurlController](./src/main/java/com/ll/demo03/SurlController.java)의 `System.out.println()`도 `log.debug()`로 바꿨다. 개발에서는 SQL·트랜잭션 흐름을 보고, 운영에서는 애플리케이션과 Hibernate·트랜잭션 로그를 INFO로 덮어써 DEBUG·TRACE 출력을 줄인다.

## Fly.io 도메인과 DNS (7~8강)

서버에 프로그램을 배포해도 사용자가 접속할 주소를 연결해야 한다. 강의에서는 Fly.io 앱에 도메인을 붙이고 DNSZI에서 CNAME을 설정하는 과정을 다뤘다.

```text
사용자가 입력하는 도메인
  → DNS에서 접속 대상 확인 (CNAME 등)
  → Fly.io 애플리케이션
  → Spring Boot 서버
```

| 개념 | 이해한 내용 |
| --- | --- |
| 도메인 | 사람이 기억하기 쉬운 인터넷 주소 |
| 기본 도메인 | Fly.io 앱에 제공되는 `<앱 이름>.fly.dev` 주소 |
| DNS | 도메인의 접속 대상을 찾게 해주는 시스템 |
| 네임서버 | 해당 도메인의 DNS 정보를 관리하는 서버 |
| DNSZI | 강의에서 DNS 레코드를 관리한 서비스 |
| 서브도메인 | 도메인 아래에 붙이는 이름. `www`, `api.www` 등 |
| CNAME | 도메인 이름이 다른 도메인 이름을 가리키게 하는 레코드 |
| HTTPS 인증서 | 접속한 서버의 이름 확인과 암호화 통신에 사용 |

강의 예제에서는 `www.surl.oa.gg`와 `api.www.surl.oa.gg`를 Fly.io 앱에 연결한다. **두 주소는 강사 예시이며 내 서비스 주소가 아니다.** 연결 순서는 도메인 확보 → DNSZI로 관리할 네임서버 지정 → Fly 앱에 호스트 등록 → CNAME 설정 → DNS 전파·인증서·HTTPS 확인이다.

```bash
# 강의 예시: 실제 실행하지 않음
fly certs add www.surl.oa.gg
fly certs show www.surl.oa.gg
fly certs add api.www.surl.oa.gg
fly certs show api.www.surl.oa.gg
```

**CNAME을 추가했다고 HTTPS가 자동으로 준비되는 것은 아니다.** 앱에서 요구하는 DNS·인증서 검증 절차도 필요하다. 현재 Fly.io는 앱별로 안내하는 CNAME 대상을 확인하도록 설명하므로 강사의 주소를 그대로 복사해서 사용하지 않는다. ([Fly.io 공식 문서](https://fly.io/docs/networking/custom-domain/))

8강에서는 하나의 앱에 도메인을 더 연결할 수 있다는 점과 무중단 배포·자원 해제 개념을 배웠다. 무중단 배포는 새 버전의 정상 여부를 확인하면서 기존 인스턴스에서 요청을 전환하는 방식이다. 머신 수·상태 확인·배포 전략이 맞아야 하며, 이번에는 요청 연속성을 검증하지 않았다.

자원 해제는 대상 앱·DB 구분 → 보관할 데이터 확인 → 대상 자원 해제 → 남은 앱·머신·볼륨·DB와 청구 상태 확인 순서로 정리했다. 앱 하나를 삭제했다고 별도 DB 등 모든 자원이 정리되는 것은 아니다. **도메인 구매, DNS 변경, 인증서 발급, 실제 배포와 자원 삭제는 모두 미실습**이다.

## AWS, IAM, AWS CLI와 Terraform (9~10강)

이제 서버를 어디에 올리고 어떻게 관리할지 배우기 시작했다.

| 용어 | 쉽게 정리한 의미 |
| --- | --- |
| AWS | 인터넷으로 서버·네트워크 등의 인프라를 제공하는 클라우드 |
| EC2 | AWS에서 실행하는 가상 서버 |
| VPC | AWS 안에서 구성하는 가상 네트워크 |
| IAM | 누가 어떤 AWS 자원에 접근하고 작업할 수 있는지 관리 |
| 루트 사용자 / IAM 사용자 | AWS 계정의 최상위 사용자 / 계정 안에서 권한을 부여받는 사용자 |
| IAM 정책 | 허용·거부할 작업과 대상 자원, 조건을 정의 |
| 계정 별칭 | IAM 로그인 주소에서 계정 ID 대신 사용하는 이름. 사용자 이름과 구분 |
| AWS Console | 웹 화면에서 AWS 관리 |
| AWS CLI | 터미널 명령어로 AWS 관리 |
| Terraform | 서버와 네트워크 등 원하는 인프라 상태를 코드로 관리 |

9강은 계정 가입 → 콘솔 접속 → IAM 사용자 생성 → `AdministratorAccess` 연결 → 계정 별칭 지정 → IAM 사용자 로그인 순서다. `AdministratorAccess`는 광범위한 관리 권한이며 루트 전용 작업까지 같아지는 것은 아니다. **이번에는 계정 가입·사용자 생성·정책 연결·로그인을 하지 않았다.**

강의 예제와 구분해 확인한 현재 AWS 권장사항은 **루트 계정 보호·MFA·최소 권한·임시 자격 증명**이다. 사람은 IAM Identity Center 등의 연동을, 워크로드는 IAM 역할을 통한 임시 자격 증명을 우선한다. ([AWS 공식 안내](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html))

10강에서 이해한 흐름은 다음과 같다.

```text
AWS Console ────────────┐
AWS CLI ────────────────┼→ AWS API → EC2 / VPC 등
Terraform → AWS Provider┘
```

**Terraform은 AWS CLI를 반드시 실행하는 프로그램이 아니다.** AWS Provider를 통해 API와 통신한다. 이처럼 인프라를 코드로 관리하는 방식을 **IaC(Infrastructure as Code)** 라고 한다. ([HashiCorp 공식 문서](https://developer.hashicorp.com/terraform/language/providers))

## 문제 해결과 실행 확인

| 문제 | 원인과 해결 |
| --- | --- |
| 기존 PostgreSQL 실습의 연결 오류 | JDBC URL 오타를 고친 뒤 연결·JPA 테이블 생성 확인. 이번 점검에서는 재접속하지 않음 |
| 초기 회원이 일부만 있을 때 나머지가 누락될 수 있음 | 전체 회원 수 대신 username별 존재 여부 확인 |
| 샘플 게시물을 생성하면서 회원을 다시 가입시킬 위험 | `All`에서 회원 준비 → `NotProd`에서는 기존 회원 조회 |
| 테스트가 개발 DB를 사용할 수 있음 | `test` 프로파일과 메모리 H2 분리 |
| 8090 포트 사용 중으로 검증 서버 시작 실패 | 프로젝트 기본 포트는 유지하고 별도 검증 서버만 18090 사용 |
| 현재 요청 사용자를 1번 회원으로 가정 | `Rq.getMember()`가 `getReferenceById(1L)` 사용. 회원 생성 순서에 따라 다른 계정을 가리킬 수 있어 추후 로그인 방식 개선 필요 |

Java 21에서 Gradle 빌드와 자동 테스트 7개가 통과했다. H2 메모리 DB에서 초기화·기존 데이터 보존·트랜잭션 롤백을 확인하고, 별도 파일 DB에서는 개발 서버 실행과 재시작 후 데이터 유지 여부를 확인했다.

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.11"
.\gradlew.bat clean test build --console=plain
```

| 검증 항목 | 기존 실행에서 확인한 결과 |
| --- | --- |
| 빌드·자동 테스트 | build 성공, 테스트 7개 통과. 실패·오류·스킵 0개 |
| 테스트 DB | JDBC 메타데이터에서 H2 메모리 DB 확인, `member`·`article`·`surl` 생성 |
| 초기화·보존 | 빈 DB의 회원 4명·게시물 4개 생성, 반복 호출 중복 방지, 일부 회원·기존 게시물 보존 |
| 트랜잭션 | 회원·게시물 저장 뒤 예외가 발생하면 함께 롤백 |
| prod 설정·Bean | DB 접속 없이 `NotProd` 제외, PostgreSQL 드라이버·INFO 로그·콘솔 비활성화 확인 |
| 별도 dev 실행 | 기본 dev·H2 파일 연결, `/all`·`/h2-console/`·`/add` HTTP 200 |
| 재시작 | 검증 DB의 회원 4명·게시물 4개·등록 URL 1개 유지, 초기 데이터 재삽입 없음 |

기존 H2 파일이 실행 중인 다른 프로세스에서 사용되고 있어, 개발 서버 검증은 **별도 H2 파일과 포트 `18090`**에서 진행했다. 따라서 이 결과는 내 기존 `db_dev.mv.db` 파일을 그대로 실행해 검증했다는 뜻은 아니다. 운영 PostgreSQL 연결도 아직 확인하지 않았다.

검증 대상은 `jdbc:h2:file:./.gradle/verification/db_dev_check`이며, 기본 개발 설정은 `jdbc:h2:file:./db_dev`다. 테스트 내용은 [Demo03ApplicationTests](./src/test/java/com/ll/demo03/Demo03ApplicationTests.java)와 [ProdProfileTests](./src/test/java/com/ll/demo03/ProdProfileTests.java)에 있다. 운영 Profile 테스트는 설정·Bean 조건만 검사하며 운영 DB 연결 성공을 뜻하지 않는다.

## 이번 주 핵심

- **JPA로 DB 연결 대상을 바꿀 수 있지만, 데이터까지 자동으로 옮겨 주지는 않는다.**
- **Profile**은 개발·테스트·운영 설정을 분리하고, **`@Profile`**은 환경별 Bean 등록을 구분한다.
- **`@Order`**는 초기화 Runner의 실행 순서를 정한다. 회원을 먼저 준비해야 작성자 있는 게시물을 만들 수 있다.
- **DNS의 CNAME**은 다른 도메인 이름을 가리키는 설정이고, HTTPS 인증서 작업은 별개다.
- **EC2는 서버, IAM은 권한 관리, Terraform은 인프라 코드 관리 도구**다.

## 다음에 해볼 것

11강의 AWS CLI 설치, 12강의 Terraform 설치, 13강의 EC2 생성부터 이어서 진행할 예정이다. 기존 `Rq`의 **회원 ID 1번 고정**은 추후 로그인 구현에서 보완할 부분이다. 이번에 **AWS 가입·IAM 생성·CLI·Terraform 설치·EC2 구축·서비스 배포는 하지 않았다.** 이후 11~20강의 실제 작업과 검증 결과를 이 README에 추가한다.

## 참고 자료

- [강의 Chapter 07](https://www.slog.gg/p/13485#f) — 07-01~07-10 (강의 PDF 93~105쪽)
- [강사 GitHub — Fly.io 단계](https://github.com/jhs512/demo03-2024/tree/chapter-07-fly.io)
- [강사 GitHub — AWS 단계](https://github.com/jhs512/demo03-2024/tree/chapter-07-aws)
- [5주차 학습 기록](../J2-week05/README.md)
