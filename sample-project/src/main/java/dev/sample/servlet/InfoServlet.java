package dev.sample.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        String lifeStage = req.getParameter("type");
        
        // 2. Replica DB 연결 (ApplicationContextListener 활용)
        DataSource replicaDs = ApplicationContextListener.getReplicaDataSource(getServletContext());
        LifeStageDao dao = new LifeStageDao(replicaDs);
        List<Map<String, Object>> results = dao.findAll(lifeStage);
        
        // 3. 데이터 조회 및 결과 전송
        // List<Map<String, Object>> result = dao.findByStage(type);
        // req.setAttribute("data", result);
            
        // 로드밸런싱 확인용 포트 정보 추가
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
            
        out.println("선택된 타입: " + lifeStage);
            
        out.println("<table border='1' style='border-collapse: collapse; width: 50%;'>");
        out.println("<tr style='background-color: #f2f2f2;'>");
        out.println("<th>고객번호</th><th>연령대</th><th>라이프스테이지</th><th>총이용금액</th>");
        out.println("</tr>");

        for (Map<String, Object> row : results) {
            out.println("<tr>");
            out.println("<td>" + row.get("SEQ") + "</td>");
            out.println("<td>" + row.get("AGE") + "</td>");
            out.println("<td>" + row.get("LIFE_STAGE") + "</td>");
            
            // 금액 데이터는 포맷팅해서 출력하면 더 좋습니다.
            out.println("<td>" + row.get("TOT_USE_AM") + " 만원</td>");
            out.println("</tr>");
        }

        out.println("</table>");
    }
}
