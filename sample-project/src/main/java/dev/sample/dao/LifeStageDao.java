package dev.sample.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

public class LifeStageDao {

    private final DataSource dataSource;

    public LifeStageDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * life_stages 테이블 전체 조회 (Replica DB 사용)
     *
     * @return 각 행을 컬럼명→값 Map으로 담은 List
     */
    public List<Map<String, Object>> findAll() {
        List<Map<String, Object>> result = new ArrayList<>();

        // TODO: 필요 시 집계 컬럼, 조건, 정렬 방식을 변경
        String sql = "SELECT LIFE_STAGE" +
                     "     , COUNT(*)           AS CNT" +
                     "     , SUM(TOT_USE_AM)    AS TOT_USE_AM" +
                     "     , SUM(CRDSL_USE_AM)  AS CRDSL_USE_AM" +
                     "     , SUM(CNF_USE_AM)    AS CNF_USE_AM" +
                     "  FROM CARD_TRANSACTION" +
                     " GROUP BY LIFE_STAGE" +
                     " ORDER BY LIFE_STAGE";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    // TODO: 필요 시 컬럼명/값 매핑 방식을 변경
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                result.add(row);
            }

        } catch (Exception e) {
            throw new RuntimeException("LifeStageDao.findAll() 실행 중 오류 발생", e);
        }

        return result;
    }
}
