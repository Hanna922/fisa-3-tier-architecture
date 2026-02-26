-- ============================================================
-- init.sql
-- 최초 컨테이너 실행 시 자동으로 실행됨
-- 1. 초기 스키마 생성
-- 2. 복제용 전용 계정 생성 (Replica가 이 계정으로 Source에 접속)
-- ============================================================

-- DB 선택
USE card_db;

-- ----------------------------------------------------------
-- 복제 전용 계정 생성
-- Replica 컨테이너가 이 계정으로 Source에 접속해서 binlog를 읽음
-- '%' = 어느 호스트에서든 접속 허용 (Docker 네트워크 내부)
-- ----------------------------------------------------------
CREATE USER IF NOT EXISTS 'replicator'@'%' IDENTIFIED BY '1234';
GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';
FLUSH PRIVILEGES;

-- ----------------------------------------------------------
-- 예시 테이블 (서블릿 프로젝트에 맞게 수정하세요)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(100),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS board (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     TEXT,
    author_id   INT,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------
-- 테스트 데이터
-- ----------------------------------------------------------
INSERT INTO users (username, password, email) VALUES
    ('admin', '1234', 'admin@fisa.dev'),
    ('user1', '1234', 'user1@fisa.dev');

INSERT INTO board (title, content, author_id) VALUES
    ('첫 번째 글', '안녕하세요!', 1),
    ('두 번째 글', '테스트 데이터입니다.', 2);
