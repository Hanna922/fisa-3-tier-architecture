package dev.sample;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@WebListener
public class ApplicationContextListener implements ServletContextListener {

    private HikariDataSource sourceDs;
    private HikariDataSource replicaDs;
    private JedisPool jedisPool;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        Properties props = loadJdbcProperties();

        // Source DataSource: 쓰기/읽기 모두 처리
        HikariConfig sourceConfig = new HikariConfig();
        sourceConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        sourceConfig.setJdbcUrl(props.getProperty("source.url"));
        sourceConfig.setUsername(props.getProperty("source.username"));
        sourceConfig.setPassword(props.getProperty("source.password"));
        sourceDs = new HikariDataSource(sourceConfig);
        ctx.setAttribute("SOURCE_DS", sourceDs);

        // Replica DataSource: 읽기 전용
        HikariConfig replicaConfig = new HikariConfig();
        replicaConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        replicaConfig.setJdbcUrl(props.getProperty("replica.url"));
        replicaConfig.setUsername(props.getProperty("replica.username"));
        replicaConfig.setPassword(props.getProperty("replica.password"));
        replicaDs = new HikariDataSource(replicaConfig);
        ctx.setAttribute("REPLICA_DS", replicaDs);

        // Redis 커넥션 풀
        jedisPool = new JedisPool(
            new JedisPoolConfig(),
            props.getProperty("redis.host"),
            Integer.parseInt(props.getProperty("redis.port"))
        );
        ctx.setAttribute("JEDIS_POOL", jedisPool);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (sourceDs  != null) sourceDs.close();
        if (replicaDs != null) replicaDs.close();
        if (jedisPool != null) jedisPool.close();
    }

    public static DataSource getSourceDataSource(ServletContext ctx) {
        return (DataSource) ctx.getAttribute("SOURCE_DS");
    }

    public static DataSource getReplicaDataSource(ServletContext ctx) {
        return (DataSource) ctx.getAttribute("REPLICA_DS");
    }

    public static JedisPool getJedisPool(ServletContext ctx) {
        return (JedisPool) ctx.getAttribute("JEDIS_POOL");
    }

    private Properties loadJdbcProperties() {
        Properties props = new Properties();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try (InputStream is = cl.getResourceAsStream("jdbc.properties")) {
            if (is == null) {
                throw new RuntimeException("jdbc.properties를 클래스패스에서 찾을 수 없습니다.");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("jdbc.properties 로딩 실패", e);
        }

        return props;
    }
}
