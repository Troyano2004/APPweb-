

package com.erwin.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String defaultUser;

    @Value("${spring.datasource.password}")
    private String defaultPass;

    @Value("${spring.datasource.driver-class-name}")
    private String driver;

    @Value("${spring.datasource.hikari.maximum-pool-size:15}")
    private int maxPool;

    @Bean
    public DataSource defaultDataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(defaultUser);
        cfg.setPassword(defaultPass);
        cfg.setDriverClassName(driver);
        cfg.setMaximumPoolSize(maxPool);
        cfg.setPoolName("POOL_DEFAULT");
        return new HikariDataSource(cfg);
    }

    @Primary
    @Bean
    public DataSource dataSource(DataSource defaultDataSource) {
        DynamicRoutingDataSource routing = new DynamicRoutingDataSource(url, driver);

        // ✅ FIX 1: requerido por AbstractRoutingDataSource
        Map<Object, Object> targets = new HashMap<>();
        targets.put("DEFAULT", defaultDataSource);
        routing.setTargetDataSources(targets);

        // ✅ default por si no hay sesión
        routing.setDefaultTargetDataSource(defaultDataSource);

        routing.afterPropertiesSet();
        return routing;
    }
}