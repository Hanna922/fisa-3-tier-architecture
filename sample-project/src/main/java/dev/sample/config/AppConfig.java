package dev.sample.config;


import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import dev.sample.dao.LifeStageDao;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@ComponentScan("dev.sample")
@PropertySource("classpath:jdbc.properties") // 기존 loadJdbcProperties() 대체
public class AppConfig {

    @Value("${source.url}")
    private String sourceUrl;
    @Value("${source.username}")
    private String sourceUsername;
    @Value("${source.password}")
    private String sourcePassword;

    @Value("${replica.url}")
    private String replicaUrl;
    @Value("${replica.username}")
    private String replicaUsername;
    @Value("${replica.password}")
    private String replicaPassword;

    @Value("${redis.host}")
    private String redisHost;
    @Value("${redis.port}")
    private int redisPort;

    @Bean(name = "sourceDataSource", destroyMethod = "close")
    public HikariDataSource sourceDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(sourceUrl);
        config.setUsername(sourceUsername);
        config.setPassword(sourcePassword);
        return new HikariDataSource(config);
    }

    @Bean(name = "replicaDataSource", destroyMethod = "close")
    public HikariDataSource replicaDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(replicaUrl);
        config.setUsername(replicaUsername);
        config.setPassword(replicaPassword);
        return new HikariDataSource(config);
    }

    @Bean(destroyMethod = "close")
    public JedisPool jedisPool() {
        return new JedisPool(new JedisPoolConfig(), redisHost, redisPort);
    }
    
    @Bean(name = "lifeStageDaoSource")
    public LifeStageDao lifeStageDaoSource(
            @Qualifier("sourceDataSource") DataSource ds) {
        return new LifeStageDao(ds);
    }
    
    @Bean(name = "lifeStageDaoReplica")
    public LifeStageDao lifeStageDaoReplica(
            @Qualifier("replicaDataSource") DataSource ds) {
        return new LifeStageDao(ds);
    }
    
}