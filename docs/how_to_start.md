
### 사전 요구 사항

- Docker Desktop 실행 중
- Eclipse + Apache Tomcat 9.0 설치

### 1단계: 인프라 실행

```bash
cd infra

docker compose down -v
docker compose up -d --build
```

### 2단계: InnoDB Cluster 구성, 데이터 적재

[infra/README.md](../infra/README.md)를 참고해주세요.

### 3단계: jdbc.properties 생성

> `jdbc.properties`는 `.gitignore`에 등록되어 있습니다. 민감 정보를 커밋하지 않도록 주의하세요.

```bash
cp sample-project/src/main/resources/jdbc.properties.example \
   sample-project/src/main/resources/jdbc.properties
# 이후 jdbc.properties에 실제 username, password 입력
```

### 4단계: app_user 계정 생성 (로그인용)

```bash
docker exec fisa-mysql-node1 mysql -u root -p1234 -e \
  "USE card_db; CREATE TABLE IF NOT EXISTS app_user (user_id VARCHAR(50) PRIMARY KEY, password VARCHAR(100), role VARCHAR(20)); INSERT INTO app_user VALUES ('admin', '1234', 'ADMIN');"

# Windows
docker exec fisa-mysql-node1 mysql -u root -p1234 -e "USE card_db; CREATE TABLE IF NOT EXISTS app_user (user_id VARCHAR(50) PRIMARY KEY, password VARCHAR(100), role VARCHAR(20)); INSERT INTO app_user VALUES ('admin', '1234', 'ADMIN');"
```

### 5단계: Eclipse에서 Tomcat 실행

1. Eclipse `Servers` 탭 → Tomcat 서버 더블클릭
2. `Modules` 탭 → `sample-project`가 등록되어 있는지 확인
   - 없으면 `Add Web Module...` → `sample-project` 추가
3. 8080, 8090 두 서버 모두 **Stop → Clean → Start**

### 동작 확인

```bash
# 로그인 페이지
curl http://localhost/sample-project/login

# Redis 캐시 확인 (life-stages 첫 요청 후)
docker exec fisa-redis redis-cli KEYS "life-stages*"

# HikariCP 헬스체크
curl http://localhost/sample-project/test/hikari
```