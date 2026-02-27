package dev.sample.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
		String sql = "SELECT LIFE_STAGE" + "     , COUNT(*)           AS CNT"
				+ "     , SUM(TOT_USE_AM)    AS TOT_USE_AM" + "     , SUM(CRDSL_USE_AM)  AS CRDSL_USE_AM"
				+ "     , SUM(CNF_USE_AM)    AS CNF_USE_AM" + "  FROM CARD_TRANSACTION" + " GROUP BY LIFE_STAGE"
				+ " ORDER BY LIFE_STAGE";

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

	private String loadSql(String fileName) {
	    InputStream is = getClass().getClassLoader().getResourceAsStream("sql/" + fileName);
	    if (is == null) throw new RuntimeException("SQL 파일 없음: sql/" + fileName);
	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
	        return reader.lines().collect(Collectors.joining("\n"));
	    } catch (Exception e) {
	        throw new RuntimeException("SQL 파일 로드 실패: " + fileName, e);
	    }
	}
	
	
	/**
	 * admin 용 insert
	 * @param basYh
	 * @param seq
	 * @param lifeStage
	 * @param totUseAm
	 * @return
	 */
	public int insert(
	        String basYh,
	        String seq,
	        String lifeStage,
	        long totUseAm
	) {

	    String sql = loadSql("insert.sql");

	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        int i = 1;

	        pstmt.setString(i++, basYh);
	        pstmt.setString(i++, seq);
	        pstmt.setString(i++, lifeStage);
	        pstmt.setLong(i++, totUseAm);

	        return pstmt.executeUpdate();

	    } catch (Exception e) {
	        throw new RuntimeException("insert 실패", e);
	    }
	}
	
	/**
	 * Admin 용 delete (기준시점 + 고객번호 기준)
	 */
	public int delete(String basYh, String seq) {

	    String sql = loadSql("deleteBySEQ.sql");

	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setString(1, basYh);
	        pstmt.setString(2, seq);

	        return pstmt.executeUpdate();

	    } catch (Exception e) {
	        throw new RuntimeException("delete 실패", e);
	    }
	}

}