# Infra 실행 가이드

## 최초 실행

```bash
# 1. 컨테이너 시작
docker compose up --build -d

# 2. 복제 설정
bash setup-replication.sh

# 3. 데이터 적재 (한 번만 실행)
bash load-data.sh "<dat 파일 경로>"
```

## 재시작 (볼륨 유지)

```bash
docker compose down
docker compose up -d
```

## 초기화 후 재시작 (볼륨 삭제)

```bash
docker compose down -v
docker compose up --build -d
bash setup-replication.sh
bash load-data.sh "<dat 파일 경로>"
```
