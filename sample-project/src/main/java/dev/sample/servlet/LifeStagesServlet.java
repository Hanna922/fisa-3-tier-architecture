package dev.sample.servlet;

import java.io.IOException;
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

import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import dev.sample.ApplicationContextListener;
import dev.sample.dao.LifeStageDao;

@WebServlet("/life-stages")
public class LifeStagesServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LifeStagesServlet.class);
    private static final String CACHE_KEY       = "life-stages:all";
    private static final String LOCK_KEY        = "lock:life-stages:all";
    private static final int    TTL_SECONDS     = 3600;
    private static final int    LOCK_TTL        = 10;   // 락 최대 유지 시간 (초)
    private static final long   LOCK_WAIT_MS    = 100;  // 락 대기 간격 (ms)
    private static final int    LOCK_RETRY      = 50;   // 최대 재시도 횟수 (총 5초)

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
        DataSource replicaDs = ApplicationContextListener.getReplicaDataSource(getServletContext());
        JedisPool jedisPool = ApplicationContextListener.getJedisPool(getServletContext());

        // 1. Try Redis GET
        try (Jedis jedis = jedisPool.getResource()) {
            String cached = jedis.get(CACHE_KEY);

            if (cached != null) {
                log.debug("Cache hit: {}", CACHE_KEY);
                return gson.fromJson(cached, listType);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, fallback to DB: {}", e.getMessage());
            return new LifeStageDao(replicaDs).findAll();
        }

        // 2단계: Cache Miss → 분산 락 획득 시도
        // 락 값으로 UUID를 사용해 자신이 건 락만 해제할 수 있도록 보장
        String lockValue = UUID.randomUUID().toString();

        try (Jedis jedis = jedisPool.getResource()) {
            // NX(키가 없을 때만 저장) + EX(TTL 설정) 원자적 수행
            String acquired = jedis.set(LOCK_KEY, lockValue, SetParams.setParams().nx().ex(LOCK_TTL));

            if ("OK".equals(acquired)) {
                // 락 획득 성공 → 이 요청만 DB 조회
                log.debug("Lock acquired, querying DB: {}", CACHE_KEY);
                try {
                    List<Map<String, Object>> result = new LifeStageDao(replicaDs).findAll();
                    jedis.setex(CACHE_KEY, TTL_SECONDS, gson.toJson(result));
                    return result;
                } finally {
                    // 자신이 건 락인지 확인 후 해제 (다른 요청의 락을 실수로 삭제 방지)
                    if (lockValue.equals(jedis.get(LOCK_KEY))) {
                        jedis.del(LOCK_KEY);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Redis lock failed, fallback to DB: {}", e.getMessage());
            return new LifeStageDao(replicaDs).findAll();
        }

        // 3단계: 락 획득 실패 → 다른 요청이 DB 조회 중 → 캐시 적재 대기 후 재조회
        log.debug("Lock not acquired, waiting for cache: {}", CACHE_KEY);
        for (int i = 0; i < LOCK_RETRY; i++) {
            try {
                Thread.sleep(LOCK_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            try (Jedis jedis = jedisPool.getResource()) {
                String cached = jedis.get(CACHE_KEY);
                if (cached != null) {
                    log.debug("Cache populated after waiting ({} ms): {}", (i + 1) * LOCK_WAIT_MS, CACHE_KEY);
                    return gson.fromJson(cached, listType);
                }
            } catch (Exception e) {
                log.warn("Redis retry failed, fallback to DB: {}", e.getMessage());
                break;
            }
        }

        // 4단계: 대기 후에도 캐시 없음 → DB 직접 조회 (최후 수단)
        log.warn("Cache not populated after waiting, querying DB directly: {}", CACHE_KEY);
        return new LifeStageDao(replicaDs).findAll();
    }
}
