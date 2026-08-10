package com.college.erp.tenant;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Builds the app's runtime {@code DataSource} bean (used by JPA) from {@code spring.datasource.*}
 * and wraps it with {@link TenantAwareDataSource} — see that class's javadoc for why, and
 * {@code V017__multi_tenancy_foundation.sql}'s header for why this connects as the least-privilege
 * {@code erp_app} role rather than the superuser Flyway itself uses (configured separately via
 * {@code spring.flyway.*}, untouched by this class).
 *
 * <p>Standard Spring Boot recipe for "customize the auto-configured DataSource": defining a bean
 * named {@code dataSource} here makes {@code DataSourceAutoConfiguration} back off (it is
 * {@code @ConditionalOnMissingBean(DataSource.class)}), so this is the only {@code DataSource}
 * bean JPA ever sees — no double-configuration.
 */
@Configuration
public class TenantDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties properties) {
        HikariDataSource hikariDataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        return new TenantAwareDataSource(hikariDataSource);
    }
}
