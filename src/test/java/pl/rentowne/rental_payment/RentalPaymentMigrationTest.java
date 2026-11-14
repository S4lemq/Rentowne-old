package pl.rentowne.rental_payment;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

class RentalPaymentMigrationTest {
    @Test
    void shouldCoverAllHistoricalSettlementsOnceWithoutFabricatingReceipts() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("postgres:16.1")) {
            postgres.start();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
                var sql = connection.createStatement();
                sql.execute("CREATE TABLE tenant(tenant_id BIGINT PRIMARY KEY)");
                sql.execute("CREATE TABLE rented_object(rented_object_id BIGINT PRIMARY KEY)");
                sql.execute("CREATE TABLE settlement(settlement_id BIGINT PRIMARY KEY,tenant_id BIGINT,rented_object_id BIGINT,total_amount NUMERIC(19,2),status TEXT)");
                sql.execute("INSERT INTO tenant VALUES (1),(2),(3)");
                sql.execute("INSERT INTO rented_object VALUES (1),(2)");
                sql.execute("INSERT INTO settlement VALUES (1,1,1,2539.89,'TO_PAY'),(2,1,1,800,'PAID'),(3,2,2,1500,'TO_PAY'),(4,NULL,2,NULL,'TO_PAY')");
                try (var liquibase = new Liquibase("db/changelog/ddl-scripts/067-rental-payments.sql", new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) {
                    liquibase.update(new Contexts(), new LabelExpression());
                    var totals = sql.executeQuery("SELECT SUM(s.total_amount - c.amount) FROM settlement s JOIN settlement_opening_credit c USING(settlement_id)");
                    totals.next();
                    assertThat(totals.getBigDecimal(1)).isZero();
                    var receipts = sql.executeQuery("SELECT count(*) FROM rental_payment"); receipts.next();
                    assertThat(receipts.getLong(1)).isZero();
                    var statuses = sql.executeQuery("SELECT status,total_amount FROM settlement WHERE settlement_id = 1"); statuses.next();
                    assertThat(statuses.getString(1)).isEqualTo("TO_PAY");
                    assertThat(statuses.getBigDecimal(2)).isEqualByComparingTo("2539.89");
                    sql.execute("INSERT INTO settlement VALUES (5,1,1,2600,'TO_PAY')");
                    liquibase.update(new Contexts(), new LabelExpression());
                    var coverage = sql.executeQuery("SELECT count(*) FROM settlement_opening_credit"); coverage.next();
                    assertThat(coverage.getLong(1)).isEqualTo(4);
                    var newDebt = sql.executeQuery("SELECT SUM(s.total_amount - COALESCE(c.amount,0)) FROM settlement s LEFT JOIN settlement_opening_credit c USING(settlement_id) WHERE s.tenant_id = 1"); newDebt.next();
                    assertThat(newDebt.getBigDecimal(1)).isEqualByComparingTo("2600");
                    sql.execute("DELETE FROM settlement WHERE settlement_id = 1");
                    var deletedCoverage = sql.executeQuery("SELECT count(*) FROM settlement_opening_credit WHERE settlement_id = 1"); deletedCoverage.next();
                    assertThat(deletedCoverage.getLong(1)).isZero();
                }
            }
        }
    }
}
