-- ============================================================
-- init.sql
-- 최초 컨테이너 실행 시 자동으로 실행됨
-- 1. 복제용 계정 생성
-- 2. CARD_TRANSACTION 테이블 생성
-- 데이터 적재는 load-data.sh 로 별도 실행
-- ============================================================

USE card_db;

-- ----------------------------------------------------------
-- 복제 전용 계정 생성
-- ----------------------------------------------------------
CREATE USER IF NOT EXISTS 'replicator'@'%' IDENTIFIED BY '1234';
GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';
FLUSH PRIVILEGES;

-- ----------------------------------------------------------
-- CARD_TRANSACTION 테이블 생성
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `CARD_TRANSACTION` (
  `BAS_YH`           CHAR(6)       COMMENT '기준시점(분기)',
  `SEQ`              VARCHAR(20)   COMMENT '고객번호',
  `AGE`              CHAR(2)       COMMENT '연령대',
  `SEX_CD`           CHAR(2)       COMMENT '성별',
  `MBR_RK`           CHAR(2)       COMMENT '회원등급',
  `ATT_YM`           CHAR(6)       COMMENT '입회년월',
  `HOUS_SIDO_NM`     VARCHAR(40)   COMMENT '거주지역_1',
  `DIGT_CHNL_REG_YN` CHAR(1)       COMMENT '디지털채널가입여부',
  `DIGT_CHNL_USE_YN` CHAR(1)       COMMENT '디지털채널이용여부(당월)',
  `LIFE_STAGE`       VARCHAR(40)   COMMENT '라이프스테이지',
  `TOT_USE_AM`       DECIMAL(18,0) COMMENT '총이용금액',
  `CRDSL_USE_AM`     DECIMAL(18,0) COMMENT '신용카드이용금액',
  `CNF_USE_AM`       DECIMAL(18,0) COMMENT '체크카드이용금액',
  `INTERIOR_AM`      DECIMAL(18,0) COMMENT '가전/가구/주방용품',
  `INSUHOS_AM`       DECIMAL(18,0) COMMENT '보험/병원',
  `OFFEDU_AM`        DECIMAL(18,0) COMMENT '사무통신/서적/학원',
  `TRVLEC_AM`        DECIMAL(18,0) COMMENT '여행/레져/문화',
  `FSBZ_AM`          DECIMAL(18,0) COMMENT '요식업',
  `SVCARC_AM`        DECIMAL(18,0) COMMENT '용역/수리/건축자재',
  `DIST_AM`          DECIMAL(18,0) COMMENT '유통',
  `PLSANIT_AM`       DECIMAL(18,0) COMMENT '보건위생',
  `CLOTHGDS_AM`      DECIMAL(18,0) COMMENT '의류/신변잡화',
  `AUTO_AM`          DECIMAL(18,0) COMMENT '자동차/연료/정비',
  `FUNITR_AM`        DECIMAL(18,0) COMMENT '가구',
  `APPLNC_AM`        DECIMAL(18,0) COMMENT '가전제품',
  `HLTHFS_AM`        DECIMAL(18,0) COMMENT '건강식품',
  `BLDMNG_AM`        DECIMAL(18,0) COMMENT '건물및시설관리',
  `ARCHIT_AM`        DECIMAL(18,0) COMMENT '건축/자재',
  `OPTIC_AM`         DECIMAL(18,0) COMMENT '광학제품',
  `AGRICTR_AM`       DECIMAL(18,0) COMMENT '농업',
  `LEISURE_S_AM`     DECIMAL(18,0) COMMENT '레져업소',
  `LEISURE_P_AM`     DECIMAL(18,0) COMMENT '레져용품',
  `CULTURE_AM`       DECIMAL(18,0) COMMENT '문화/취미',
  `SANIT_AM`         DECIMAL(18,0) COMMENT '보건/위생',
  `INSU_AM`          DECIMAL(18,0) COMMENT '보험',
  `OFFCOM_AM`        DECIMAL(18,0) COMMENT '사무/통신기기',
  `BOOK_AM`          DECIMAL(18,0) COMMENT '서적/문구',
  `RPR_AM`           DECIMAL(18,0) COMMENT '수리서비스',
  `HOTEL_AM`         DECIMAL(18,0) COMMENT '숙박업',
  `GOODS_AM`         DECIMAL(18,0) COMMENT '신변잡화',
  `TRVL_AM`          DECIMAL(18,0) COMMENT '여행업',
  `FUEL_AM`          DECIMAL(18,0) COMMENT '연료판매',
  `SVC_AM`           DECIMAL(18,0) COMMENT '용역서비스',
  `DISTBNP_AM`       DECIMAL(18,0) COMMENT '유통업비영리',
  `DISTBP_AM`        DECIMAL(18,0) COMMENT '유통업영리',
  `GROCERY_AM`       DECIMAL(18,0) COMMENT '음식료품',
  `HOS_AM`           DECIMAL(18,0) COMMENT '의료기관',
  `CLOTH_AM`         DECIMAL(18,0) COMMENT '의류',
  `RESTRNT_AM`       DECIMAL(18,0) COMMENT '일반/휴게음식',
  `AUTOMNT_AM`       DECIMAL(18,0) COMMENT '자동차정비/유지',
  `AUTOSL_AM`        DECIMAL(18,0) COMMENT '자동차판매',
  `KITWR_AM`         DECIMAL(18,0) COMMENT '주방용품',
  `FABRIC_AM`        DECIMAL(18,0) COMMENT '직물',
  `ACDM_AM`          DECIMAL(18,0) COMMENT '학원',
  `MBRSHOP_AM`       DECIMAL(18,0) COMMENT '회원제형태업소'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
