-- a2: 기존 데이터를 정리하면서 제약조건과 조회 조건을 적용한다.
-- MySQL 실습용. 실행하면 기존 a2 DB가 삭제되고 다시 만들어진다.
-- 학습용 실패 SQL은 전체 실행이 중단되지 않도록 주석으로 남겼다.

DROP DATABASE IF EXISTS `a2`;
CREATE DATABASE `a2`;
USE `a2`;

CREATE TABLE article (
    id INT,
    regDate DATETIME,
    title VARCHAR(100),
    `body` TEXT
);

-- 1. id를 생략해 NULL인 행 두 개 생성
INSERT INTO article
SET regDate = NOW(),
    title = '제목',
    `body` = '내용';

INSERT INTO article
SET regDate = NOW(),
    title = '제목',
    `body` = '내용';
SELECT * FROM article;

-- 실패: 기존 id에 NULL이 있어 NOT NULL을 바로 적용할 수 없다.
-- 실습 오류: Invalid use of NULL value (1138)
-- ALTER TABLE article MODIFY id INT NOT NULL;

-- 2. NULL을 0으로 바꾼 뒤 NOT NULL 적용
UPDATE article
SET id = 0;
ALTER TABLE article MODIFY id INT NOT NULL;
SELECT * FROM article;
DESC article;

-- 실패: NULL은 없어졌지만 id가 모두 0이라 PRIMARY KEY를 적용할 수 없다.
-- 실습 오류: Duplicate entry '0' for key 'article.PRIMARY' (1062)
-- ALTER TABLE article ADD PRIMARY KEY(id);

-- 3. 중복을 없앤 뒤 PRIMARY KEY 적용
-- LIMIT 1은 변경할 행 수만 제한한다. 어느 행인지는 지정하지 않는다.
UPDATE article
SET id = 1
WHERE id = 0
LIMIT 1;
SELECT * FROM article;

UPDATE article
SET id = 2
WHERE id = 0;
ALTER TABLE article ADD PRIMARY KEY(id);

-- PRIMARY KEY로 번호의 고유성을 보장하고 AUTO_INCREMENT로 새 번호를 부여한다.
ALTER TABLE article MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;
DESC article;

ALTER TABLE article MODIFY COLUMN regDate DATETIME NOT NULL;
ALTER TABLE article MODIFY COLUMN title VARCHAR(100) NOT NULL;
ALTER TABLE article MODIFY COLUMN `body` TEXT NOT NULL;
ALTER TABLE article MODIFY COLUMN id INT UNSIGNED NOT NULL AUTO_INCREMENT;

-- 4. ADD / CHANGE / MODIFY: 작성자 칼럼의 이름과 위치 변경
ALTER TABLE article ADD COLUMN writer VARCHAR(100) NOT NULL AFTER title;
ALTER TABLE article CHANGE `writer` `nickname` VARCHAR(100) NOT NULL;
ALTER TABLE article MODIFY COLUMN nickname VARCHAR(100) NOT NULL AFTER `body`;

-- hit를 추가/삭제/다시 추가하는 것은 DROP COLUMN 실습이므로 유지한다.
ALTER TABLE article ADD COLUMN hit INT UNSIGNED NOT NULL AFTER nickname;
DESC article;
ALTER TABLE article DROP COLUMN hit;
DESC article;
ALTER TABLE article ADD COLUMN hit INT UNSIGNED NOT NULL AFTER nickname;
DESC article;

-- NOT NULL이어도 빈 문자열은 들어갈 수 있다.
-- 위 ADD COLUMN 뒤 기존 행의 nickname은 '', hit는 0으로 채워진다.
UPDATE article
SET nickname = '무명'
WHERE nickname = '';

-- 5. id를 생략하면 AUTO_INCREMENT로 3~6이 부여된다.
INSERT INTO article
SET regDate = NOW(),
    title = '제목3',
    `body` = '내용3',
    nickname = '홍길순',
    hit = 10;

INSERT INTO article
SET regDate = NOW(),
    title = '제목4',
    `body` = '내용4',
    nickname = '홍길동',
    hit = 55;

INSERT INTO article
SET regDate = NOW(),
    title = '제목5',
    `body` = '내용5',
    nickname = '홍길동',
    hit = 10;

INSERT INTO article
SET regDate = NOW(),
    title = '제목6',
    `body` = '내용6',
    nickname = '임꺽정',
    hit = 100;
SELECT * FROM article;

-- 6. 조회 조건: 조회수 상위 3개 (hit가 같은 행 사이의 순서는 미지정)
SELECT *
FROM article
ORDER BY hit DESC
LIMIT 3;

-- '홍길'로 시작하는 작성자
SELECT *
FROM article
WHERE nickname LIKE '홍길%';

-- 두 조건 모두 만족
SELECT *
FROM article
WHERE hit >= 10 AND hit <= 55;

SELECT *
FROM article
WHERE nickname != '무명'
  AND hit <= 50;

-- 두 조건 중 하나 이상 만족
SELECT *
FROM article
WHERE nickname = '무명'
   OR hit >= 55;
