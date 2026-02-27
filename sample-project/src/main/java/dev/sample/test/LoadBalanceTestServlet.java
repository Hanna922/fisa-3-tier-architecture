package dev.sample.test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/test/load-balance")
public class LoadBalanceTestServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {

    	resp.setContentType("text/html;charset=UTF-8"); // HTML 형식으로 변경

        // 현재 서버의 정보를 가져옴
        String serverIp = req.getLocalAddr();
        int serverPort = req.getLocalPort();
        
        PrintWriter out = resp.getWriter();
        out.println("<html><body>");
        out.println("<h1>Nginx Load Balancing Test</h1>");
        out.println("<p>응답한 서버 IP: <b>" + serverIp + "</b></p>");
        out.println("<p>응답한 서버 포트: <b style='color:blue; font-size:20px;'>" + serverPort + "</b></p>");
        out.println("<hr>");
        out.println("<p>현재 시간: " + new java.util.Date() + "</p>");
        out.println("</body></html>");
    }
}
