

package com.erwin.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    private final String url;
    private final String driverClassName;

    // ✅ FIX 2: cache por user:pass (evita pool pegado con password viejo)
    private final ConcurrentHashMap<String, DataSource> cache = new ConcurrentHashMap<>();

    public DynamicRoutingDataSource(String url, String driverClassName) {
        this.url = url;
        this.driverClassName = driverClassName;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // Solo para el mecanismo interno; nosotros resolvemos el DS abajo.
        return DbContextHolder.getUser();
    }

    @Override
    protected DataSource determineTargetDataSource() {
        String user = DbContextHolder.getUser();
        String pass = DbContextHolder.getPass();

        // Si NO hay sesión (no logueado), usa el default
        if (user == null || user.isBlank() || pass == null) {
            return (DataSource) getResolvedDefaultDataSource();
        }

        // ✅ clave por user:pass
        String key = user + ":" + pass;

        return cache.computeIfAbsent(key, k -> crearDataSourcePara(user, pass));
    }

    private DataSource crearDataSourcePara(String user, String pass) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setDriverClassName(driverClassName);

        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(1);
        cfg.setPoolName("POOL_" + user);

        return new HikariDataSource(cfg);
    }
}