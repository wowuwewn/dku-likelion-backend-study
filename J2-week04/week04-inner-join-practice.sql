-- ========================================
-- J2-week04 INNER JOIN 실습
-- dept / emp
-- ========================================

DROP DATABASE IF EXISTS a5;

CREATE DATABASE a5;

USE a5;

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

SELECT *
FROM dept;

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

SELECT *
FROM emp;

UPDATE dept
SET `name` = '마케팅'
WHERE `name` = '홍보';

SELECT *
FROM dept;

SELECT *
FROM emp;

UPDATE emp
SET deptName = '마케팅'
WHERE deptName = '홍보';

select *
from emp;

UPDATE dept
SET `name` = '홍보'
WHERE `name` = '마케팅';

SELECT *
FROM dept;

UPDATE emp
SET deptName = '홍보'
WHERE deptName = '마케팅';

SELECT *
FROM emp;

ALTER TABLE emp
ADD COLUMN deptId INT UNSIGNED NOT NULL;

UPDATE emp
SET deptId = 1
WHERE deptName = '홍보';

UPDATE emp
SET deptId = 2
WHERE deptName = '기획';

SELECT *
FROM emp;

ALTER TABLE emp
DROP COLUMN deptName;

SELECT *
FROM emp;

UPDATE dept
SET `name` = '마케팅'
WHERE `name` = '홍보';

SELECT *
FROM dept;

SELECT *
FROM emp;

SELECT emp.*, dept.name AS `부서명`
FROM emp
INNER JOIN dept;

SELECT emp.*, dept.id, dept.name AS `부서명`
FROM emp
INNER JOIN dept
ON emp.deptId = dept.id;

SELECT emp.id AS `사원번호`,
       emp.name AS `사원명`,
       DATE(emp.regDate) AS `입사일`,
       dept.name AS `부서명`
FROM emp
INNER JOIN dept
ON emp.deptId = dept.id
ORDER BY `부서명`, `사원명`;

SELECT E.id AS `사원번호`,
       E.name AS `사원명`,
       DATE(E.regDate) AS `입사일`,
       D.name AS `부서명`
FROM emp AS E
INNER JOIN dept AS D
ON E.deptId = D.id
ORDER BY `부서명`, `사원명`;

SELECT *
FROM dept;

INSERT INTO emp
SET regDate = NOW(),
`name` = '김영희',
deptId = 2;

SELECT *
FROM emp;

INSERT INTO dept
SET regDate = NOW(),
`name` = 'IT';

SELECT *
FROM dept;

INSERT INTO emp
SET regDate = NOW(),
`name` = '김철수',
deptId = 3;

SELECT E.id AS `사원번호`,
       E.name AS `사원명`,
       DATE(E.regDate) AS `입사일`,
       D.name AS `부서명`
FROM emp AS E
INNER JOIN dept AS D
ON E.deptId = D.id
ORDER BY `부서명`, `사원명`;

