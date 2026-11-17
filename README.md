# dku-likelion-backend-study

단국대학교 멋쟁이사자처럼 14기 백엔드(Java) 트랙 스터디 기록 레포지토리입니다.
주차별 강의 학습 정리와 실습 코드를 관리합니다.

**최근 학습 — [2학기 8주차 — 아이디/비밀번호를 포함시킨 인증](./J2-week08/README.md)**<br>
7주차 회원가입 API에서 교육용 아이디·비밀번호 인증으로 확장했습니다. AuthService 인가·SURL REST CRUD·RequestScope 캐싱을 구현했습니다.

## 🛠️ 기술 스택

**Languages & Backend** ![Java 21](https://img.shields.io/badge/Java%2021-ED8B00?style=flat-square) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)<br>
**Database** ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white) ![H2 Database](https://img.shields.io/badge/H2%20Database-09476B?style=flat-square)<br>
**Tools & Infrastructure** ![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=flat-square&logo=intellijidea&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white) ![Git](https://img.shields.io/badge/Git-F05032?style=flat-square&logo=git&logoColor=white) ![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white) ![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat-square) ![Terraform](https://img.shields.io/badge/Terraform-844FBA?style=flat-square&logo=terraform&logoColor=white)

DBeaver·Postman도 실습에 사용했습니다. AWS CLI·Terraform은 기초 개념만 학습했으며, 도구 설치·인프라 구축은 아직 진행 전입니다.

## 📚 주차별 학습 정리

### 1학기

| 주차 | 주제 | 핵심 내용 | 바로가기 |
|------|------|-----------|----------|
| 2주차 | Java Beginner | 변수와 자료형, 연산자, 조건문·반복문, 자료구조, 함수 | [week02](./week02) |
| 3주차 | 회원가입 프로그램 | Scanner 입력 처리, 데이터 저장, 반복 가입 로직 구현 | [week03](./week03) |
| 4주차 | Java Next Level (객체지향 + 예외처리) | 객체지향(클래스·생성자·상속·오버로딩·인터페이스 등) + 예외처리 | [week04](./week04) |
| 5주차 | Web 동작 원리 & Spring Boot | 웹 동작 원리, Spring Boot 구조, Product CRUD API 구현 | [week05](./week05) |
| 6주차 | 파이널 프로젝트: Books CRUD API | Spring Boot로 Books CRUD API 설계 및 구현 (POST/GET/PUT/DELETE) | [week06](./week06) |

### 2학기

| 주차 | 주제 | 핵심 내용 | 바로가기 |
|------|------|-----------|----------|
| 1주차 | 자바로 구현하는 간단한 할 일 관리 서비스 | 콘솔 입력, Todo 객체 관리, List, 할 일 추가·조회·수정·삭제, Controller 책임 분리 | [J2-week01](./J2-week01) |
| 2주차 | Spring Boot로 Todo와 URL 단축 서비스 구현 | Controller와 JSON 응답, Todo 관리, URL 등록·조회·리다이렉트 | [J2-week02](./J2-week02) |
| 3주차 | fly.io 로 서비스 배포하기 | Docker, Fly.io 배포, 환경별 Profile, GitHub Actions 자동 배포, Secrets | [J2-week03](./J2-week03) |
| 4주차 | 데이터베이스 | MySQL, DBeaver, SQL CRUD, 제약조건, 테이블 관계, INNER JOIN | [J2-week04](./J2-week04) |
| 5주차 | Spring Data JPA로 영속성 부여 | Entity / Repository / Service, 트랜잭션·Auditing, 작성자 관계, URL 단축 서비스 DB 저장 | [J2-week05](./J2-week05) |
| 6주차 | AWS, 테라폼으로 EC2 구축하고 서비스 배포 | PostgreSQL·H2, Spring Profile, Fly.io 도메인, AWS·IAM·CLI·Terraform 기초 | [J2-week06](./J2-week06) |
| 7주차 | REST API로 회원가입 구현 | 회원가입 REST API, Validation, 예외 처리, 응답 DTO, AOP | [J2-week07](./J2-week07) |
| 8주차 | 아이디/비밀번호를 포함시킨 인증 | 아이디·비밀번호 인증, AuthService 인가, SURL REST CRUD, RequestScope 캐싱 | [J2-week08](./J2-week08) |

6주차는 DB·실행 환경 분리와 인프라 기초까지 진행했으며, 실제 EC2 배포는 아직 완료하지 않았습니다.

## 🌱 스터디를 통해 배운 것

- Java 기초 문법부터 객체지향, 예외처리까지 단계별로 학습
- 웹의 동작 원리(클라이언트-서버, HTTP)와 Spring Boot 구조 이해
- JPA를 활용한 데이터베이스 연동과 RESTful CRUD API 직접 구현
- Spring Profile로 실행 환경을 나누고 AWS·IAM·Terraform 기초 정리
- 회원가입 API의 입력 검증·예외 처리와 DTO·AOP 기반 응답 구성
- 아이디·비밀번호 인증과 소유권 인가의 차이, 요청 범위 캐싱 이해
- Git/GitHub를 활용한 주차별 코드 버전 관리

---

> 🔗 학습 정리 노션 허브와 함께 운영 중 · 매주 수요일 (비)대면 스터디 진행
