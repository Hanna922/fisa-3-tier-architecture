 
 INSERT INTO CARD_TRANSACTION (
      BAS_YH,
      SEQ,
      AGE,
      SEX_CD,
      MBR_RK,
      ATT_YM,
      HOUS_SIDO_NM,
      DIGT_CHNL_REG_YN,
      DIGT_CHNL_USE_YN,
      LIFE_STAGE,
      TOT_USE_AM,
      CRDSL_USE_AM,
      CNF_USE_AM
  ) VALUES (
      ?,   -- BAS_YH           : 기준시점(분기)   예) 202401
      ?,   -- SEQ              : 고객번호
      ?,   -- AGE              : 연령대           예) 30
      ?,   -- SEX_CD           : 성별             예) M / F
      ?,   -- MBR_RK           : 회원등급         예) 01~05
      ?,   -- ATT_YM           : 입회년월         예) 202001
      ?,   -- HOUS_SIDO_NM     : 거주지역         예) 서울특별시
      ?,   -- DIGT_CHNL_REG_YN : 디지털채널가입   예) Y / N
      ?,   -- DIGT_CHNL_USE_YN : 디지털채널이용   예) Y / N
      ?,   -- LIFE_STAGE       : 라이프스테이지   예) NEW_JOB
      ?,   -- TOT_USE_AM       : 총이용금액
      ?,   -- CRDSL_USE_AM     : 신용카드이용금액
      ?    -- CNF_USE_AM       : 체크카드이용금액
  );
