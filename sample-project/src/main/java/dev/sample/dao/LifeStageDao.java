package dev.sample.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

	public List<Map<String, Object>> findMembershipTierByLifeStage(String lifeStage) {
	    List<Map<String, Object>> result = new ArrayList<>();
	    String sql = loadSQL("membershipTier_by_lifestage.sql"); 

	    try (Connection conn = dataSource.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        
	        // 2. 파라미터 바인딩
	        pstmt.setString(1, lifeStage);
	        
	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                Map<String, Object> row = new LinkedHashMap<>();
	                // 쿼리 결과 컬럼명과 매칭
	                row.put("LIFE_STAGE", rs.getString("LIFE_STAGE"));
	                row.put("MBR_RK", rs.getInt("MBR_RK"));
	                row.put("CNT", rs.getInt("CNT"));
	                row.put("RATIO_PCT", rs.getDouble("RATIO_PCT"));
	                result.add(row);
	            }
	        }
	    } catch (Exception e) {
	        throw new RuntimeException("멤버 등급 분포 조회 중 오류 발생", e);
	    }
	    return result;
	}

	public List<Map<String, Object>> findConsumptionTypeByLifeStage(String lifeStage) {
		List<Map<String, Object>> result = new ArrayList<>();
		String sql = loadSQL("necessaryOrNot_by_lifestage.sql");

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, lifeStage); // 쿼리의 ? 파라미터 바인딩

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("LIFE_STAGE", lifeStage); // UNI
					row.put("ESSENTIAL_AMT", rs.getLong("ESSENTIAL_AMT")); // 필수 소비 금액
					row.put("OPTIONAL_AMT", rs.getLong("OPTIONAL_AMT")); // 선택 소비 금액
					result.add(row);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("쿼리 실행 중 오류 발생", e);
		}
		return result;
	}

	public List<Map<String, Object>> findTop5ByLifeStage(String lifeStage) {
		List<Map<String, Object>> result = new ArrayList<>();
		String sql = loadSQL("top5_by_lifestage.sql");

		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, lifeStage); // 쿼리의 ? 파라미터 바인딩

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("RNK", rs.getInt("RNK"));
					row.put("CATEGORY", rs.getString("CATEGORY"));
					row.put("TOTAL_AMT", rs.getLong("TOTAL_AMT"));
					result.add(row);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("쿼리 실행 중 오류 발생", e);
		}
		return result;
	}

	public List<Map<String, Object>> findCreditOfCheckByLifeStage(String lifeStage) {
		List<Map<String, Object>> result = new ArrayList<>();
		String sql = loadSQL("creditOrNot_by_lifestage.sql");
		
		try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			
			pstmt.setString(1, lifeStage);
			
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("LIFE_STAGE", lifeStage);
					row.put("CRDSL_USE_AM", rs.getLong("CRDSL_USE_AM"));
					row.put("CNF_USE_AM", rs.getLong("CNF_USE_AM"));
					result.add(row);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("쿼리 실행 중 오류 발생", e);
		}
		return result;
	}

	private String loadSQL(String filename) {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("sql/" + filename);
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			if (is == null) {
				throw new IllegalArgumentException(filename + " SQL 파일을 찾을 수 없습니다.");
			}
			return reader.lines().collect(Collectors.joining("\n"));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}