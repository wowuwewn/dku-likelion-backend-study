# 2학기 2주차 — Spring Boot로 Todo와 URL 단축 서비스 구현

지난주 콘솔로 만들었던 Todo 기능을 Spring Boot Controller로 옮겨, URL로 할 일을 추가·조회·수정·삭제했습니다. 이어서 URL 등록·조회·이동 기능도 실습했습니다.

## 학습 내용

- `@GetMapping`으로 URL과 메서드 연결, 요청 파라미터와 `@PathVariable`로 값 받기
- 문자열과 객체·컬렉션의 응답 형식 확인하기
- Lombok으로 Getter·Setter·Builder를 만들고 Spring의 객체 주입 방식 살펴보기
- Todo 관리와 URL 등록·조회·이동 기능 구현하기

## 프로젝트 구조

```text
J2-week02
├── demo-01/                 # Controller, JSON, 의존성 주입 실습
├── demo02/demo02/           # Todo 관리
└── demo03/                 # URL 단축 실습
```

- `HomeController`: 요청 파라미터 처리와 여러 응답 형식 실습. `Post`에 `@JsonIgnore`, `@Builder.Default` 사용
- `Home2Controller`, `ComponentA`~`E`, `ComponentConfig`: `@Autowired`, `@Component`, `@Bean`, `@RequiredArgsConstructor`를 이용한 주입 실습
- `Todo`, `TodoController`: 할 일 데이터와 추가·조회·수정·삭제 처리
- `Surl`, `SurlController`: URL 데이터, 등록·조회·리다이렉트와 사용 횟수 처리

세 프로젝트 모두 `application.yml`에서 서버 포트를 `8090`으로 설정했습니다.

## 구현 기능

### Todo

| 기능 | Method | URL |
|------|--------|-----|
| 목록 조회 | GET | `/todos` |
| 개별 조회 | GET | `/todos/detail?id=1`, `/todos/1` |
| 추가 | GET | `/todos/add?body=Java공부` |
| 수정 | GET | `/todos/modify/1?body=Spring공부` |
| 삭제 | GET | `/todos/remove/1` |

`TodoController`의 `ArrayList`에 저장하므로 서버를 다시 실행하면 할 일 목록이 초기화됩니다. 수정과 삭제는 처리 여부를 `true` 또는 `false`로 반환합니다.

### URL 단축

| 기능 | Method | URL |
|------|--------|-----|
| 등록 | GET | `/add?body=검색&url=https%3A%2F%2Fwww.google.com` |
| 경로로 등록 | GET | `/s/검색/https://www.google.com` |
| 등록 목록 조회 | GET | `/all` |
| 등록된 URL로 이동 | GET | `/g/1` |

`/s/{body}/**`는 요청 URI에서 URL 부분을 가져오고, 쿼리 문자열이 있으면 함께 붙여 저장합니다. `/g/{id}`로 이동하면 `count`를 1씩 늘린 뒤 `redirect:`로 등록된 URL에 이동합니다.

## 핵심 개념

- `@Controller`: HTTP 요청을 처리하는 클래스
- `@ResponseBody`: Controller 메서드의 반환값을 HTTP 응답 본문으로 전달
- `@RestController`: Todo의 메서드 반환값을 응답 본문으로 전달
- Jackson: Java 객체와 배열, `List`, `Map` 등을 JSON 형태로 변환
- `@PathVariable`: `/todos/1`, `/g/1`처럼 URL 경로의 값을 메서드 인자로 전달
- Lombok의 `@Getter`, `@Setter`, `@Builder`: 반복적인 데이터 클래스 코드를 줄이기
- `stream().filter().findFirst().orElse(null)`과 `removeIf`: 목록에서 번호로 찾거나 삭제하기

## 배운 점

- 콘솔의 명령어 입력이 URL 요청으로 바뀌어도, 목록에서 Todo를 찾아 수정·삭제하는 로직은 그대로 쓰인다는 점을 확인했다.
- 문자열뿐 아니라 객체와 목록도 반환해보고, `@JsonIgnore`를 붙인 `Post.id`는 JSON 응답에서 제외되도록 설정했다.
- `/add`의 쿼리 파라미터에 URL을 넣을 때는 인코딩이 필요하다는 점을 알게 됐다. 복잡한 URL은 `HttpServletRequest`로 요청 URI와 쿼리 문자열을 가져와 저장했다.

## 실습하면서 확인한 점

- `Demo01Application`은 `com.ll.demo_01`, `Home2Controller`와 Component 관련 클래스는 `com.ll.demo01`로 패키지가 다르다.
- Spring Boot는 실행 클래스가 있는 패키지와 그 아래만 기본으로 스캔한다. 그래서 `Home2Controller`와 Component들은 등록되지 않고 `/home2/action1`도 사용할 수 없다.
- 실습 코드에서는 패키지를 고치지 않았다. 같은 폴더에 있어도 `package` 선언이 다르면 스캔 대상이 아니라는 걸 확인했다.

## 다음에 보완할 점

- Todo의 추가·수정·삭제도 이번에는 GET으로 처리했다. 다음에는 POST, PATCH 또는 PUT, DELETE로 나눠볼 수 있다.
- Todo와 URL은 `ArrayList`에만 저장되어 서버를 다시 켜면 사라진다. 다음에는 DB와 Repository를 연결해볼 수 있다.
- Controller 안에서 처리하는 조회·수정 로직은 나중에 Service로 나눠볼 수 있다.
