package com.licensegraph.asset;

import static org.assertj.core.api.Assertions.assertThat;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.AssetType;
import com.licensegraph.common.PolicyRef;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * AI-assisted: Checks that a stored asset survives an application restart on the persistent {@code
 * postgres} profile (Module B, tasks B6 and B10).
 *
 * <p>Runs only when {@code DB_URL} is set, because CI has no PostgreSQL. See the README's
 * Database section for how to run it locally.
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class AssetPersistenceRestartTest {

    @Test
    void assetIsStillThereAfterRestart() {
        // A fresh client id per run, so repeated runs against the same database never collide.
        String clientId = "restart-test-" + UUID.randomUUID();
        AssetRef ref = new AssetRef("dataset-A", 1);
        PolicyRef policy = new PolicyRef("license-A", 1);

        try (ConfigurableApplicationContext first = startApplication()) {
            first.getBean(AssetRepository.class)
                    .saveAndFlush(
                            new Asset(clientId, ref, AssetType.DATASET, policy, "restart check"));
        }

        // The first application is fully shut down, so the asset can only come from the database.
        try (ConfigurableApplicationContext second = startApplication()) {
            Asset found = second.getBean(AssetRepository.class)
                    .findByClientIdAndAssetIdAndVersion(clientId, ref.assetId(), ref.version())
                    .orElseThrow(() -> new AssertionError("asset was lost on restart"));

            assertThat(found.getRef()).isEqualTo(ref);
            assertThat(found.getType()).isEqualTo(AssetType.DATASET);
            assertThat(found.getPolicy()).isEqualTo(policy);
            assertThat(found.getMetadata()).isEqualTo("restart check");
        }
    }

    private static ConfigurableApplicationContext startApplication() {
        return new SpringApplicationBuilder(PersistenceOnly.class)
                .profiles("postgres")
                .web(WebApplicationType.NONE)
                .run();
    }

    /**
     * Starts only the database layer for assets (datasource, JPA, the asset repository), so the
     * test does not depend on other modules' beans being present.
     */
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Asset.class)
    @EnableJpaRepositories(basePackageClasses = Asset.class)
    static class PersistenceOnly {
    }
}
