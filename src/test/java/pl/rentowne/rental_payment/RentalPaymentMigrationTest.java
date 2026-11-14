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
    void shouldMigrateExistingPositionsWithoutChangingBillsAndAllowManualDepositInstalments() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("postgres:16.1")) {
            postgres.start();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
                var sql = connection.createStatement();
                sql.execute("CREATE TABLE settlement(settlement_id BIGINT PRIMARY KEY, tenant_id BIGINT, deposit_assessment BOOLEAN, deposit_top_up NUMERIC(14,2), additional_charge NUMERIC(14,2), additional_charge_description VARCHAR(500), total_amount NUMERIC(14,2))");
                sql.execute("CREATE UNIQUE INDEX uk_settlement_deposit_assessment ON settlement(tenant_id) WHERE deposit_assessment=true");
                sql.execute("INSERT INTO settlement VALUES (1,7,true,500,75,'Klucze',3009.79),(2,8,false,0,30,'Krzesło',2464.79),(3,9,false,0,0,NULL,2434.79)");
                try (var liquibase = new Liquibase("db/changelog/ddl-scripts/069-manual-settlement-charges.sql", new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) {
                    liquibase.update(new Contexts(), new LabelExpression());
                    var positions = sql.executeQuery("SELECT type,amount,description FROM settlement_charge WHERE settlement_id=1 ORDER BY position");
                    positions.next();
                    assertThat(positions.getString(1)).isEqualTo("DEPOSIT");
                    assertThat(positions.getBigDecimal(2)).isEqualByComparingTo("500");
                    positions.next();
                    assertThat(positions.getString(1)).isEqualTo("OTHER");
                    assertThat(positions.getString(3)).isEqualTo("Klucze");
                    var total = sql.executeQuery("SELECT total_amount FROM settlement WHERE settlement_id=1");
                    total.next();
                    assertThat(total.getBigDecimal(1)).isEqualByComparingTo("3009.79");
                    sql.execute("INSERT INTO settlement VALUES (4,7,true,200,0,NULL,2634.79)");
                    liquibase.update(new Contexts(), new LabelExpression());
                    var count = sql.executeQuery("SELECT count(*) FROM settlement_charge");
                    count.next();
                    assertThat(count.getInt(1)).isEqualTo(3);
                }
            }
        }
    }

    @Test
    void shouldPreserveHistoricalDepositAndChargesWithoutInferringArrears() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("postgres:16.1")) {
            postgres.start();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
                var sql = connection.createStatement();
                sql.execute("CREATE TABLE tenant(tenant_id BIGINT PRIMARY KEY)");
                sql.execute("CREATE TABLE lease_agreement(lease_agreement_id BIGINT PRIMARY KEY, deposit_paid NUMERIC(14,2) NOT NULL)");
                sql.execute("CREATE TABLE rental_payment(amount NUMERIC(14,2))");
                sql.execute("CREATE TABLE settlement(settlement_id BIGINT PRIMARY KEY, tenant_id BIGINT, total_amount NUMERIC(14,2))");
                sql.execute("INSERT INTO tenant VALUES (1),(2)");
                sql.execute("INSERT INTO lease_agreement VALUES (1,2000),(2,0)");
                sql.execute("INSERT INTO settlement VALUES (1,1,2434.79)");
                try (var liquibase = new Liquibase("db/changelog/ddl-scripts/068-deposit-and-extra-charges.sql", new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) {
                    liquibase.update(new Contexts(), new LabelExpression());
                    var leases = sql.executeQuery("SELECT deposit_paid, deposit_opening_paid, deposit_balance_confirmed FROM lease_agreement ORDER BY lease_agreement_id");
                    leases.next();
                    assertThat(leases.getBigDecimal(1)).isEqualByComparingTo("2000");
                    assertThat(leases.getBigDecimal(2)).isEqualByComparingTo("2000");
                    assertThat(leases.getBoolean(3)).isFalse();
                    leases.next();
                    assertThat(leases.getBigDecimal(1)).isZero();
                    assertThat(leases.getBoolean(3)).isFalse();
                    var settlement = sql.executeQuery("SELECT total_amount, deposit_top_up, additional_charge, deposit_assessment FROM settlement");
                    settlement.next();
                    assertThat(settlement.getBigDecimal(1)).isEqualByComparingTo("2434.79");
                    assertThat(settlement.getBigDecimal(2)).isZero();
                    assertThat(settlement.getBigDecimal(3)).isZero();
                    assertThat(settlement.getBoolean(4)).isFalse();
                    sql.execute("INSERT INTO lease_agreement(lease_agreement_id,deposit_paid) VALUES (3,NULL)");
                    liquibase.update(new Contexts(), new LabelExpression());
                    var receipts = sql.executeQuery("SELECT count(*) FROM rental_payment");
                    receipts.next();
                    assertThat(receipts.getInt(1)).isZero();
                }
            }
        }
    }

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
