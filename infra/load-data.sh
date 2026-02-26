#!/bin/bash
# ============================================================
# load-data.sh
# CARD_TRANSACTION 테이블에 dat 파일 데이터를 적재합니다.
#
# 사전 조건:
#   - docker compose up 이 완료된 상태
#   - 로컬에 mysql client 설치되어 있어야 함
#
# 사용법:
#   bash load-data.sh <dat파일경로>
#
# 예시:
#   bash load-data.sh "C:/Users/admin/Desktop/card_data/EDU_DATA_F.dat"
# ============================================================

FILE_PATH=$1

if [ -z "$FILE_PATH" ]; then
  echo "❌ dat 파일 경로를 인자로 전달해주세요."
  echo "   사용법: bash load-data.sh <dat파일경로>"
  echo "   예시:   bash load-data.sh \"C:/Users/admin/Desktop/card_data/EDU_DATA_F.dat\""
  exit 1
fi

if [ ! -f "$FILE_PATH" ]; then
  echo "❌ 파일을 찾을 수 없습니다: $FILE_PATH"
  exit 1
fi

echo "=========================================="
echo " CARD_TRANSACTION 데이터 적재 시작"
echo " 파일: $FILE_PATH"
echo " 약 1~2분 소요됩니다..."
echo "=========================================="

mysql -h 127.0.0.1 -P 3307 -u root -p1234 --local-infile=1 card_db <<EOF
SET autocommit=0;
SET unique_checks=0;
SET foreign_key_checks=0;

LOAD DATA LOCAL INFILE '${FILE_PATH}'
INTO TABLE CARD_TRANSACTION
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\r\n'
IGNORE 1 LINES;

SET autocommit=1;
SET foreign_key_checks=1;
SET unique_checks=1;
COMMIT;
EOF

if [ $? -eq 0 ]; then
  echo ""
  echo "✅ 데이터 적재 완료"
else
  echo ""
  echo "❌ 데이터 적재 실패"
  exit 1
fi

echo ""
echo "적재된 행 수 확인 중..."
mysql -h 127.0.0.1 -P 3307 -u root -p1234 card_db \
  -e "SELECT COUNT(*) AS total_rows FROM CARD_TRANSACTION;"
