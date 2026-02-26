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

@WebServlet("/test/log")
public class LogTestServlet extends HttpServlet {

    private static final Logger log =
            LoggerFactory.getLogger(LogTestServlet.class);

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        PrintWriter out = resp.getWriter();

        log.trace("TRACE log test");
        log.debug("DEBUG log test");
        log.info("INFO log test");
        log.warn("WARN log test");
        log.error("ERROR log test");

        out.println("Logback Test OK");
    }
}
