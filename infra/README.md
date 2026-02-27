# Infra 실행 가이드

## 최초 실행 (기존 설치 이력 없음)

```bash
# 1. 컨테이너 시작 (mysql-router는 이 단계에서 제외됨)
docker compose up --build -d

# 2. InnoDB Cluster 구성 (node3 데이터는 Clone Plugin이 자동 복사)
bash setup-cluster.sh

# 3. 데이터 적재 (한 번만 실행)
bash load-data.sh "<dat 파일 경로>"

# 4. MySQL Router 기동
docker compose --profile router up -d
```

## 기존 설치에서 전환 (mysql-source/replica → node1/2/3)

```bash
# 1. 컨테이너 중지 (볼륨 유지 — -v 옵션 사용 금지)
docker compose down

# 2. 컨테이너 기동 (mysql-source-data, mysql-replica-data 볼륨 그대로 마운트됨)
docker compose up --build -d

# 3. InnoDB Cluster 구성
bash setup-cluster.sh

# 4. MySQL Router 기동
docker compose --profile router up -d
```

## 재시작 (볼륨 유지)

```bash
docker compose down
docker compose up -d
docker compose --profile router up -d
```

## 초기화 후 재시작 (볼륨 삭제)

```bash
docker compose down -v
docker compose up --build -d
bash setup-cluster.sh
bash load-data.sh "<dat 파일 경로>"
docker compose --profile router up -d
```

## 클러스터 상태 확인

```bash
docker exec -it fisa-mysql-shell mysqlsh --uri root:1234@mysql-node1:3306 \
    --js -e "var c = dba.getCluster(); print(JSON.stringify(c.status(), null, 2));"
```

---

## profiles: [router] 란?

Docker Compose의 `profiles` 기능은 서비스를 논리적 그룹으로 분리해, **기본 실행(`docker compose up`)에서 제외**할 수 있게 합니다.

```yaml
mysql-router:
  profiles:
    - router   # 이 서비스는 기본 up 대상에서 제외됨
```

| 명령어 | router 기동 여부 |
|---|---|
| `docker compose up -d` | ❌ 제외 |
| `docker compose --profile router up -d` | ✅ 포함 |

### 이 프로젝트에서 사용하는 이유

MySQL Router는 InnoDB Cluster가 완전히 구성된 후에야 정상 기동됩니다.
Router가 기동 시 클러스터 메타데이터를 읽어 bootstrap하기 때문에, 클러스터가 없는 상태에서 먼저 뜨면 오류가 발생합니다.

```
일반 서비스 (mysql-node1/2/3, redis 등)
    └── docker compose up -d 로 기동

클러스터 구성 완료 (setup-cluster.sh)
    └── docker compose --profile router up -d 로 기동
```

`depends_on`의 healthcheck 조건과 조합해, 세 노드가 모두 healthy 상태가 된 후에만 Router가 시작되도록 보장합니다.
