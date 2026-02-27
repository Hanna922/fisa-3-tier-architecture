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

	public List<Map<String, Object>> findMembershipTierByLifeStageMock(String lifeStage) {
		List<Map<String, Object>> mockResults = new ArrayList<>();

		// 데이터 1: MBR_RK 25
		Map<String, Object> row1 = new LinkedHashMap<>();
		row1.put("LIFE_STAGE", lifeStage);
		row1.put("MBR_RK", 000);
		row1.put("CNT", 000);
		row1.put("RATIO_PCT", 000.0);
		mockResults.add(row1);

		// 데이터 2: MBR_RK 24
		Map<String, Object> row2 = new LinkedHashMap<>();
		row2.put("LIFE_STAGE", lifeStage);
		row2.put("MBR_RK", 000);
		row2.put("CNT", 000);
		row2.put("RATIO_PCT", 000.0);
		mockResults.add(row2);

		// 데이터 3: MBR_RK 23
		Map<String, Object> row3 = new LinkedHashMap<>();
		row3.put("LIFE_STAGE", lifeStage);
		row3.put("MBR_RK", 000);
		row3.put("CNT", 000);
		row3.put("RATIO_PCT", 000.0);
		mockResults.add(row3);

		// 데이터 4: MBR_RK 22
		Map<String, Object> row4 = new LinkedHashMap<>();
		row4.put("LIFE_STAGE", lifeStage);
		row4.put("MBR_RK", 000);
		row4.put("CNT", 000);
		row4.put("RATIO_PCT", 000.0);
		mockResults.add(row4);

		// 데이터 5: MBR_RK 21
		Map<String, Object> row5 = new LinkedHashMap<>();
		row5.put("LIFE_STAGE", lifeStage);
		row5.put("MBR_RK", 000);
		row5.put("CNT", 000);
		row5.put("RATIO_PCT", 000.0);
		mockResults.add(row5);

		return mockResults;
	}

	public List<Map<String, Object>> findConsumptionTypeByLifeStageMock(String lifeStage) {
		List<Map<String, Object>> mockResults = new ArrayList<>();

		// 제공해주신 쿼리 결과를 한 줄(Row)로 생성
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("LIFE_STAGE", lifeStage); // UNI
		row.put("ESSENTIAL_AMT", 000L); // 필수 소비 금액
		row.put("OPTIONAL_AMT", 000L); // 선택 소비 금액

		mockResults.add(row);
		return mockResults;
	}

	public List<Map<String, Object>> findTop5ByLifeStageMock(String lifeStage) {
		List<Map<String, Object>> mockResults = new ArrayList<>();

		// 순위 데이터 정의 (표 데이터 그대로 입력)
		Object[][] data = { { "AAA", 000L, 1 }, { "AAA", 000L, 2 }, { "AAA", 000L, 3 }, { "AAA", 000L, 4 },
				{ "AAA", 000L, 5 } };

		for (Object[] obj : data) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("LIFE_STAGE", lifeStage);
			row.put("CATEGORY", obj[0]);
			row.put("TOTAL_AMT", obj[1]);
			row.put("RNK", obj[2]);
			mockResults.add(row);
		}

		return mockResults;
	}

	public List<Map<String, Object>> findCreditOfCheckByLifeStageMock(String lifeStage) {
		List<Map<String, Object>> mockResults = new ArrayList<>();

		// 쿼리 결과 한 줄(Row)을 Map 형태로 생성
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("LIFE_STAGE", lifeStage); // UNI
		row.put("CRDSL_USE_AM", 000L); // 신용카드 이용 금액
		row.put("CNF_USE_AM", 000L); // 체크카드 이용 금액

		mockResults.add(row);
		return mockResults;
	}
}