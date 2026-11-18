package in.joyway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsRenderStylePostgresUrl() {
        DatabaseUrlEnvironmentPostProcessor.ParsedJdbc parsed =
                DatabaseUrlEnvironmentPostProcessor.parsePostgresUrl(
                        "postgresql://joyway:p%40ss@dpg-abc.oregon-postgres.render.com/joyway");

        assertEquals(
                "jdbc:postgresql://dpg-abc.oregon-postgres.render.com:5432/joyway?sslmode=require",
                parsed.jdbcUrl());
        assertEquals("joyway", parsed.username());
        assertEquals("p@ss", parsed.password());
    }

    @Test
    void preservesExistingSslMode() {
        DatabaseUrlEnvironmentPostProcessor.ParsedJdbc parsed =
                DatabaseUrlEnvironmentPostProcessor.parsePostgresUrl(
                        "postgres://u:p@db.example.com:6543/app?sslmode=verify-full");

        assertEquals(
                "jdbc:postgresql://db.example.com:6543/app?sslmode=verify-full",
                parsed.jdbcUrl());
    }

    @Test
    void ignoresJdbcUrls() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DATABASE_URL", "jdbc:postgresql://localhost:5432/joyway");

        new DatabaseUrlEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertFalse(environment.getPropertySources()
                .contains(DatabaseUrlEnvironmentPostProcessor.PROPERTY_SOURCE_NAME));
    }
}
