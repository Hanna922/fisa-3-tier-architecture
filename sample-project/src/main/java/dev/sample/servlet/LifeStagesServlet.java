package dev.sample.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import dev.sample.ApplicationContextListener;
import dev.sample.dao.LifeStageDao;

@WebServlet("/life-stages")
public class LifeStagesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        DataSource replicaDs = ApplicationContextListener.getReplicaDataSource(getServletContext());
        LifeStageDao dao = new LifeStageDao(replicaDs);

        List<Map<String, Object>> lifeStages = dao.findAll();
        req.setAttribute("lifeStages", lifeStages);

        RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/life-stages/list.html");
        rd.forward(req, resp);
    }
}
