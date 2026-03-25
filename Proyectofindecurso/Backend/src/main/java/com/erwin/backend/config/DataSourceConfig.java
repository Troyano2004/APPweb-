package com.erwin.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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

        // ── Recovery mode: arrancar aunque la BD no exista ────────────────────
        // -1 = Hikari no valida la conexión al iniciar el pool.
        cfg.setInitializationFailTimeout(-1);
        cfg.setConnectionTimeout(5_000);
        cfg.setIdleTimeout(300_000);
        cfg.setMaxLifetime(1_800_000);
        cfg.setKeepaliveTime(30_000);

        return new HikariDataSource(cfg);
    }

    @Primary
    @Bean
    public DataSource dataSource(DataSource defaultDataSource) {
        DynamicRoutingDataSource routing = new DynamicRoutingDataSource(url, driver);

        Map<Object, Object> targets = new HashMap<>();
        targets.put("DEFAULT", defaultDataSource);
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(defaultDataSource);
        routing.afterPropertiesSet();
        return routing;
    }

    /**
     * Verifica si la BD principal existe conectando a 'postgres'.
     * CORRECCIÓN: usa PreparedStatement para evitar SQL injection.
     */
    public static boolean baseDeDatosExiste(String jdbcUrl, String usuario, String password) {
        try {
            String dbName      = jdbcUrl.substring(jdbcUrl.lastIndexOf('/') + 1);
            String postgresUrl = jdbcUrl.substring(0, jdbcUrl.lastIndexOf('/')) + "/postgres";
            try (Connection conn = DriverManager.getConnection(postgresUrl, usuario, password);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT 1 FROM pg_database WHERE datname = ?")) {
                ps.setString(1, dbName);
                return ps.executeQuery().next();
            }
        } catch (Exception e) {
            return false;
        }
    }
}