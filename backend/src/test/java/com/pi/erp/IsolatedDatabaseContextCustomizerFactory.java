package com.pi.erp;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/** Applies to every Spring test context, before datasource or Flyway beans are created. */
public class IsolatedDatabaseContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
        Class<?> testClass, List<ContextConfigurationAttributes> configAttributes
    ) {
        return new DatabaseGuard();
    }

    private record DatabaseGuard() implements ContextCustomizer {
        @Override
        public void customizeContext(
            ConfigurableApplicationContext context, MergedContextConfiguration configuration
        ) {
            var environment = context.getEnvironment();
            if (!Arrays.asList(environment.getActiveProfiles()).contains("test")) {
                throw new IllegalStateException("Backend tests require the test profile.");
            }
            String url = environment.getProperty("spring.datasource.url", "");
            boolean dedicated = Set.of(
                "jdbc:postgresql://127.0.0.1:55435/jamshub_test",
                "jdbc:postgresql://127.0.0.1:55434/products_verify"
            ).contains(url) || url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55433/auth_verify_[0-9]+");
            if (!dedicated) {
                throw new IllegalStateException(
                    "Set spring.datasource.url explicitly to an isolated verification database."
                );
            }
            for (String key : List.of(
                "spring.flyway.url", "spring.datasource.hikari.jdbc-url",
                "spring.jpa.properties.jakarta.persistence.jdbc.url",
                "spring.jpa.properties.hibernate.connection.url"
            )) {
                String override = environment.getProperty(key);
                if (override != null && !url.equals(override)) {
                    throw new IllegalStateException("Test database override must match datasource: " + key);
                }
            }
            if (environment.containsProperty("spring.datasource.jndi-name")) {
                throw new IllegalStateException("JNDI datasources are not allowed in backend tests.");
            }
        }
    }
}
