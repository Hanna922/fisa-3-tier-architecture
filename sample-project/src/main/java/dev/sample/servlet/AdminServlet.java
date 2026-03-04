package dev.sample.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import dev.sample.ApplicationContextListener;
import dev.sample.dao.LifeStageDao;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@WebServlet("/admin/card-transaction")
public class AdminServlet extends HttpServlet {

	private static final Logger log = LoggerFactory.getLogger(AdminServlet.class);
	private static final String CACHE_KEY = "life-stages:all";
	private LifeStageDao lifeStageDaoSource;
	private JedisPool jedisPool;

	@Override
	public void init() throws ServletException {
		ApplicationContext ctx = ApplicationContextListener.getBeanContainer(getServletContext());
		lifeStageDaoSource = ctx.getBean("lifeStageDaoSource", LifeStageDao.class);
		jedisPool = ctx.getBean(JedisPool.class);
		
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String view = req.getParameter("view");

		if ("delete".equals(view)) {

			req.getRequestDispatcher("/WEB-INF/views/admin/delete.html").forward(req, resp);
		} else {

			req.getRequestDispatcher("/WEB-INF/views/admin/insert.html").forward(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		req.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json;charset=UTF-8");

		// action 파라미터로 insert - delete 분기
		String action = req.getParameter("action");

		try {
			int rows;
			if ("insert".equals(action)) {
				rows = handleInsert(req);
			} else if ("delete".equals(action)) {
				rows = handleDelete(req);
			} else {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.getWriter().write("{\"result\":\"error\",\"message\":\"unknown action\"}");
				return;
			}

			invalidateCache();
			log.info("Admin {} 완료 - rows={}", action, rows);
			resp.getWriter().write("{\"result\":\"ok\",\"rows\":" + rows + "}");

		} catch (Exception e) {
			log.error("Admin {} 실패", action, e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write("{\"result\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
		}
	}

	private int handleInsert(HttpServletRequest req) {
		return lifeStageDaoSource.insert(req.getParameter("basYh"), req.getParameter("seq"),
				req.getParameter("lifeStage"), parseLong(req.getParameter("totUseAm")));
	}

	
	private int handleDelete(HttpServletRequest req) {
		return lifeStageDaoSource.delete(req.getParameter("basYh"), req.getParameter("seq"));
	}

	private void invalidateCache() {
		try (Jedis jedis = jedisPool.getResource()) {
			jedis.del(CACHE_KEY);
			
			log.debug("캐시 무효화: {}", CACHE_KEY);
		} catch (Exception e) {
			log.warn("캐시 무효화 실패 (Redis 장애) - DB 데이터는 정상 처리됨: {}", e.getMessage());
		}
	}

	private long parseLong(String val) {
		try {
			return (val != null && !val.isBlank()) ? Long.parseLong(val) : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
