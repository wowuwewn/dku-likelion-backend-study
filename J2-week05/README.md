# 2학기 5주차 — Spring Data JPA로 영속성 부여

2주차의 URL 단축 서비스는 `ArrayList`에 저장했고, 3주차에는 배포 후 재시작하면 데이터가 사라졌다.<br>
4주차에 SQL로 MySQL을 다룬 데 이어, 이번에는 Java 객체와 MySQL을 JPA로 연결해 기존 서비스를 DB에 저장하는 구조로 바꿨다.

학습 범위는 [강의 페이지의 Chapter 06](https://www.slog.gg/p/13485#f), 제공된 「백엔드 완전정복 | 실습으로 배우는 Spring Boot」 PDF의 Chapter 06(78~93쪽), [공식 강의 GitHub](https://github.com/jhs512/demo03-2024)의 06-01~06-20이다. 아래 설명은 현재 `J2-week05`의 최종 코드와 실제 실행·DB 조회 결과를 기준으로 정리했다.

## 이번 주 한눈에 보기

| 구분 | 내용 |
| --- | --- |
| 학습 목표 | Java 객체의 상태와 관계를 MySQL에 저장하고, 재시작 후에도 유지하기 |
| 실습 환경 | Java 21 / Spring Boot 3.2.4 / Spring Data JPA / MySQL 8.4.1 |
| 실행 설정 | Spring Boot `8090`, Docker 컨테이너 `mysql-1`의 MySQL `3306`, DB `surl_dev` |
| Article | 엔티티·Repository CRUD, Service, 작성자 관계 |
| Member | 회원 저장, username 중복 검사, 작성자 참조 |
| Surl | 메모리 저장에서 DB 저장으로 전환, 조회수 변경, 작성자 관계 |
| 공통 구조 | 트랜잭션, Auditing, `BaseEntity` / `BaseTime`, `RsData` / `GlobalException` |

## 이전 주차와의 연결

```text
2주차: Controller / ArrayList 기반 URL 단축 서비스
  → 3주차: Fly.io 배포, 재시작 후 메모리 데이터 유실 확인
  → 4주차: MySQL과 SQL CRUD, 테이블 관계와 JOIN 학습
  → 5주차: Spring Data JPA로 기존 서비스를 MySQL에 연결
```

지난주에는 SQL을 직접 작성했다. 이번에는 엔티티와 Repository를 사용했지만 DB에서는 같은 종류의 SQL이 실행됐다. Java 코드만 보는 대신 실제 행과 칼럼도 확인했다.

4주차에 사원마다 부서명을 중복 저장하는 대신 부서 번호를 저장했던 내용도 이어졌다. 이번에는 게시물과 단축 URL에 작성자의 정보를 반복해서 넣지 않고, 회원 번호로 연결했다.

## Spring Data JPA와 MySQL 연결

[build.gradle](./build.gradle)에 JPA 기능과 MySQL 드라이버를 추가했다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
runtimeOnly 'com.mysql:mysql-connector-j'
```

Spring Data JPA 의존성을 통해 Repository 기반의 엔티티 저장·조회 기능을 사용할 수 있고, MySQL 드라이버는 실제 MySQL과 통신하는 데 필요하다. [application.yml](./src/main/resources/application.yml)의 datasource에 접속할 DB 정보를 설정했다.

```text
Entity와 Repository 사용
→ Spring Data JPA
→ JPA 구현체인 Hibernate
→ JDBC / MySQL Connector/J
→ MySQL
```

| 구성 요소 | 이번 실습에서의 역할 |
| --- | --- |
| Entity | Java 객체의 필드와 DB 칼럼, 객체 간 관계를 매핑 |
| Repository | 엔티티의 저장·조회·삭제 기능 제공 |
| Spring Data JPA | Repository 인터페이스의 구현을 제공하고 JPA를 사용하기 쉽게 연결 |
| JPA | Java 객체의 영속성을 관리하는 표준 API와 규칙 |
| Hibernate | JPA 구현체. 매핑에 따라 SQL 생성, 엔티티 변경 감지 |
| JDBC / MySQL 드라이버 | 생성된 SQL을 MySQL에 전달하고 결과를 받음 |

현재 설정은 `spring.jpa.hibernate.ddl-auto: update`다. 애플리케이션을 시작할 때 엔티티 매핑에 맞춰 테이블을 생성·갱신한다. 다만 기존 칼럼의 자료형이나 기존 데이터까지 원하는 상태로 모두 바꿔주는 것은 아니다. `Article.body`는 코드의 `TEXT` 매핑과 실제 DB 칼럼을 함께 확인했다.

`org.hibernate.SQL: DEBUG`와 `org.hibernate.orm.jdbc.bind: TRACE` 로그를 사용해 실행 SQL과 전달된 값을 확인했다. Java 메서드 호출이 어떤 SQL로 이어지는지 비교하기 위한 설정이다.

## Article로 확인한 Entity / Repository CRUD

[Article](./src/main/java/com/ll/demo03/Article.java)은 `@Entity`로 등록했고, [ArticleRepository](./src/main/java/com/ll/demo03/ArticleRepository.java)는 다음처럼 작성했다.

```java
public interface ArticleRepository extends JpaRepository<Article, Long> {
}
```

관리할 엔티티는 `Article`, ID 타입은 `Long`이다. Spring Data JPA가 인터페이스의 기본 저장·조회·삭제 기능을 구현한다.

현재 ID 설정은 공통 부모인 [BaseEntity](./src/main/java/com/ll/demo03/global/jpa/entity/BaseEntity.java)에 있다.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@EqualsAndHashCode.Include
private Long id;
```

`@Entity`는 클래스를 JPA의 관리 대상으로 지정하고, `@Id`는 기본키를 지정한다. `GenerationType.IDENTITY`는 이번 MySQL 환경에서 DB의 `AUTO_INCREMENT`로 번호를 받는 방식이다. 세 설정은 서로 다른 역할이다.

| 작업 | Java에서 사용하는 기능 | DB에서 이어지는 작업 |
| --- | --- | --- |
| INSERT | `articleRepository.save(article)` | 새 게시물 행 저장, ID 생성 |
| SELECT | `findById(id)`, `findAll()` | 한 행 또는 전체 게시물 조회 |
| COUNT | `articleRepository.count()` | 게시물 행 수 조회 |
| DELETE | `articleRepository.delete(article)` | 해당 게시물 삭제 |
| UPDATE | 관리 중인 엔티티를 쓰기 트랜잭션 안에서 변경 | 변경 감지 후 UPDATE |

`findById()`는 결과가 없을 수도 있어 `Optional<Article>`을 반환하고, `findAll()`은 여러 건을 담는 `List<Article>`을 반환한다.

06-10에서는 메서드 이름으로 조건 조회를 정의하는 방식도 학습했다. 최종 `ArticleRepository`는 공식 06-20 코드처럼 기본 기능만 사용한다. 현재 조건 조회의 예는 [MemberRepository](./src/main/java/com/ll/demo03/MemberRepository.java)에 있다.

```java
Optional<Member> findByUsername(String username);
```

Spring Data JPA가 이 이름을 해석해 username 조건 조회를 만든다.

## Service 계층과 Dirty Checking

### 처리 책임 분리

| 계층 | 실제 역할 |
| --- | --- |
| Controller / 호출부 | 요청 인수를 받거나 초기 데이터 생성을 호출 |
| Service | 객체 생성, 작성자 연결, 중복 검사, 조회수 변경과 트랜잭션 범위 관리 |
| Repository | 엔티티 저장·조회·삭제 |
| Entity | 데이터의 상태와 관계 표현 |

[ArticleService](./src/main/java/com/ll/demo03/ArticleService.java)의 `write(Member author, String title, String body)`는 작성자·제목·내용으로 게시물을 만들고 Repository에 저장한다. 호출부가 DB 처리 세부사항까지 모두 다루지 않도록 책임을 나눈 것이다.

### Setter가 UPDATE를 실행하는 것은 아님

`Article.setTitle(...)`은 Lombok의 `@Setter`가 만들며 먼저 객체의 값만 바꾼다. **관리 중인 엔티티를 쓰기 트랜잭션에서 변경하면 Hibernate가 기존 상태와 비교해 flush 시 UPDATE한다.** 일반적으로 커밋 직전에 반영된다.

영속성 컨텍스트는 JPA가 엔티티를 관리하는 공간이다. 여기에서 관리되는 객체의 변경을 확인하는 기능이 Dirty Checking이다. Builder로 만든 일반 객체나 관리가 끝난 객체의 필드를 바꿨다고 자동으로 DB에 저장되지는 않는다.

현재 `NotProd.work2()`는 비어 있다. 최종 코드에서 실제로 실행되는 변경 감지는 Surl 조회수 증가에서 확인했다.

```java
@Transactional
public void increaseCount(Surl surl) {
    surl.increaseCount();
}
```

[Surl](./src/main/java/com/ll/demo03/Surl.java)의 `increaseCount()`는 `count++`만 수행한다. [SurlService](./src/main/java/com/ll/demo03/SurlService.java)의 쓰기 트랜잭션에서 변경 감지가 동작해, `save()`를 다시 호출하지 않아도 UPDATE가 실행됐다.

> ⭐ 중요: `@Transactional`만 붙였다고 아무 객체나 자동 저장되는 것은 아니다. JPA가 관리하는 엔티티를 쓰기 트랜잭션 안에서 변경해야 Dirty Checking으로 DB에 반영된다.

## JPA Auditing으로 날짜 기록

객체를 만들 때마다 생성일·수정일을 직접 넣는 대신 Auditing으로 기록했다. 현재 날짜 필드는 [BaseTime](./src/main/java/com/ll/demo03/global/jpa/entity/BaseTime.java)에 모여 있다.

```java
@CreatedDate
private LocalDateTime createDate;

@LastModifiedDate
private LocalDateTime modifyDate;
```

| 설정 | 역할 |
| --- | --- |
| `@EnableJpaAuditing` | [Demo03Application](./src/main/java/com/ll/demo03/Demo03Application.java)에서 Auditing 활성화 |
| `@EntityListeners(AuditingEntityListener.class)` | BaseTime을 상속한 엔티티의 저장·수정 시 날짜 기록 동작 연결 |
| `@CreatedDate` | 최초 저장 시 생성일 기록 |
| `@LastModifiedDate` | 최초 저장 및 변경 시 수정일 기록 |

날짜는 `LocalDateTime`을 사용하며 현재 DB에는 `datetime(6)`으로 저장된다. `BaseTime.setModified()`도 있지만 현재 요청 흐름에서 직접 호출하지 않는다. 자동 날짜 기록은 Auditing이 수행한다.

4번 Surl의 `/g/4` 호출 전후를 DB에서 조회해 비교했다.

| 항목 | 호출 전 | 호출 후 |
| --- | --- | --- |
| `count` | 1 | 2 |
| `create_date` | 2026-10-06 23:53:48.499995 | 2026-10-06 23:53:48.499995 |
| `modify_date` | 2026-10-06 23:53:48.623713 | 2026-10-06 23:54:56.059474 |

조회수가 변경되어도 생성일은 유지됐고 수정일만 갱신됐다. 날짜를 Controller에서 직접 바꾸지 않아도 실제 UPDATE와 함께 기록되는 것을 확인했다.

## 정상 결과, 예외와 트랜잭션

### RsData / GlobalException

[RsData](./src/main/java/com/ll/demo03/global/rsData/RsData.java)는 처리 결과를 `resultCode`, `statusCode`, `msg`, `data`로 묶는다. 데이터만 반환할 때보다 어떤 작업이 완료됐는지 함께 전달할 수 있다.

URL 생성은 `RsData.of(메시지, surl)`로 정상 결과를 반환한다. 반면 [MemberService](./src/main/java/com/ll/demo03/MemberService.java)는 같은 username이 있으면 결과 코드 `400-1`의 `GlobalException`을 던진다. 정상 결과와 예외를 구분해 이후 저장 로직이 계속되는 것을 막는다. DB의 기존 username UNIQUE 제약조건도 유지했다.

[GlobalException](./src/main/java/com/ll/demo03/global/exceptions/GlobalException.java)은 `RuntimeException`을 상속하고 오류 정보를 RsData로 보관한다. 현재는 전역 예외 핸들러가 없어, 예외의 `404-0`이나 RsData의 `statusCode`가 실제 HTTP 응답 코드로 자동 변환되지는 않는다.

### 조회와 쓰기 트랜잭션

Service 클래스에는 `@Transactional(readOnly = true)`를 적용하고, DB를 변경하는 public 메서드는 일반 `@Transactional`로 덮어쓴다.

| Service | 기본 조회 | 쓰기 트랜잭션을 사용하는 메서드 |
| --- | --- | --- |
| ArticleService | `count()`, `findById()`, `findAll()` | `write()`, `delete()` |
| MemberService | 회원 조회, `getReferenceById()` | `join()` |
| SurlService | `findAll()`, `findById()` | `add()`, `increaseCount()` |

트랜잭션은 DB 작업을 한 단위로 묶는다. 정상 종료하면 커밋하고, 처리 중 `RuntimeException`이 트랜잭션 경계 밖으로 전달되면 기본적으로 롤백한다. 오류 결과 객체를 반환하는 것과 예외를 던지는 것은 이 점에서도 다르다.

[NotProd](./src/main/java/com/ll/demo03/NotProd.java)는 `self.work1()`로 Spring의 프록시를 통해 호출해 트랜잭션을 적용한다. 안의 회원 가입과 게시물 저장도 기본 설정에 따라 같은 트랜잭션에 참여한다. `this.work1()`로 직접 호출하는 경우와 구분했다.

## 공통 필드를 @MappedSuperclass로 분리

Article과 Member에 반복되던 ID·날짜 설정을 부모로 옮겼고 Surl도 같은 구조를 사용한다.

| 클래스 | 담당하는 내용 |
| --- | --- |
| BaseEntity | `id`, 기본키와 자동 ID 생성 매핑 |
| BaseTime | BaseEntity 상속 + `createDate`, `modifyDate`, Auditing |
| Article / Member / Surl | BaseTime 상속 + 각 엔티티의 고유 데이터와 관계 |

`@MappedSuperclass`는 자식에게 DB 매핑을 상속한다. **부모 자체는 엔티티가 아니며 별도 테이블도 생기지 않는다.** 실제 DB에는 `article`, `member`, `surl`만 있고 각 테이블에 ID·날짜 칼럼이 들어 있다.

ID와 시간 기록을 나눠 공통 역할을 구분했다. 상속은 코드와 매핑의 중복을 줄이는 것이며, 서로 다른 테이블이 같은 ID나 같은 한 행을 공유한다는 뜻은 아니다.

## 작성자 Member와 엔티티 관계

### Article에 작성자 연결

```java
@ManyToOne
private Member author;
```

한 회원이 여러 게시물을 작성할 수 있어 Article → Member는 다대일 관계다. Java에서는 회원 객체를 참조하고, DB에서는 `article.author_id`에 `member.id`를 저장한다. 회원 정보 전체를 article 행에 복사하지 않는다.

`NotProd`는 `memberService.join(...).getData()`로 받은 회원을 `articleService.write(member1, "제목 1", "내용 1")`에 전달한다. Service가 `.author(author)`로 연결한 뒤 게시물을 저장한다. 회원 자동 저장을 위한 cascade 설정은 없으므로 현재 호출부는 회원부터 저장한다.

### Surl에도 같은 작성자 관계 적용

```java
@ManyToOne
@JsonIgnore
private Member author;
```

한 회원이 여러 단축 URL을 만들 수 있어 같은 다대일 관계를 사용한다. 이름을 반복 저장하는 대신 회원 ID로 연결하고, 필요한 정보는 관계를 따라 조회한다.

| 엔티티 | Java 참조 | 실제 DB 외래키 |
| --- | --- | --- |
| Article | `Article.author` → Member | `article.author_id` → `member.id` |
| Surl | `Surl.author` → Member | `surl.author_id` → `member.id` |

현재는 Article·Surl에서 Member로 향하는 단방향 관계이며, Member에 게시물이나 Surl 목록을 추가하지 않았다. MySQL의 외래키 메타데이터에서도 위 두 관계를 확인했다.

`@JsonIgnore`는 Surl의 JSON 응답에서 author를 제외한다. **DB 저장이나 외래키 관계를 제거하는 설정은 아니다.** 실제 `/all` 응답에는 author가 없지만 DB의 `surl.author_id`에는 회원 번호가 남았다.

### Rq / getReferenceById

[Rq](./src/main/java/com/ll/demo03/global/rq/Rq.java)는 `@RequestScope`로 요청마다 사용하는 회원 조회 창구다.

```java
public Member getMember() {
    // 06-20 강의의 임시 로그인 가정: 1번 회원의 프록시를 반환한다.
    return memberService.getReferenceById(1L);
}
```

현재는 인증된 사용자를 구분하는 로그인 구현이 아니라 **1번 회원을 현재 회원이라고 가정한 실습 코드**다. 두 Surl 생성 경로 모두 이 회원을 Service에 전달한다.

`getReferenceById()`는 회원의 참조 객체인 프록시를 얻는다. 아직 초기화되지 않은 프록시에서 ID만 읽을 때는 ID를 이미 알고 있고, username 같은 실제 데이터가 필요해지면 회원 조회가 일어날 수 있다.

| `/add`의 실제 로그 구간 | 이번 실행에서 확인한 내용 |
| --- | --- |
| `before get id` → `after get id` | 회원 SELECT 없이 ID 접근 |
| `before get username` → `after get username` | 회원 SELECT 실행 후 username 접근 |

프록시의 조회 시점과 연관관계의 fetch 설정은 구분해야 한다. 현재 `@ManyToOne`에는 `fetch = LAZY`를 지정하지 않았고 이 프로젝트의 JPA 기본값은 EAGER다.

## Surl에 영속성을 부여한 결과

기존에는 Controller의 리스트에 URL을 넣고 ID와 날짜를 직접 관리했다. 현재는 [SurlController](./src/main/java/com/ll/demo03/SurlController.java)가 [SurlService](./src/main/java/com/ll/demo03/SurlService.java)를 호출하고, [SurlRepository](./src/main/java/com/ll/demo03/SurlRepository.java)가 MySQL에 저장한다.

```text
변경 전: Controller → ArrayList → JVM 메모리
변경 후: Controller → Service → Repository → JPA / Hibernate → MySQL
```

| 요청 | 현재 처리 흐름 |
| --- | --- |
| `/add?body=...&url=...` | 현재 회원 참조 → Surl 생성 → DB 저장 → RsData 반환 |
| `/s/{body}/**` | 경로 뒤 원본 URL과 쿼리 문자열 추출 → 같은 생성 Service 호출 |
| `/all` | Repository에서 Surl 목록 조회 → JSON 반환 |
| `/g/{id}` | ID 조회 → 조회수 증가 → 원본 URL로 302 리다이렉트 |

프로그램의 객체는 재시작하면 다시 만들어지지만, MySQL에 저장한 행은 남는다. 이 행을 다시 조회해 같은 URL과 조회수를 사용할 수 있다는 점이 이번 주의 **영속성 부여**다.

### 최종 코드 실행 검증

캡처 대신 실제 실행·SQL 로그·DB 조회 결과를 표로 남겼다. 소스 변경 없이 Java 21로 전체 빌드와 기존 테스트를 실행하고, 8090에서 최종 코드를 실행해 확인했다.

| 검증 항목 | 실제 결과 |
| --- | --- |
| Gradle build | 성공. 기존 `contextLoads` 테스트 1개 통과 |
| DB 테이블 | `member`, `article`, `surl` 확인. 부모 클래스 테이블 없음 |
| Article 칼럼 | `id`의 PK / AUTO_INCREMENT, `body`의 TEXT, 날짜와 `author_id` 확인 |
| URL 생성 | `Spring Data JPA` URL을 기존 `/add`로 등록. `200-1`, 생성 ID 4, INSERT 로그 확인 |
| 목록 조회 | `/all`에서 저장된 Surl 4개 조회 |
| URL 이동 | `/g/4`가 `302`, `Location: https://spring.io/projects/spring-data-jpa` 반환 |
| 변경 감지 | 별도 save 호출 없이 UPDATE 로그 확인. 두 번 이동해 조회수 0 → 1 → 2 |
| Auditing | DB 조회에서 생성일 유지, 수정일 갱신 확인 |
| 작성자 연결 | 4번 Surl의 `author_id=1`, JOIN 결과 `username=user1` |
| JSON 제외 | `/all`의 Surl 응답에 author 필드 없음 |
| 재시작 | 애플리케이션 재시작 후 ID 4의 URL·조회수 2·날짜가 그대로 유지 |

현재 DB의 Surl 데이터를 회원과 LEFT JOIN한 결과다. `/all`은 정렬 조건이 없으므로 아래 표는 DB에서 ID 순서로 조회해 정리했다.

| Surl ID | body | author_id | 회원 username | count |
| --- | --- | --- | --- | --- |
| 1 | 네이버 | NULL | NULL | 1 |
| 2 | 구글 | 1 | user1 | 1 |
| 3 | 스프링 | 1 | user1 | 0 |
| 4 | Spring Data JPA | 1 | user1 | 2 |

회원은 2명, 게시물은 1개다. 기존 게시물 `id=2`와 작성자 관계를 추가하기 전에 만든 Surl `id=1`은 author가 NULL인 상태를 보존했다. 연관관계를 코드에 추가해도 기존 행의 작성자가 자동으로 채워지지는 않는다.

`NotProd.work1()`에는 회원 두 명과 게시물 네 개를 만드는 코드가 있지만, `articleService.count() > 0`이면 건너뛴다. 기존 게시물이 있어 실행되지 않았으므로 초기화 코드의 샘플 수와 현재 DB의 행 수는 다르다.

## 이번 주에 이해한 핵심 개념

- JPA를 사용해도 최종 저장·조회 작업은 MySQL의 SQL로 실행된다.
- Entity는 상태와 관계, Repository는 DB 접근, Service는 작업 흐름과 트랜잭션을 담당한다.
- 영속성 컨텍스트가 관리하는 엔티티의 변경은 쓰기 트랜잭션에서 Dirty Checking으로 반영된다.
- Auditing과 공통 부모를 사용해 ID·날짜 설정의 반복을 줄였다.
- 작성자는 Java의 객체 참조와 DB의 외래키로 같은 관계를 표현한다.
- Surl을 DB에 저장하고 재시작 후 다시 읽어 메모리 저장의 한계를 해결했다.

## 특히 헷갈렸던 부분

| 헷갈리는 지점 | 정리한 내용 |
| --- | --- |
| `@Entity`, `@Id`, `@GeneratedValue` | 관리 대상 지정, 기본키 지정, ID 생성 방식으로 역할이 다름 |
| Builder와 저장 | 객체 생성과 Repository를 통한 DB 저장은 별도 작업 |
| Setter와 UPDATE | Setter가 SQL을 실행하지 않음. 관리 상태와 트랜잭션 조건이 필요 |
| `@MappedSuperclass` | 부모 매핑을 상속하지만 부모 자체의 테이블은 없음 |
| `Member author` | 회원 전체를 자식 테이블에 복사하지 않고 회원 ID로 연결 |
| `@JsonIgnore` | JSON 응답에서 제외하며 DB 저장·관계에는 영향 없음 |
| `Rq.getMember()` | 현재 로그인 기능이 아니라 1번 회원을 가정한 실습 코드 |

## 최종 구조

```text
공통 필드 상속
BaseEntity (id)
  ↓
BaseTime (createDate, modifyDate)
  ↓
Member / Article / Surl

Java 객체 관계
Member
  ↑
  ├─ Article.author
  └─ Surl.author

DB 외래키 관계
member.id
  ↑
  ├─ article.author_id
  └─ surl.author_id

요청과 저장
SurlController → Rq에서 회원 참조 확보
  → SurlService → SurlRepository → JPA / Hibernate → MySQL
```

## 강의 코드와 현재 프로젝트

최종 구조는 [공식 06-20 코드](https://github.com/jhs512/demo03-2024/tree/4211d41f66a707b1111bcc2b7fe8cce8cd0e95f9)를 기준으로 확인했다. 강의 원본의 `domain/...` 패키지 대신 기존 `com.ll.demo03`에 Entity·Service·Repository를 유지했고, 공통 클래스는 `global` 아래에 두었다. `GenerationType.IDENTITY`를 명시해 사용했으며 Member의 기존 username UNIQUE 제약도 유지했다.

검증 대상은 로컬 8090 서버와 `surl_dev` DB다. Fly.io 배포 환경의 DB 연동은 이번 실습 범위에 포함되지 않는다.

## 이번 주 한 줄 정리

SQL을 직접 작성해 DB를 다루던 단계에서, Spring Data JPA로 Java 객체의 상태와 관계를 MySQL에 저장하고 URL 단축 데이터를 재시작 후에도 유지하는 구조로 발전했다.
