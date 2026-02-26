#!/bin/bash
# ============================================================
# setup-replication.sh
# docker-compose up 후 딱 한 번 실행하면 됩니다.
# 실행 위치: infra/ 에서 실행
# 사용법: bash setup-replication.sh
# ============================================================

echo "=========================================="
echo " MySQL Source/Replica 복제 설정 시작"
echo "=========================================="

# Source 헬스체크 대기
echo "[0단계] Source MySQL 준비 대기 중..."
until docker exec fisa-mysql-source mysqladmin ping -u root -p1234 --silent; do
  echo "  대기 중..."
  sleep 2
done
echo "✅ Source 준비 완료"

# ----------------------------------------------------------
# 1. Source의 바이너리 로그 위치 확인 (GTID 모드 - 참고용)
# ----------------------------------------------------------
echo ""
echo "[1단계] Source 바이너리 로그 상태 확인 중..."

# MySQL 8.4에서 SHOW MASTER STATUS 제거됨 → SHOW BINARY LOG STATUS 사용
BINARY_LOG_STATUS=$(docker exec fisa-mysql-source mysql \
  -u root -p1234 \
  --skip-column-names \
  -e "SHOW BINARY LOG STATUS;")

if [ $? -ne 0 ] || [ -z "$BINARY_LOG_STATUS" ]; then
  echo ""
  echo "❌ Source 바이너리 로그 상태 조회 실패."
  echo "   아래 명령어로 직접 확인해보세요:"
  echo "   docker exec fisa-mysql-source mysql -u root -p1234 -e \"SHOW BINARY LOG STATUS\""
  exit 1
fi

echo "✅ Binary Log 상태: $BINARY_LOG_STATUS"
echo "   (GTID 모드 사용 중 - FILE/POS 불필요)"

# ----------------------------------------------------------
# 2. Replica에 Source 연결 정보 설정
# ----------------------------------------------------------
echo ""
echo "[2단계] Replica에 Source 연결 정보 설정 중..."

# GTID 방식 - FILE/POS 지정 필요 없음
docker exec fisa-mysql-replica mysql \
  -u root -p1234 \
  -e "STOP REPLICA; CHANGE REPLICATION SOURCE TO SOURCE_HOST='mysql-source', SOURCE_USER='replicator', SOURCE_PASSWORD='1234', SOURCE_AUTO_POSITION=1, GET_SOURCE_PUBLIC_KEY=1; START REPLICA;"
if [ $? -eq 0 ]; then
  echo "✅ Replica 설정 완료"
else
  echo "❌ Replica 설정 실패"
  exit 1
fi

# ----------------------------------------------------------
# 3. 복제 상태 확인
# ----------------------------------------------------------
echo ""
echo "[3단계] 복제 상태 확인 중... (5초 대기)"
sleep 5

docker exec fisa-mysql-replica mysql \
  -u root -p1234 \
  -e "SHOW REPLICA STATUS\G" | grep -E "Replica_IO_Running|Replica_SQL_Running|Seconds_Behind_Source|Last_Error"

echo ""
echo "=========================================="
echo " Replica_IO_Running: Yes"
echo " Replica_SQL_Running: Yes"
echo " 복제 정상 동작 중. 스크립트 종료"
echo "=========================================="