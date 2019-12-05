package no.nav.fo.veilarbvedtaksstotte.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil;
import org.flywaydb.core.Flyway;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.sql.DataSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.sbl.util.EnvironmentUtils.*;

public class DbUtils {

    public static DataSource createDataSource(String dbUrl, DbRole dbRole) {
        HikariConfig config = createDataSourceConfig(dbUrl);
        return createVaultRefreshDataSource(config, dbRole);
    }

    @SneakyThrows
    public static void migrateAndClose(DataSource dataSource, DbRole dbRole) {
        migrate(dataSource, dbRole);
        dataSource.getConnection().close();
    }

    public static void migrate(DataSource dataSource, DbRole dbRole) {
        Flyway.configure()
                .dataSource(dataSource)
                .table("vedtaksstotte_flyway_schema_history")
                .initSql(String.format("SET ROLE \"%s\"", toDbRoleStr(dbRole)))
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    public static HikariConfig createDataSourceConfig(String dbUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        return config;
    }

    public static <T> ResultSetExtractor<T> singleResult(ResultSetExtractor<T> rse) {
        return rs -> {
            if (rs.next()) {
                return rse.extractData(rs);
            }
            return null;
        };
    }

    @SneakyThrows
    public static PreparedStatement createPreparedStatement(JdbcTemplate db, String sql) {
        return db.getDataSource().getConnection().prepareStatement(sql);
    }

    @SneakyThrows
    private static DataSource createVaultRefreshDataSource(HikariConfig config, DbRole dbRole) {
        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, getMountPath(), toDbRoleStr(dbRole));
    }

    private static String getMountPath() {
        boolean isProd = getEnvironmentClass() == EnviromentClass.P;
        return "postgresql/" + (isProd ? "prod-fss" : "preprod-fss");
    }

    public static String toDbRoleStr(DbRole dbRole) {
        String namespace = requireNamespace();
        String environment = "default".equals(namespace) ? "p" : namespace;
        String role = EnumUtils.getName(dbRole).toLowerCase();

        return String.join("-", APPLICATION_NAME, environment, role);
    }

}
