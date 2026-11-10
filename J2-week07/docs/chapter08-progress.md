# Chapter 08 단계별 검증

완성 코드의 강의 대응표와 실행 근거다. 중간 단계의 API 형태는 뒤 단계에서 바뀐다.

| 단계 | 확인·변경 | 단계 종료 검증 |
| --- | --- | --- |
| 08-01 | 기존 Service·Entity·Repository와 최종 REST 가입 구조 확인 | 기존 테스트 7개 통과, Java 21 확인 |
| 08-02 | MemberController → 기존 join 연결, password JSON 제외 | GET 가입·DB 저장·비밀번호 비노출, 전체 8개 통과 |
| 08-03 | Ut.str.isBlank·hasLength 재사용 | null·빈 문자열·공백·일반 문자열 6개, 전체 14개 통과 |
| 08-04 | 기존 중복 throw 재사용, Service 필수 입력 방어 | 저장 전 중단 7개, 전체 21개 통과 |
| 08-05 | ControllerAdvice·ExceptionHandler 추가 | 중복 가입 메시지 중앙 처리·행 수 보존, 전체 22개 통과 |
| 08-06 | ArticleController의 HTML·JSON 두 표현 | Content-Type·본문·회원 정보 제외, 전체 23개 통과 |
| 08-07 | Postman Collection 요청 예제 작성 | JSON 파싱·요청 항목 확인. Postman 앱 설치·실행은 하지 않음 |
| 08-08 | 가입 POST 전환, GET은 생성 불가 | POST 저장·GET 405와 행 수 불변, 전체 24개 통과 |
| 08-09 | ApiV1MemberController·RequestBody·버전 경로 | JSON → 필드 매핑·DB 저장·GET 405, 전체 25개 통과 |
| 08-10 | ResponseEntity로 예외의 HTTP 상태 지정 | 중복 요청 실제 400, 정상 200, 전체 25개 통과 |
| 08-11 | RsData 재사용·Empty의 명시적 {} 직렬화 | 성공·실패 네 필드와 data 확인, 전체 25개 통과 |
| 08-12 | validation starter·Valid·NotBlank | 세 필드 누락·공백 6개, Validation 예외 발생 확인, 전체 31개 통과 |
| 08-13 | Validation 예외 전용 핸들러 | 누락·공백·null·빈 문자열 12개와 통일 응답, 전체 37개 통과 |
| 08-14 | JSON·기타 예외 처리, Rq 경로, 안전한 메시지 | 400·404·405·415·500, Allow·경로·비밀 비노출, 전체 40개 통과 |
| 08-15 | MemberJoinRespBody·공개 필드 3개·newDataOf | 엔티티 대신 data.item, 비밀번호 비노출, 전체 40개 통과 |
| 08-16 | ResponseAspect·Rq.setStatusCode, 가입 결과 201 | 실제 프록시·201·동적 202·기존 JSON/HTML·예외 응답, 전체 41개 통과 |

이후 최종 검수에서 URL 회귀·HTML 이스케이프·실제 HTTP와 DB 커밋 검증을 추가했다.

최종 결과(2026-11-09): Java 21.0.11, Gradle 8.6, `clean test build` 성공.
자동 테스트 44개 통과, 실패·오류·스킵 0개.
test Profile 별도 실행 서버(18091)에 PowerShell 5개 요청 통과 후 서버 종료.
Postman Collection은 작성·파싱 확인만 했으며 Postman 앱은 실행하지 않았다.
