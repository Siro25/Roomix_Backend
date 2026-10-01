package com.team4.core.merge;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FeatureDatabaseUpgradeTest {
    @Test
    void existingFeatureSchemaAndDataSurviveAdminMigrations() throws Exception {
        String url = "jdbc:h2:mem:feature_upgrade;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        try (var dataSource = new HikariDataSource()) {
            dataSource.setJdbcUrl(url);
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            Flyway.configure().dataSource(dataSource).target("4").load().migrate();
            try (var connection = dataSource.getConnection(); var sql = connection.createStatement()) {
                sql.executeUpdate("""
                        INSERT INTO users VALUES ('00000000-0000-0000-0000-000000000001','owner','owner@example.com',
                        'hash','Owner','0900000000',NULL,'LANDLORD','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                        """);
                sql.executeUpdate("""
                        INSERT INTO houses (id,landlord_id,name,address_street,ward,city,total_floors,created_at,updated_at)
                        VALUES ('00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000001',
                        'House','Street','Ward','City',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                        """);
                sql.executeUpdate("""
                        INSERT INTO floors VALUES ('00000000-0000-0000-0000-000000000003',
                        '00000000-0000-0000-0000-000000000002',0,'Ground','Existing description')
                        """);
                sql.executeUpdate("""
                        INSERT INTO rooms (id,floor_id,room_number,area,base_price,max_tenants,created_at,updated_at)
                        VALUES ('00000000-0000-0000-0000-000000000004','00000000-0000-0000-0000-000000000003',
                        '101',25,3000000,2,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                        """);
                var result = Flyway.configure().dataSource(dataSource).load().migrate();
                assertThat(result.migrationsExecuted).isEqualTo(2);
                try (var rows = sql.executeQuery("SELECT total_floors FROM houses")) {
                    assertThat(rows.next()).isTrue(); assertThat(rows.getInt(1)).isEqualTo(1);
                }
                try (var rows = sql.executeQuery("SELECT room_number FROM rooms")) {
                    assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("101");
                }
                Flyway.configure().dataSource(dataSource).load().validate();
            }
        }
    }
}
