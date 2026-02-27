package dev.sample.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dev.sample.ApplicationContextListener;
import dev.sample.dao.LifeStageDao;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@WebServlet("/life-stages")
public class LifeStagesServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LifeStagesServlet.class);
    private static final String CACHE_KEY = "life-stages:all";
    private static final int TTL_SECONDS = 3600;

    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        List<Map<String, Object>> lifeStages = getFromCacheOrDb();
        req.setAttribute("lifeStages", lifeStages);
      
        resp.setContentType("text/html");

        RequestDispatcher rd = req.getRequestDispatcher("/WEB-INF/views/life-stages/list.html");
        rd.forward(req, resp);
    }

    private List<Map<String, Object>> getFromCacheOrDb() {
        JedisPool jedisPool = ApplicationContextListener.getJedisPool(getServletContext());

        try (Jedis jedis = jedisPool.getResource()) {
            String cached = jedis.get(CACHE_KEY);

            if (cached != null) {
                log.debug("Cache hit: {}", CACHE_KEY);
                return gson.fromJson(cached, listType);
            }

            log.debug("Cache miss: {} → querying DB", CACHE_KEY);
            DataSource replicaDs = ApplicationContextListener.getReplicaDataSource(getServletContext());
            List<Map<String, Object>> result = new LifeStageDao(replicaDs).findAll();

            jedis.setex(CACHE_KEY, TTL_SECONDS, gson.toJson(result));
            return result;
        }
    }
}
