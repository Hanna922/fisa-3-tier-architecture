package dev.sample.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;

import dev.sample.ApplicationContextListener;

@WebServlet("/test/hikari")
public class HikariHealthCheckServlet extends HttpServlet {

    private DataSource ds;

    @Override
    public void init() throws ServletException {
        ApplicationContext ctx = ApplicationContextListener.getBeanContainer(getServletContext());
        ds = ctx.getBean("sourceDataSource", DataSource.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String sql = "SELECT 1";

        long start = System.currentTimeMillis();
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            int v = rs.getInt(1);

            long elapsed = System.currentTimeMillis() - start;
            out.println("Status: OK");
            out.println("queryResult=" + v);
            out.println("elapsedMs=" + elapsed);
            out.println("connClass=" + con.getClass().getName());
        } catch (Exception e) {
            resp.setStatus(500);
            out.println("FAIL: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }
}
