-- a1: 제목/내용만 있는 테이블에서 id와 작성일을 차례로 추가한다.
-- MySQL 실습용. 실행하면 기존 a1 DB가 삭제되고 다시 만들어진다.
-- mysql.user 구조 조회에는 해당 테이블을 읽을 수 있는 권한이 필요하다.

-- 1. DB와 테이블 확인
SHOW DATABASES;
USE mysql;
SHOW TABLES;

-- 실패 사례: 작은따옴표는 문자열 값에 사용하므로 테이블명을 이렇게 쓰면 안 된다.
-- DESC 'user';
DESC `user`;

CREATE DATABASE IF NOT EXISTS test;
USE test;
SHOW TABLES;

DROP DATABASE IF EXISTS `a1`;
CREATE DATABASE `a1`;
USE `a1`;
SHOW DATABASES;
SHOW TABLES;

-- 2. 제목과 내용만으로 시작
CREATE TABLE article (
    title VARCHAR(100),
    `body` TEXT
);
SHOW TABLES;
DESC article;

INSERT INTO article
SET title = '제목',
    `body` = '내용';

SELECT title FROM article;
SELECT title, `body` FROM article;
SELECT * FROM article;

-- 같은 내용의 두 행을 구분하기 어렵다.
INSERT INTO article
SET title = '제목',
    `body` = '내용';
SELECT * FROM article;

-- 3. id 추가: 기존 행에는 번호가 자동으로 채워지지 않고 NULL이 들어간다.
ALTER TABLE article ADD COLUMN id INT FIRST;
SELECT * FROM article;

-- NULL인 두 행이 모두 1로 바뀐다. 아직 고유한 번호가 아니다.
UPDATE article
SET id = 1
WHERE id IS NULL;
SELECT * FROM article;

-- 실습에서는 LIMIT 1로 한 행만 바꿔 번호를 구분했다.
-- ORDER BY가 없으므로 어느 행이 선택될지는 보장되지 않는다.
UPDATE article
SET id = 2
LIMIT 1;
SELECT * FROM article;

INSERT INTO article
SET id = 3,
    title = '제목3',
    `body` = '내용3';

DELETE FROM article
WHERE id = 2;
SELECT * FROM article;

-- 4. 작성일 추가: DATETIME은 날짜와 시간을 저장한다.
ALTER TABLE article ADD COLUMN regDate DATETIME AFTER id;
DESC article;
SELECT * FROM article;

UPDATE article
SET regDate = '2018-08-10 15:00:00'
WHERE id = 1;
SELECT * FROM article;

-- NOW()는 실행 시점의 날짜와 시간을 반환한다.
SELECT NOW();
UPDATE article
SET regDate = NOW()
WHERE id = 3;
SELECT * FROM article;
