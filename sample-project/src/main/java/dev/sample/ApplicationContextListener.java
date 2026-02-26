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

@WebListener
public class ApplicationContextListener implements ServletContextListener {

    private HikariDataSource sourceDs;
    private HikariDataSource replicaDs;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        Properties props = loadJdbcProperties();

        // Source DataSource: 쓰기/읽기 모두 처리
        HikariConfig sourceConfig = new HikariConfig();
        sourceConfig.setJdbcUrl(props.getProperty("source.url"));
        sourceConfig.setUsername(props.getProperty("source.username"));
        sourceConfig.setPassword(props.getProperty("source.password"));
        sourceDs = new HikariDataSource(sourceConfig);
        ctx.setAttribute("SOURCE_DS", sourceDs);

        // Replica DataSource: 읽기 전용
        HikariConfig replicaConfig = new HikariConfig();
        replicaConfig.setJdbcUrl(props.getProperty("replica.url"));
        replicaConfig.setUsername(props.getProperty("replica.username"));
        replicaConfig.setPassword(props.getProperty("replica.password"));
        replicaDs = new HikariDataSource(replicaConfig);
        ctx.setAttribute("REPLICA_DS", replicaDs);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (sourceDs != null) sourceDs.close();
        if (replicaDs != null) replicaDs.close();
    }

    public static DataSource getSourceDataSource(ServletContext ctx) {
        return (DataSource) ctx.getAttribute("SOURCE_DS");
    }

    public static DataSource getReplicaDataSource(ServletContext ctx) {
        return (DataSource) ctx.getAttribute("REPLICA_DS");
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
