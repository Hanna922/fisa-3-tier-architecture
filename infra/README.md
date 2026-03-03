# MySQL InnoDB Cluster & Data Setup Guide

이 가이드는 MySQL Group Replication 클러스터를 구성하고, 대용량 카드 거래 데이터를 안전하게 적재하는 절차를 안내합니다. **모든 명령어는 `infra` 폴더 내 터미널에서 실행하는 것을 기준으로 합니다.**

## 사전 파일 세팅

```text
infra/
├── data/
│   ├── EDU_DATA_F.dat        # 원본 데이터 파일 (대용량)
│   └── TEST_DATA_F.dat       # 테스트용 데이터 파일 (소량 추출본)
└── scripts/
    ├── setup.sql             # 전체 데이터 적재용 SQL 스크립트
    └── setup_test.sql        # 테스트 데이터 적재용 SQL 스크립트
```

도커 띄우기

```bash
docker compose down -v
docker compose up -d --build
```

</br>

## 1단계: 클러스터 등록

먼저 `mysqlsh` 스크립트를 실행하여 3개의 노드를 하나의 클러스터로 묶어줍니다.

```bash
# 클러스터 구성 및 노드(node2, node3) 등록
docker exec -it fisa-mysql-node1 mysqlsh --js --file /home/scripts/register_cluster.js --uri root:1234@localhost:3306 --verbose=1
```

실행 중 비밀번호 요청이 뜨면 `1234`를 입력하세요. 작업 완료 후 `cluster.status()` 결과가 출력됩니다.


</br>

## 2단계: 데이터 적재 테스트 (setup_test.sql)

데이터 규모가 크므로, 먼저 쪼개진 테스트용 파일(`TEST_DATA.dat`)을 통해 적재 프로세스와 복제 상태를 점검합니다. 이 단계를 먼저 수행할 것을 권장합니다.

```bash
# 1. 컨테이너 접속
docker exec -it fisa-mysql-node1 /bin/bash

# 2. 내부 경로에 있는 파일로 적재
mysql --local-infile=1 -u root -p1234 < /home/scripts/setup_test.sql

# OR
docker exec fisa-mysql-node1 bash -c "mysql --local-infile=1 -u root -p1234 < /home/scripts/setup_test.sql"
```

적재 후, **모든 노드(Node1, Node2, Node3)** 각각에 접속하여 데이터 건수가 일치하는지 확인합니다.

```bash
docker exec -it fisa-mysql-node1 bash -c "mysql -u root -p1234 -e \"SELECT COUNT(*) FROM card_db.CARD_TRANSACTION;\""
docker exec -it fisa-mysql-node2 bash -c "mysql -u root -p1234 -e \"SELECT COUNT(*) FROM card_db.CARD_TRANSACTION;\""
docker exec -it fisa-mysql-node3 bash -c "mysql -u root -p1234 -e \"SELECT COUNT(*) FROM card_db.CARD_TRANSACTION;\""
```

</br>

## 3단계: 전체 데이터 적재하기 (setup.sql)

테스트 결과가 정상이라면 원본 데이터를 적재합니다.

```bash
docker exec -it fisa-mysql-node1 bash -c "mysql --local-infile=1 -u root -p1234 < /home/scripts/setup.sql"
```

</br>

## 4단계: 라우터 설정하기

라우터 컨테이너를 실행합니다. 

```bash
docker compose --profile router up -d
```
```bash
# 기존 메타데이터와 충돌 시 --force 옵션으로 bootstrap 덮어쓰기
docker exec -it fisa-mysql-router mysqlrouter \
  --bootstrap root:1234@fisa-mysql-node1:3306 \
  --user=mysqlrouter \
  --force
```

```bash
# 라우터 접속 테스트

# 6446 포트로 접속하여 현재 쓰기 가능한 노드 확인
mysql -u root -p1234 -h 127.0.0.1 -P 6446 -e "SELECT @@server_uuid, @@hostname;"

# 6447 포트로 접속하여 읽기 전용 노드로 연결되는지 확인
mysql -u root -p1234 -h 127.0.0.1 -P 6447 -e "SELECT @@server_uuid, @@hostname;"
```

</br>

---

# 참고 자료

MySQL Shell을 통해 컨테이너 내부에서 직접 DB 클러스터의 상태를 점검하거나 설정을 변경할 수 있습니다.

```bash
# 1. 컨테이너 접속 및 MySQL Shell 실행
docker exec -it fisa-mysql-node1 /bin/bash
mysqlsh -uroot -p1234

# 2. JavaScript 모드 전환 (JS 모드에서만 dba 객체 사용 가능)
\js

# 3. 기존 클러스터 객체 가져오기 및 상태 확인
var cluster = dba.getCluster("dockercluster")
cluster.status()  # 노드별 Health, Role(Primary/Secondary) 확인

# 4. (필요 시) 인스턴스 구성 적합성 체크
dba.checkInstanceConfiguration("root@fisa-mysql-node2:3306")

# 5. MySQL Shell 종료
\quit

```

