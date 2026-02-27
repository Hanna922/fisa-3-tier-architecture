/* =====================================================
   라이프스테이지별 필수 vs 선택 소비 구조
   - 필수 : 병원(HOST) 마트(GROCERY) 교통(FUEL)
   - 선택 : 외식(RESTRNT) 여행(TRVL) 문화(CULTURE)
   ===================================================== */
SELECT
  LIFE_STAGE,
  SUM(HOS_AM + GROCERY_AM + FUEL_AM)       AS ESSENTIAL_AMT,
  SUM(RESTRNT_AM + TRVL_AM + CULTURE_AM)   AS OPTIONAL_AMT
FROM CARD_TRANSACTION
WHERE LIFE_STAGE = ?
GROUP BY LIFE_STAGE
ORDER BY LIFE_STAGE;