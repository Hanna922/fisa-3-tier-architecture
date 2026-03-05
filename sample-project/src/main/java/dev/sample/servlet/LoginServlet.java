package dev.sample.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.springframework.context.ApplicationContext;

import dev.sample.ApplicationContextListener;
import dev.sample.dao.UserDao;
import dev.sample.dao.UserDao.User;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private UserDao userDao;

    @Override
    public void init() throws ServletException {
        ApplicationContext ctx = ApplicationContextListener.getBeanContainer(getServletContext());
        userDao = ctx.getBean(UserDao.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.getRequestDispatcher("/WEB-INF/views/auth/login.html")
           .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        String userId = req.getParameter("user_id");
        String password = req.getParameter("password");

        User user = userDao.findByUsername(userId);

        if (user == null || !user.password.equals(password)) {
            resp.sendRedirect(req.getContextPath() + "/login?error=1");
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("loginUser", user.userId);

        resp.sendRedirect(req.getContextPath() + "/life-stages");
    }
}
