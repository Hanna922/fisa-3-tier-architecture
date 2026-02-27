# 카드 소비 데이터 분석 서비스

라이프스테이지별 카드 소비 패턴을 분석하는 3-Tier 웹 애플리케이션입니다.

## 목차

1. [아키텍처 개요](#아키텍처-개요)
2. [기술 스택](#기술-스택)
3. [프로젝트 구조](#프로젝트-구조)
4. [데이터](#데이터)
5. [시작 가이드](#시작-가이드)
6. [API 엔드포인트](#api-엔드포인트)
7. [주요 설계 결정](#주요-설계-결정)
8. [트러블슈팅](#트러블슈팅)

## 아키텍처 개요

```
Client (Browser / curl)
        │
        ▼ HTTP :80
┌───────────────────┐
│       Nginx       │  Load Balancer
│(Docker Container) │  Round-Robin
└────────┬──────────┘
         │
    ┌────┴────┐
    ▼         ▼
 :8080      :8090
┌──────┐  ┌──────┐
│Tomcat│  │Tomcat│  WAS × 2 (Local Process)
│ WAS  │  │ WAS  │  Servlet/JSP, HikariCP
└──┬───┘  └──┬───┘
   └────┬────┘
        │ JDBC
   ┌────┴─────────────────┐
   │                      │
   ▼ :3307                ▼ :3308
┌──────────────┐  ┌──────────────────┐
│ MySQL Source │  │  MySQL Replica   │
│(Write/Read)  │◄─│   (Read Only)    │
│   Docker     │  │    Docker        │
└──────────────┘  └──────────────────┘
        ▲ Binlog Replication (GTID)

   ┌────────────┐
   │   Redis    │  Cache Layer
   │  :6379     │  TTL 3600s
   │  Docker    │
   └────────────┘
```

### 계층별 역할

| 계층         | 구성 요소      | 역할                                         |
| ------------ | -------------- | -------------------------------------------- |
| Presentation | Nginx          | HTTP 요청 수신, 두 대의 Tomcat으로 부하 분산 |
| Application  | Tomcat WAS × 2 | 비즈니스 로직 처리, DB 접근, 캐시 조회       |
| Cache        | Redis          | 반복 조회 결과 캐싱, DB 부하 절감            |
| Data         | MySQL Source   | 쓰기/읽기 겸용 (Source)                      |
| Data         | MySQL Replica  | 읽기 전용 (Replica), Source와 실시간 동기화  |

### Source / Replica 분기 전략

```
읽기 요청 (SELECT)
  → LifeStageDao(REPLICA_DS)
      → MySQL Replica :3308

쓰기 요청 (INSERT / UPDATE / DELETE)
  → XxxDao(SOURCE_DS)
      → MySQL Source :3307
```

같은 hostname으로 포트를 분리하는 방식은 Docker 포트 매핑으로 구현합니다.

- Source: `localhost:3307` → Docker 컨테이너 내부 `3306`
- Replica: `localhost:3308` → Docker 컨테이너 내부 `3306`

### Redis Cache-Aside 패턴

```
요청 → jedis.get("life-stages:all")
           │
      ┌────┴────┐
      │ null?   │
      ├─ Yes ───► MySQL 조회 → jedis.setex(key, 3600, json) → 응답
      └─ No ────► JSON 역직렬화 → 응답  (DB 미접근)
```

## 기술 스택

| 분류            | 기술                    | 버전            |
| --------------- | ----------------------- | --------------- |
| Language        | Java                    | 21              |
| WAS             | Apache Tomcat           | 9.0.115         |
| Web API         | Servlet / JSP           | Java EE 4.0     |
| Connection Pool | HikariCP                | 5.0.1           |
| JDBC Driver     | MySQL Connector/J       | 8.4.0           |
| Cache Client    | Jedis                   | 5.1.0           |
| JSON            | Gson                    | 2.10.1          |
| Logging         | SLF4J + Logback         | 2.0.16 / 1.5.15 |
| Boilerplate     | Lombok                  | 1.18.38         |
| Load Balancer   | Nginx                   | latest          |
| Cache           | Redis                   | 7.2-alpine      |
| Database        | MySQL                   | 8.4             |
| Container       | Docker / Docker Compose | -               |
| IDE             | Eclipse                 | -               |

## 프로젝트 구조

```
sample-workspace/
├── README.md
├── infra/                          # 인프라 설정
│   ├── docker-compose.yml          # 컨테이너 정의 (Nginx, MySQL×2, Redis)
│   ├── setup-replication.sh        # MySQL Source-Replica 복제 초기 설정
│   ├── load-data.sh                # 데이터 적재 (Linux/Mac)
│   ├── load-data.bat               # 데이터 적재 (Windows cmd)
│   ├── nginx/
│   │   ├── Dockerfile
│   │   └── nginx.conf              # upstream 정의, proxy_set_header 설정
│   ├── mysql-source/
│   │   ├── Dockerfile
│   │   ├── my.cnf                  # binlog 활성화, GTID 설정
│   │   └── init.sql                # CARD_TRANSACTION DDL, replicator 계정 생성
│   ├── mysql-replica/
│   │   ├── Dockerfile
│   │   └── my.cnf                  # read_only, GTID 설정
│   └── mysql-source/*.sql          # 분석 쿼리 모음
│
├── libraries/                      # 공유 JAR 파일
│   ├── HikariCP-5.0.1.jar
│   ├── jedis-5.1.0.jar
│   ├── gson-2.10.1.jar
│   ├── commons-pool2-2.12.0.jar
│   ├── mysql-connector-j-8.4.0.jar
│   ├── slf4j-api-2.0.16.jar
│   ├── logback-*.jar
│   └── lombok-1.18.38.jar
│
└── sample-project/                 # Java 웹 애플리케이션
    └── src/main/
        ├── java/dev/sample/
        │   ├── ApplicationContextListener.java   # HikariCP + Redis 초기화
        │   ├── servlet/
        │   │   └── LifeStagesServlet.java         # GET /life-stages
        │   ├── dao/
        │   │   └── LifeStageDao.java              # Replica DB 조회
        │   └── test/                              # 헬스체크 서블릿
        │       ├── HikariHealthCheckServlet.java
        │       ├── LogTestServlet.java
        │       └── LombokTestServlet.java
        ├── resources/
        │   ├── jdbc.properties         # DB 접속 정보 (gitignore 대상)
        │   ├── jdbc.properties.example # 접속 정보 템플릿
        │   └── logback.xml
        └── webapp/WEB-INF/
            ├── web.xml
            └── views/life-stages/list.html
```

### 분석 쿼리 목록 (`infra/mysql-source/`)

| 파일                              | 설명                                                 |
| --------------------------------- | ---------------------------------------------------- |
| `creditOrNot.sql`                 | 라이프스테이지별 신용카드 vs 체크카드 이용 금액 비중 |
| `creditOrNot_by_lifestage.sql`    | 위 쿼리의 라이프스테이지 필터 버전                   |
| `membershipTier.sql`              | 회원등급별 소비 분석                                 |
| `membershipTier_by_lifestage.sql` | 라이프스테이지 × 회원등급 교차 분석                  |
| `necessaryOrNot.sql`              | 필수/선택 소비 비율 분석                             |
| `necessaryOrNot_by_lifestage.sql` | 라이프스테이지별 필수/선택 소비 비율                 |
| `top5.sql`                        | 라이프스테이지별 TOP 5 업종 (CTE + WINDOW 함수)      |
| `top5_by_lifestage.sql`           | 특정 라이프스테이지 TOP 5 업종                       |

## 데이터

- 테이블: `CARD_TRANSACTION`
- 규모: **약 538만 행** (5,382,734 rows)
- 주요 컬럼

| 컬럼             | 설명                                     |
| ---------------- | ---------------------------------------- |
| `LIFE_STAGE`     | 라이프스테이지 (분석 기준 주요 컬럼)     |
| `AGE`            | 연령대                                   |
| `SEX_CD`         | 성별                                     |
| `MBR_RK`         | 회원등급                                 |
| `TOT_USE_AM`     | 총 이용금액                              |
| `CRDSL_USE_AM`   | 신용카드 이용금액                        |
| `CNF_USE_AM`     | 체크카드 이용금액                        |
| 업종별 금액 컬럼 | INTERIOR_AM, INSUHOS_AM, TRVL_AM 외 다수 |

## 시작 가이드

### 사전 요구 사항

- Docker Desktop 실행 중
- MySQL Client 설치 (데이터 적재 시 필요)
- Eclipse + Apache Tomcat 9.0 설치

### 1단계: 인프라 실행

```bash
cd infra

# 컨테이너 빌드 및 실행
docker compose up --build -d

# MySQL Source-Replica 복제 설정 (최초 1회)
bash setup-replication.sh
```

### 2단계: 데이터 적재

```bash
# Windows (cmd에서 실행)
load-data.bat "C:\Users\{username}\Desktop\EDU_DATA_F.dat"

# Linux / Mac
bash load-data.sh "/path/to/EDU_DATA_F.dat"
```

### 3단계: jdbc.properties 파일에 DB 접속 정보 설정

> `jdbc.properties`는 `.gitignore`에 등록되어 있습니다. 민감 정보를 커밋하지 않도록 주의하세요.

### 4단계: Eclipse에서 Tomcat 실행

1. Eclipse `Servers` 탭 → Tomcat 서버 더블클릭
2. `Modules` 탭 → `sample-project`가 등록되어 있는지 확인
   - 없으면 `Add Web Module...` → `sample-project` 추가
3. 8080, 8090 두 서버 모두 **Stop → Clean → Start**

### 동작 확인

```bash
# Nginx 경유 (권장)
curl http://localhost/sample-project/life-stages

# Tomcat 직접
curl http://localhost:8080/sample-project/life-stages
curl http://localhost:8090/sample-project/life-stages

# Redis 캐시 확인 (첫 요청 후)
docker exec fisa-redis redis-cli GET "life-stages:all"

# HikariCP 헬스체크
curl http://localhost/sample-project/test/hikari
```

## API 엔드포인트

| 경로                          | 메서드 | 설명                       | DataSource           |
| ----------------------------- | ------ | -------------------------- | -------------------- |
| `/sample-project/life-stages` | GET    | 라이프스테이지별 소비 집계 | Replica (Redis 캐시) |
| `/sample-project/test/hikari` | GET    | HikariCP 커넥션 헬스체크   | -                    |
| `/sample-project/test/log`    | GET    | Logback 로깅 테스트        | -                    |
| `/sample-project/test/lombok` | GET    | Lombok 동작 테스트         | -                    |

## 주요 설계 결정

### HikariCP Source/Replica 분리

단일 DataSource 대신 두 개의 HikariCP 풀을 유지합니다.

```java
// ApplicationContextListener.java
sourceConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
sourceConfig.setJdbcUrl(props.getProperty("source.url"));   // :3307
replicaConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
replicaConfig.setJdbcUrl(props.getProperty("replica.url")); // :3308
```

- 읽기가 집중되는 서비스에서 Replica로 SELECT 부하를 분산
- Source는 쓰기 작업 전용으로 보호
- 두 풀 모두 `ServletContext`에 등록하여 서블릿에서 정적 메서드로 접근

### DB 접속 정보 외부화 (`jdbc.properties`)

하드코딩 대신 클래스패스 리소스로 분리합니다.

```java
ClassLoader cl = Thread.currentThread().getContextClassLoader();
try (InputStream is = cl.getResourceAsStream("jdbc.properties")) {
    props.load(is);
}
```

- 환경별(개발/운영) 접속 정보 교체 용이
- `.gitignore`에 등록하여 민감 정보 커밋 방지
- `jdbc.properties.example`을 함께 제공하여 온보딩 편의성 확보

### Redis Cache-Aside (대용량 데이터 대응)

538만 행의 집계 쿼리는 최초 1회만 실행합니다.

```
첫 요청:  Cache Miss → MySQL GROUP BY 쿼리 → Redis setex(TTL=3600) → 응답
이후 요청: Cache Hit  → Redis GET → 응답 (DB 미접근)
```

- TTL 3600초(1시간) 후 자동 만료 → 다음 요청 시 재적재
- Gson으로 `List<Map<String, Object>>`를 JSON 직렬화/역직렬화

### SQL 최적화 (CTE 단일 스캔)

초기 UNION ALL 방식(풀스캔 8회 = 4,300만 행)을 CTE로 개선합니다.

```sql
-- Before: UNION ALL 8회 → CARD_TRANSACTION을 8번 풀스캔
SELECT 'INTERIOR' AS CATEGORY, SUM(INTERIOR_AM) FROM CARD_TRANSACTION
UNION ALL
SELECT 'HOSPITAL',              SUM(HOS_AM)      FROM CARD_TRANSACTION
-- ... 6개 더

-- After: CTE 1회 스캔 → 집계 결과(수십 행)를 UNION ALL
WITH agg AS (
  SELECT LIFE_STAGE, SUM(INTERIOR_AM) AS INTERIOR, ... FROM CARD_TRANSACTION GROUP BY LIFE_STAGE
)
SELECT LIFE_STAGE, 'INTERIOR' AS CATEGORY, INTERIOR FROM agg
UNION ALL SELECT LIFE_STAGE, 'HOSPITAL', HOSPITAL FROM agg
-- ...
```

## 트러블슈팅

### 1. HikariCP `Failed to get driver instance`

**증상**

```
SEVERE: Exception sending context initialized event to listener instance of class
[dev.sample.ApplicationContextListener]
java.lang.RuntimeException: Failed to get driver instance for
jdbcUrl=jdbc:mysql://localhost:3307/card_db
```

**원인**

`driverClassName`을 명시하지 않으면 HikariCP가 `DriverManager.getDriver(url)`로 드라이버를 탐색하는데, Servlet 컨테이너의 클래스로더 격리로 인해 MySQL Connector/J 자동 등록이 실패할 수 있습니다.

`contextInitialized()`에서 예외가 발생하면 Tomcat이 웹 애플리케이션 전체를 비활성화하므로, 모든 경로가 404를 반환합니다.

**해결**

```java
// HikariConfig에 드라이버 클래스를 명시적으로 지정
sourceConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
replicaConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
```

### 2. `LifeStageDao.findAll()` 테이블 없음 오류

**증상**

```
SEVERE: Servlet.service() for servlet [dev.sample.servlet.LifeStagesServlet] threw exception
java.lang.RuntimeException: LifeStageDao.findAll() 실행 중 오류 발생
```

**원인**

DAO 골격의 플레이스홀더 SQL이 `SELECT * FROM life_stages`로 작성되어 있었으나, 실제 DB에는 `CARD_TRANSACTION` 테이블만 존재합니다.

**해결**

```java
String sql = "SELECT LIFE_STAGE"
           + "     , COUNT(*)          AS CNT"
           + "     , SUM(TOT_USE_AM)   AS TOT_USE_AM"
           + "     , SUM(CRDSL_USE_AM) AS CRDSL_USE_AM"
           + "     , SUM(CNF_USE_AM)   AS CNF_USE_AM"
           + "  FROM CARD_TRANSACTION"
           + " GROUP BY LIFE_STAGE"
           + " ORDER BY LIFE_STAGE";
```

### 3. Windows에서 `docker exec -it` TTY 오류

**증상**

```
the input device is not a TTY.
If you are using mintty, try prefixing the command with 'winpty'
```

**원인**

Windows Git Bash(mintty)는 `-it` 플래그가 요구하는 TTY를 제공하지 않습니다.

**해결**

```bash
# 방법 1: -it 제거 (단순 조회는 인터랙션 불필요)
docker exec fisa-redis redis-cli GET "life-stages:all"

# 방법 2: winpty 접두어
winpty docker exec -it fisa-redis redis-cli GET "life-stages:all"
```

---

### 4. `load-data.sh` Windows 미동작

**원인**

`.sh` 파일은 Windows 명령 프롬프트(cmd)에서 실행할 수 없습니다.

**해결**

`load-data.bat`을 별도 작성하여 Windows 환경을 지원합니다.

- `$1` → `%~1` (인자 따옴표 자동 제거)
- `[ -z "$VAR" ]` → `"%VAR%"==""`
- `$?` → `%ERRORLEVEL%`
- `<<EOF ... EOF` Heredoc → `-e "..."` 한 줄 방식
- 경로 역슬래시 변환: `set "PATH_FWD=%PATH:\=/%"` (LOAD DATA LOCAL INFILE은 슬래시 필요)

```cmd
# cmd에서 실행 (Git Bash 불가)
cd D:\sample-workspace\infra
load-data.bat "C:\Users\{username}\Desktop\EDU_DATA_F.dat"
```

---

### 5. MySQL 8.4 SHOW MASTER STATUS 제거

**원인**

MySQL 8.4부터 `SHOW MASTER STATUS`가 제거되었습니다.

**해결**

```sql
-- Before (MySQL 8.0 이하)
SHOW MASTER STATUS;

-- After (MySQL 8.4+)
SHOW BINARY LOG STATUS;
```

`setup-replication.sh`에 반영되어 있습니다.
