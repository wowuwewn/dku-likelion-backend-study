use mysql;
show tables;
desc `user`;
create database if not exists test;
use test;
show tables;
drop database if exists `a1`;
create database `a1`;
use `a1`;
show databases;
show tables;

create table article (
	title varchar(100),
	`body` text
	);
show tables;
desc article;
INSERT INTO article
SET title = '제목',
`body` = '내용';

select title
from article;

SELECT title, `body`
FROM article;

SELECT *
FROM article;

INSERT INTO article
SET title = '제목',
`body` = '내용';

select *
from article;

ALTER TABLE article ADD COLUMN id INT FIRST;

select *
from article;

UPDATE article
SET id = 1
WHERE id IS NULL;

UPDATE article
SET id = 2
LIMIT 1;

SELECT *
FROM article;

insert into article
set id = 3,
title = '제목3',
body = '내용3';

DELETE FROM article
WHERE id = 2;

alter table article
add column regDate datetime after id;

desc article;

SELECT *
FROM article;

update article 
set regDate = '2018-08-10 15:00:00'
where id = 1;

select *
from article;

SELECT NOW();

update article
set regDate = now()
where id = 3;

select *
from article;

-- =========================
-- 제약조건 실습 - a2
-- =========================

DROP DATABASE IF EXISTS `a2`;

CREATE DATABASE `a2`;

USE `a2`;

create table article(
	id int,
	regDate datetime,
	title varchar(100),
	`body` text
	)
	
select *
from article;

insert into article
set regDate = now(),
title= '제목',
`body` = '내용';

select *
from article;
INSERT INTO article
SET regDate = NOW(),
title = '제목',
`body` = '내용';

SELECT *
FROM article;

update article
set id = 0;

ALTER TABLE article
MODIFY id INT NOT NULL;

select * 
from article;

DESC article;

UPDATE article
SET id = 1
WHERE id = 0
LIMIT 1;

SELECT *
FROM article;

update article
set id = 2
where id = 0;

ALTER TABLE article
ADD PRIMARY KEY(id);

ALTER TABLE article
MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;

DESC article;

ALTER TABLE article MODIFY COLUMN regDate DATETIME NOT NULL;

ALTER TABLE article MODIFY COLUMN title VARCHAR(100) NOT NULL;

ALTER TABLE article MODIFY COLUMN `body` TEXT NOT NULL;

ALTER TABLE article
MODIFY COLUMN id INT UNSIGNED NOT NULL AUTO_INCREMENT;

ALTER TABLE article
ADD COLUMN writer VARCHAR(100) NOT NULL AFTER title;

ALTER TABLE article
CHANGE `writer` `nickname` VARCHAR(100) NOT NULL;

ALTER TABLE article
MODIFY COLUMN nickname VARCHAR(100) NOT NULL AFTER body;

ALTER TABLE article
ADD COLUMN hit INT UNSIGNED NOT NULL AFTER nickname;

DESC article;

ALTER TABLE article
DROP COLUMN hit;

DESC article;

ALTER TABLE article
ADD COLUMN hit INT UNSIGNED NOT NULL AFTER nickname;

DESC article;

ALTER TABLE article
MODIFY COLUMN regDate DATETIME NOT NULL;

ALTER TABLE article
MODIFY COLUMN title VARCHAR(100) NOT NULL;

DESC article;

UPDATE article
SET nickname = '무명'
WHERE nickname = '';

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

SELECT *
FROM article;

SELECT *
FROM article
ORDER BY hit DESC
LIMIT 3;

SELECT *
FROM article
WHERE nickname LIKE '홍길%';

SELECT *
FROM article
WHERE hit >= 10 AND hit <= 55;

SELECT *
FROM article
WHERE nickname != '무명'
AND hit <= 50;

SELECT *
FROM article
WHERE nickname = '무명'
OR hit >= 55;