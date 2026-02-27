/* =====================================================
   라이프스테이지별 회원등급 비율
   ===================================================== */
SELECT
  LIFE_STAGE,
  MBR_RK,
  COUNT(*) AS CNT,
  ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (PARTITION BY LIFE_STAGE), 2) AS RATIO_PCT
FROM card_transaction
GROUP BY LIFE_STAGE, MBR_RK
ORDER BY LIFE_STAGE, RATIO_PCT DESC;