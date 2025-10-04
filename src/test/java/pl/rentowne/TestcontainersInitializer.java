package pl.rentowne;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class TestcontainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16.1")
                    .withDatabaseName("rentowne")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withExposedPorts(5432);

    static {
        postgres.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword()
        ).applyTo(ctx.getEnvironment());
    }
}

