/* =====================================================
   특정 라이프스테이지가 유독 많이 쓰는 업종
   - 전체 평균 대비 소비 비율
   ===================================================== */

WITH single_scan AS (
  SELECT
    AVG(CASE WHEN LIFE_STAGE = 'NEW_JOB' THEN INTERIOR_AM END)  AS nj_interior,
    AVG(CASE WHEN LIFE_STAGE = 'NEW_JOB' THEN INSUHOS_AM  END)  AS nj_insuhos,
    AVG(CASE WHEN LIFE_STAGE = 'NEW_JOB' THEN TRVL_AM     END)  AS nj_trvl,
    AVG(CASE WHEN LIFE_STAGE = 'NEW_JOB' THEN CULTURE_AM  END)  AS nj_culture,
    AVG(CASE WHEN LIFE_STAGE = 'NEW_JOB' THEN GROCERY_AM  END)  AS nj_grocery,
    AVG(CASE WHEN LIFE_STAGE = 'NEW_JOB' THEN HOS_AM      END)  AS nj_hospital,
    AVG(CASE WHEN LIFE_STAGE = 'NEW_JOB' THEN RESTRNT_AM  END)  AS nj_restaurant,
    AVG(CASE WHEN LIFE_STAGE = 'NEW_JOB' THEN FUEL_AM     END)  AS nj_fuel,
    AVG(INTERIOR_AM)  AS total_interior,
    AVG(INSUHOS_AM)   AS total_insuhos,
    AVG(TRVL_AM)      AS total_trvl,
    AVG(CULTURE_AM)   AS total_culture,
    AVG(GROCERY_AM)   AS total_grocery,
    AVG(HOS_AM)       AS total_hospital,
    AVG(RESTRNT_AM)   AS total_restaurant,
    AVG(FUEL_AM)      AS total_fuel
  WHERE LIFE_STAGE = ?
  FROM card_transaction
)
SELECT
  CATEGORY,
  TARGET_AVG,
  TOTAL_AVG,
  ROUND(TARGET_AVG / NULLIF(TOTAL_AVG, 0), 2) AS RELATIVE_RATIO
  FROM (
    SELECT 'INTERIOR'   AS CATEGORY, nj_interior,   total_interior   FROM single_scan
  UNION ALL SELECT 'INSUHOS',    nj_insuhos,    total_insuhos    FROM single_scan
  UNION ALL SELECT 'TRVL',       nj_trvl,       total_trvl       FROM single_scan
  UNION ALL SELECT 'CULTURE',    nj_culture,    total_culture    FROM single_scan
  UNION ALL SELECT 'GROCERY',    nj_grocery,    total_grocery    FROM single_scan
  UNION ALL SELECT 'HOSPITAL',   nj_hospital,   total_hospital   FROM single_scan
  UNION ALL SELECT 'RESTAURANT', nj_restaurant, total_restaurant FROM single_scan
  UNION ALL SELECT 'FUEL',       nj_fuel,       total_fuel       FROM single_scan
) base (CATEGORY, TARGET_AVG, TOTAL_AVG)
WHERE ROUND(TARGET_AVG / NULLIF(TOTAL_AVG, 0), 2) >= 1.3
ORDER BY RELATIVE_RATIO;