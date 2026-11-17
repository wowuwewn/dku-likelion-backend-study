# Chapter 09 구현 기록

범위는 09-01–09-15다. 강의 PDF 137–146쪽의 설명과 연결된 강사 커밋을 확인했다.
아래 검증은 각 중간 버전을 따로 실행했다는 뜻이 아니라, 최종 코드에서 해당 단계의 동작을 확인한 결과다.
파일은 별도 표시가 없으면 `src/main/java/com/ll/demo03/` 기준이다.

| 단계 | 구현 내용·파일 | 핵심 이유 | 최종 검증 |
| --- | --- | --- | --- |
| 09-01 학습 목표 | 커밋 9850185의 Member·Surl·Rq·응답 처리 분석, 소스·설정·Wrapper·테스트 복사 | 이전 주차·미커밋 작업 보존 | 구현 전 기존 44개 통과 |
| 09-02 등록 | ApiV1SurlController.add, SurlService.add | POST JSON 등록과 저장 책임 분리 | 등록·작성자 DB 확인 통과 |
| 09-03 단건 | ApiV1SurlController.get, SurlService.get | ID 조회, 없는 ID는 404 | 본인 조회·없는 ID 검증 통과 |
| 09-04 DTO | SurlDto, Controller 응답 record, 기존 SurlController 응답 변경 | Entity·내부 정보 비노출 | DTO 8개 필드·password/author 비노출 통과 |
| 09-05 OSIV | resources/application.yml, SurlService, Surl.author, SurlRepository | 서비스 안에서 DTO 생성, LAZY와 목록 EntityGraph 사용 | 테스트 트랜잭션 없이 OSIV=false 조회 통과 |
| 09-06 삭제 | ApiV1SurlController.delete, SurlService.delete | 쓰기 트랜잭션에서 삭제 | 삭제 후 DB 부재·재조회 404 통과 |
| 09-07 다건 | getItems, findByAuthorOrderByIdDesc | 본인 자료만 ID 역순 반환 | 본인 2건·타인 제외·빈 목록 통과 |
| 09-08 수정 | ApiV1SurlController.modify, SurlService.modify | Dirty Checking, flush 후 수정일 DTO 반영 | save 없이 JDBC 재조회로 수정 커밋 확인 |
| 09-09 권한 | SurlService의 조회·수정·삭제 | 본인과 타인 작업 구분 | 타인 GET·PUT·DELETE 각각 403, DB 원본 유지 |
| 09-10 AuthService | AuthService의 checkCanGetSurl/checkCanModifySurl/checkCanDeleteSurl | 공통 소유권 정책 관리 | 타인·작성자 없는 자료 거부 통과 |
| 09-11 매 요청 인증 | 모든 SURL 관리 경로의 Rq 호출 | 이전 요청의 성공을 신뢰하지 않음 | 기존·신규 8종 경로 누락 시 401, 실제 HTTP 요청 분리 통과 |
| 09-12 아이디 파라미터 | global/rq/Rq.getMember, MemberService.findByUsername | actorUsername으로 실제 회원 조회 | 없는 회원 401-3, 목록 작성자 구분 통과 |
| 09-13 Rq 인증 | global/rq/Rq의 RequestScope·HttpServletRequest | Controller의 인증 중복 제거 | 테스트 전용 반복 호출에서 같은 Member 반환 |
| 09-14 비밀번호 | Rq.getMember의 actorPassword 필수값·일치 검사 | 아이디만으로 인증하지 않음 | 누락·공백·아이디만·잘못된 암호 모두 401 |
| 09-15 캐시 | Rq의 member·authenticationFailure 필드 | 성공·인증 실패만 요청별 저장 | RqTests 5개, 요청 간 회원·상태 분리 통과 |

## 검증 결과

2026-11-18, Java 21.0.11 / Gradle 8.6에서 `clean test build --offline --console=plain --no-daemon` 성공.
**기존 44개 + 신규 41개 = 85개**, 실패·오류·스킵은 모두 0개다.
DB를 사용하는 테스트는 test Profile·H2 메모리 DB로 수행했고 개발·운영 DB는 변경하지 않았다.
개별 개수와 실행 방법은 [README](../README.md#실행과-검증)에 정리했다.

## 강의와 조정한 부분

- Controller 대신 기존 서비스 계층 트랜잭션을 유지했다. 조회·권한 검사·DTO 변환을 그 안에서 마친다.
- 원본의 없는 회원·비밀번호 불일치 403을 401로 구분했다. 소유권 거부는 403이다.
- 성공뿐 아니라 인증 실패도 요청 범위에 캐시한다. 일반 DB 장애는 캐시하지 않는다.
- 기존 /add·/s/**·/all에도 인증을 적용했다. /s/**는 인코딩된 인증 파라미터까지 제거한다.
- /g/{id} 공개 링크는 유지하되 조회와 조회수 증가를 한 트랜잭션으로 묶었다.
- MockMvc 이중 슬래시 정규화는 원문 URI로 테스트했다. 한글 인코딩 오류는 UTF-8 재작성과 응답 메시지 검증으로 수정했다.

[강사 최종 코드](https://github.com/jhs512/demo03-2024/tree/9f1a706)의 API 경로·메서드·DTO 필드·소유권 정책을 따랐다.
쿠키 인증·Spring Security·JWT·API Key 및 Chapter 10 이후 코드는 포함하지 않았다.
