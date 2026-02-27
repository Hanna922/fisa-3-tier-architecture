PowerShell 환경에서 발생할 수 있는 리다이렉션(`<`) 오류를 방지하고, 안전하게 테스트를 거쳐 전체 데이터를 적재할 수 있도록 가이드를 정리했습니다.

---

# 🚀 MySQL InnoDB Cluster & Data Setup Guide

이 가이드는 MySQL Group Replication 클러스터를 구성하고, 대용량 카드 거래 데이터를 안전하게 적재하는 절차를 안내합니다. **모든 명령어는 `infra` 폴더 내 터미널(PowerShell)에서 실행하는 것을 기준으로 합니다.**

## ✅ 사전 필수 체크

* `docker-compose.yml`에 `./scripts:/home/scripts` 볼륨 마운트가 설정되어 있어야 합니다.
* PowerShell 사용 시 `<` 연산자 대신 `Get-Content`와 파이프(`|`)를 사용해야 에러가 발생하지 않습니다.

---

## 1단계: 클러스터(Cluster) 등록하기

먼저 `mysqlsh` 스크립트를 실행하여 3개의 노드를 하나의 클러스터로 묶어줍니다.

```powershell
# 클러스터 구성 및 노드(node2, node3) 등록
docker exec -it fisa-mysql-node1 mysqlsh --js --file /home/scripts/register_cluster.js --uri root:1234@localhost:3306 --verbose=1

```

> **참고**: 실행 중 비밀번호 요청이 뜨면 `1234`를 입력하십시오. 작업 완료 후 `cluster.status()` 결과가 출력됩니다.

---

## 2단계: 데이터 적재 테스트 (setup_test.sql)

데이터 규모가 크므로, 먼저 쪼개진 테스트용 파일(`TEST_DATA.dat`)을 통해 적재 프로세스와 복제 상태를 점검합니다. **이 단계를 먼저 수행할 것을 강력히 권장합니다.**

### 방법 A: 호스트 터미널에서 즉시 실행 (추천)

```powershell
Get-Content ./scripts/setup_test.sql -Raw | docker exec -i fisa-mysql-node1 mysql --local-infile=1 -u root -p1234

```

### 방법 B: 컨테이너 내부 접속 후 실행

```powershell
# 1. 컨테이너 접속
docker exec -it fisa-mysql-node1 bash

# 2. 내부 경로에 있는 파일로 적재
mysql --local-infile=1 -u root -p1234 < /home/scripts/setup_test.sql

```

### 🔍 검증 (Verification)

적재 후, **Node1**과 **Node2** 각각에 접속하여 데이터 건수가 일치하는지 확인합니다.

```sql
-- 각 노드(node1, node2)에서 실행
mysql -u root -p1234 -e "SELECT COUNT(*) FROM card_db.CARD_TRANSACTION;"

```

---

## 3단계: 전체 데이터 적재하기 (setup.sql)

테스트 결과가 정상(건수 일치 및 한글 깨짐 없음)이라면 원본 데이터(`EDU_DATA_F.dat`)를 적재합니다.

### 방법 A: 호스트 터미널에서 즉시 실행

```powershell
Get-Content ./scripts/setup.sql -Raw | docker exec -i fisa-mysql-node1 mysql --local-infile=1 -u root -p1234

```

### 방법 B: 컨테이너 내부 접속 후 실행

```powershell
# 컨테이너 내부 bash 접속 상태에서
mysql --local-infile=1 -u root -p1234 < /home/scripts/setup.sql

```

---

## 📊 적재 확인 및 상태 체크

데이터 적재가 완료되면 최종적으로 클러스터의 동기화 상태와 데이터 무결성을 확인합니다.

| 작업 | 명령어 |
| --- | --- |
| **데이터 건수 확인** | `SELECT COUNT(*) FROM card_db.CARD_TRANSACTION;` |
| **데이터 샘플 확인** | `SELECT * FROM card_db.CARD_TRANSACTION LIMIT 10;` |
| **클러스터 상태 확인** | `mysqlsh --uri root:1234@localhost:3306 --js -e "dba.getCluster('dockercluster').status()"` |

---

**위의 순서대로 테스트 적재(2단계)를 먼저 진행해 보시고, Node2에서도 데이터 건수가 동일하게 조회되는지 확인해 보시겠어요?**