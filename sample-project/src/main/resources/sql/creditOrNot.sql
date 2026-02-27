/* =====================================================
   라이프스테이지별 신용카드 VS 체크카드 이용 금액 비중
   ===================================================== */
SELECT
    LIFE_STAGE,
    SUM(CRDSL_USE_AM) AS CRDSL_USE_AM,
    SUM(CNF_USE_AM)   AS CNF_USE_AM
FROM card_transaction
GROUP BY LIFE_STAGE
ORDER BY LIFE_STAGE;