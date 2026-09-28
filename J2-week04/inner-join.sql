-- a5: 부서명을 중복 저장하던 구조를 부서 번호로 바꾸고 JOIN으로 조회한다.
-- MySQL 실습용. 실행하면 기존 a5 DB가 삭제되고 다시 만들어진다.

DROP DATABASE IF EXISTS a5;
CREATE DATABASE a5;
USE a5;

-- 1. 부서와 사원 생성: 처음에는 emp에도 부서명을 직접 저장
CREATE TABLE dept (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    PRIMARY KEY(id),
    regDate DATETIME NOT NULL,
    `name` CHAR(100) NOT NULL UNIQUE
);

INSERT INTO dept
SET regDate = NOW(),
    `name` = '홍보';

INSERT INTO dept
SET regDate = NOW(),
    `name` = '기획';
SELECT * FROM dept;

CREATE TABLE emp (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    PRIMARY KEY(id),
    regDate DATETIME NOT NULL,
    `name` CHAR(100) NOT NULL,
    deptName CHAR(100) NOT NULL
);

INSERT INTO emp
SET regDate = NOW(),
    `name` = '홍길동',
    deptName = '홍보';

INSERT INTO emp
SET regDate = NOW(),
    `name` = '홍길순',
    deptName = '홍보';

INSERT INTO emp
SET regDate = NOW(),
    `name` = '임꺽정',
    deptName = '기획';
SELECT * FROM emp;

-- 2. dept만 수정하면 emp에는 예전 부서명 '홍보'가 남는다.
UPDATE dept
SET `name` = '마케팅'
WHERE `name` = '홍보';
SELECT * FROM dept;
SELECT * FROM emp;

-- 같은 이름을 저장한 사원 행들도 별도로 수정해야 한다.
UPDATE emp
SET deptName = '마케팅'
WHERE deptName = '홍보';
SELECT * FROM emp;

-- 3. 원래 상태로 복원한 뒤 구조 변경
UPDATE dept
SET `name` = '홍보'
WHERE `name` = '마케팅';
SELECT * FROM dept;
UPDATE emp
SET deptName = '홍보'
WHERE deptName = '마케팅';
SELECT * FROM emp;

-- 4. 부서명 대신 dept.id 값을 저장한다.
-- deptId 칼럼을 추가하는 것만으로 관계나 FOREIGN KEY 제약조건이 생기지는 않는다.
ALTER TABLE emp ADD COLUMN deptId INT UNSIGNED NOT NULL;

-- 이 실습에서 부서 번호는 홍보=1, 기획=2이다.
UPDATE emp
SET deptId = 1
WHERE deptName = '홍보';
UPDATE emp
SET deptId = 2
WHERE deptName = '기획';
SELECT * FROM emp;

-- 기존 부서명을 번호로 옮긴 뒤에 칼럼을 제거한다.
ALTER TABLE emp DROP COLUMN deptName;
SELECT * FROM emp;

-- 이제 dept의 이름만 수정한다. emp의 deptId는 바꿀 필요가 없다.
UPDATE dept
SET `name` = '마케팅'
WHERE `name` = '홍보';
SELECT * FROM dept;
SELECT * FROM emp;

-- 5. ON 없는 JOIN: MySQL에서는 실행되지만 사원 3명 x 부서 2개 = 6행이다.
-- 홍길동-기획, 임꺽정-마케팅처럼 실제 소속과 다른 조합까지 나온다.
SELECT emp.*, dept.name AS `부서명`
FROM emp
INNER JOIN dept;

-- 6. ON으로 emp.deptId와 dept.id가 같은 조합만 조회: 올바른 소속 3행
SELECT emp.*, dept.id, dept.name AS `부서명`
FROM emp
INNER JOIN dept
ON emp.deptId = dept.id;

-- 7. AS로 결과 칼럼명 지정, DATE()로 날짜 부분만 출력
SELECT emp.id AS `사원번호`,
       emp.name AS `사원명`,
       DATE(emp.regDate) AS `입사일`,
       dept.name AS `부서명`
FROM emp
INNER JOIN dept
ON emp.deptId = dept.id
ORDER BY `부서명`, `사원명`;

-- 테이블에도 별칭 E, D를 붙여 같은 결과 조회
SELECT E.id AS `사원번호`,
       E.name AS `사원명`,
       DATE(E.regDate) AS `입사일`,
       D.name AS `부서명`
FROM emp AS E
INNER JOIN dept AS D
ON E.deptId = D.id
ORDER BY `부서명`, `사원명`;

-- 8. 기획부서 번호 확인 후 김영희 추가
SELECT * FROM dept;
INSERT INTO emp
SET regDate = NOW(),
    `name` = '김영희',
    deptId = 2;
SELECT * FROM emp;

-- IT부서 생성 후 번호가 3인지 확인하고 김철수 추가
INSERT INTO dept
SET regDate = NOW(),
    `name` = 'IT';
SELECT * FROM dept;

INSERT INTO emp
SET regDate = NOW(),
    `name` = '김철수',
    deptId = 3;

-- 최종 결과: 사원 5명과 각 사원의 부서명
SELECT E.id AS `사원번호`,
       E.name AS `사원명`,
       DATE(E.regDate) AS `입사일`,
       D.name AS `부서명`
FROM emp AS E
INNER JOIN dept AS D
ON E.deptId = D.id
ORDER BY `부서명`, `사원명`;
