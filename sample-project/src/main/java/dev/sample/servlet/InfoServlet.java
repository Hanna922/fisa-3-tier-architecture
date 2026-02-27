package dev.sample.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import dev.sample.ApplicationContextListener;
import dev.sample.dao.LifeStageDao;

@WebServlet(urlPatterns = "/info")
public class InfoServlet extends HttpServlet {

    private static final DecimalFormat df = new DecimalFormat("#,###");
    private static final Executor DB_QUERY_POOL = Executors.newFixedThreadPool(10);
    private LifeStageDao dao;

    @Override
    public void init() throws ServletException {
        // 서블릿 초기화 시 DAO를 한 번만 생성하여 메모리 및 성능 최적화
        DataSource replicaDs = ApplicationContextListener.getReplicaDataSource(getServletContext());
        this.dao = new LifeStageDao(replicaDs);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String lifeStageCode = req.getParameter("type") != null ? req.getParameter("type") : "UNKNOWN";
        String lifeStageKor = convertToKor(lifeStageCode);
        
        // 1. 4개의 쿼리를 비동기로 동시 실행 시작 (병렬 처리)
        CompletableFuture<List<Map<String, Object>>> creditFuture = CompletableFuture.supplyAsync(() -> dao.findCreditOfCheckByLifeStage(lifeStageCode), DB_QUERY_POOL);
        CompletableFuture<List<Map<String, Object>>> tierFuture = CompletableFuture.supplyAsync(() -> dao.findMembershipTierByLifeStage(lifeStageCode), DB_QUERY_POOL);
        CompletableFuture<List<Map<String, Object>>> consumptionFuture = CompletableFuture.supplyAsync(() -> dao.findConsumptionTypeByLifeStage(lifeStageCode), DB_QUERY_POOL);
        CompletableFuture<List<Map<String, Object>>> top5Future = CompletableFuture.supplyAsync(() -> dao.findTop5ByLifeStage(lifeStageCode), DB_QUERY_POOL);

        // 2. 모든 작업이 끝날 때까지 기다림 (Join)
        // 화면 렌더링 순서대로 join()을 호출하여 결과값을 가져옵니다.
        List<Map<String, Object>> creditResult = creditFuture.join();
        List<Map<String, Object>> tierResult = tierFuture.join();
        List<Map<String, Object>> consumptionResult = consumptionFuture.join();
        List<Map<String, Object>> top5Result = top5Future.join();
        
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        
        // HTML 및 디자인(CSS) 주입
        out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>" + lifeStageKor + " 분석 리포트</title>");
        renderStyle(out);
        out.println("</head><body>");
        
        out.println("<div class='container'>");
        out.println("<h1>📊 " + lifeStageKor + " 라이프스테이지 정밀 소비 분석</h1>");
        
        // --- 각 섹션별 데이터 출력 호출 ---
        renderCreditOrCheckSection(out, lifeStageKor, creditResult);
        renderMembershipTierSection(out, lifeStageKor, tierResult);
        renderConsumptionTypeSection(out, lifeStageKor, consumptionResult);
        renderTop5CategorySection(out, lifeStageKor, top5Result);
        
        out.println("<a href='javascript:history.back()' class='btn-back'>← 분석 종료 및 목록으로</a>");
        out.println("</div>");
        out.println("</body></html>");
    }

    private void renderCreditOrCheckSection(PrintWriter out, String lifeStageKor, List<Map<String, Object>> result) {
        out.println("<h3>카드 결제 수단별 현황</h3>");
        out.println("<table>");
        out.println("<tr><th>분석 대상</th><th>신용카드 이용금액</th><th>체크카드 이용금액</th></tr>");

        if (isResultValid(result)) {
            for (Map<String, Object> row : result) {
                long crdslAm = convertToLong(row.get("CRDSL_USE_AM")) * 1000;
                long cnfAm = convertToLong(row.get("CNF_USE_AM")) * 1000;
                out.println("<tr><td>" + lifeStageKor + "</td>");
                out.println("<td>" + df.format(crdslAm) + " 원</td>");
                out.println("<td>" + df.format(cnfAm) + " 원</td></tr>");
            }
        } else {
            out.println("<tr><td colspan='3'>데이터가 존재하지 않습니다.</td></tr>");
        }
        out.println("</table>");
    }

    private void renderMembershipTierSection(PrintWriter out, String lifeStageKor, List<Map<String, Object>> result) {
        out.println("<hr><h3>등급 분포 분석</h3>");
        out.println("<table>");
        out.println("<tr><th>분석 등급</th><th>인원 수</th><th>구성비</th></tr>");

        if (isResultValid(result)) {
            for (Map<String, Object> row : result) {
                String formattedCnt = String.format("%,d", convertToLong(row.get("CNT")));
                String tier = convertToTier(row.get("MBR_RK"));
                out.println("<tr><td>" + tier + "</td><td>" + formattedCnt + " 명</td>");
                out.println("<td class='highlight-blue'>" + row.get("RATIO_PCT") + " %</td></tr>");
            }
        } else {
            out.println("<tr><td colspan='3'>분포 데이터가 없습니다.</td></tr>");
        }
        out.println("</table>");
    }

    private void renderConsumptionTypeSection(PrintWriter out, String lifeStageKor, List<Map<String, Object>> result) {
        out.println("<hr><h3>필수 vs 선택 소비 성향</h3>");
        out.println("<table>");
        out.println("<tr style='background-color: #fffef2;'><th>분석 단계</th><th>필수 소비 (생활/교통)</th><th>선택 소비 (여가/문화)</th></tr>");

        if (isResultValid(result)) {
            for (Map<String, Object> row : result) {
                long essentialAm = convertToLong(row.get("ESSENTIAL_AMT")) * 1000;
                long optionalAm = convertToLong(row.get("OPTIONAL_AMT")) * 1000;
                out.println("<tr><td>" + lifeStageKor + "</td>");
                out.println("<td>" + df.format(essentialAm) + " 원</td>");
                out.println("<td>" + df.format(optionalAm) + " 원</td></tr>");
            }
        } else {
            out.println("<tr><td colspan='3'>분석 데이터가 없습니다.</td></tr>");
        }
        out.println("</table>");
    }

    private void renderTop5CategorySection(PrintWriter out, String lifeStageKor, List<Map<String, Object>> result) {
        out.println("<hr><h3>소비 지출 TOP 5 업종</h3>");
        out.println("<table>");
        out.println("<tr><th>순위</th><th>소비 카테고리</th><th>총 지출 금액</th></tr>");

        if (isResultValid(result)) {
            for (Map<String, Object> row : result) {
                long totalAm = convertToLong(row.get("TOTAL_AMT")) * 1000;
                out.println("<tr><td><span class='rank-badge'>" + row.get("RNK") + " 위</span></td>");
                out.println("<td style='color: #444;'>" + row.get("CATEGORY") + "</td>");
                out.println("<td style='font-weight: 600;'>" + df.format(totalAm) + " 원</td></tr>");
            }
        } else {
            out.println("<tr><td colspan='3'>데이터가 없습니다.</td></tr>");
        }
        out.println("</table>");
    }

    private boolean isResultValid(List<?> result) {
        return result != null && !result.isEmpty();
    }

    private long convertToLong(Object obj) {
        if (obj == null) return 0L;
        try {
            return Long.parseLong(String.valueOf(obj));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String convertToTier(Object obj) {
        if (obj == null) return "정보 없음";
        int code = Integer.parseInt(String.valueOf(obj));
        switch(code) {
            case 21: return "VVIP";
            case 22: return "VIP";
            case 23: return "플래티넘";
            case 24: return "골드";
            case 25: return "일반 멤버";
            default: return "기타 (" + code + ")";
        }
    }
    
    private String convertToKor(String lifeStage) {
        if (lifeStage == null) return "알 수 없음";
        switch (lifeStage.toUpperCase()) {
            case "UNI":          return "대학생";
            case "NEW_JOB":      return "사회초년생";
            case "NEW_WED":      return "신혼부부";
            case "CHILD_BABY":   return "영유아자녀";
            case "CHILD_TEEN":   return "청소년자녀";
            case "CHILD_UNI":    return "대학생자녀";
            case "GOLLIFE":      return "중년";
            case "SECLIFE":      return "액티브시니어";
            case "RETIR":        return "은퇴";
            default:             return "알 수 없음";
        }
    }
    
    // CSS 스타일 출력 메서드
    private void renderStyle(PrintWriter out) {
        out.println("<style>");
        out.println("    body { font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', sans-serif; background-color: #f0f2f5; color: #1c1e21; margin: 0; padding: 40px; }");
        out.println("    .container { max-width: 900px; margin: auto; background: white; padding: 40px; border-radius: 15px; box-shadow: 0 8px 30px rgba(0,0,0,0.05); }");
        out.println("    h1 { color: #1a73e8; border-bottom: 2px solid #e8f0fe; padding-bottom: 15px; margin-bottom: 30px; font-size: 28px; }");
        out.println("    h3 { margin-top: 45px; color: #3c4043; font-size: 20px; display: flex; align-items: center; }");
        out.println("    h3::before { content: ''; display: inline-block; width: 5px; height: 22px; background: #1a73e8; margin-right: 12px; border-radius: 3px; }");
        out.println("    table { width: 100%; border-collapse: separate; border-spacing: 0; margin-top: 15px; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden; table-layout: fixed; }");
        out.println("    th, td { padding: 15px; border-bottom: 1px solid #f1f1f1; text-align: center; font-size: 15px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }");
        out.println("    th:first-child, td:first-child { width: 25%; font-weight: 500; color: #202124; }");
        out.println("    th { background-color: #f8f9fa; color: #5f6368; font-weight: 600; font-size: 14px; border-bottom: 1px solid #e0e0e0; }");
        out.println("    tr:last-child td { border-bottom: none; }");
        out.println("    tr:hover { background-color: #fdfdfd; transition: 0.2s; }");
        out.println("    .highlight-blue { color: #1a73e8; font-weight: bold; }");
        out.println("    .rank-badge { background: #e8f0fe; color: #1967d2; padding: 5px 12px; border-radius: 20px; font-weight: bold; font-size: 13px; }");
        out.println("    .btn-back { display: inline-block; margin-top: 40px; padding: 12px 25px; background: #5f6368; color: white; text-decoration: none; border-radius: 8px; font-weight: 500; }");
        out.println("    .btn-back:hover { background: #3c4043; transition: 0.3s; }");
        out.println("    hr { border: 0; height: 1px; background: #eee; margin: 40px 0; }");
        out.println("</style>");
    }
}