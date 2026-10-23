package com.licensegraph.asset;

import static com.licensegraph.asset.ConflictFixtures.DATASET_A;
import static com.licensegraph.asset.ConflictFixtures.MODEL_B;
import static com.licensegraph.asset.ConflictFixtures.MODEL_C;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.licensegraph.common.AssetType;
import com.licensegraph.common.DerivationInput;
import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.RelationType;
import com.licensegraph.policy.PolicyLookup;
import com.licensegraph.policy.PolicyView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * {@link PolicyDerivationConflictChecker} with real stored assets and the real
 * {@link JpaAssetLookup}; only Module A's {@link PolicyLookup} is mocked (Module B, B7 and B10).
 *
 * <p>Uses the work plan's running example: dataset-A:v1 (license-A:v1) -> model-B:v1
 * (license-B:v1) -> model-C:v1 (license-C:v1).
 *
 * <p>AI-assisted (Claude): tests drafted with Claude, reviewed by the Module B owner.
 */
@DataJpaTest
@Import({JpaAssetLookup.class, PolicyDerivationConflictChecker.class})
class PolicyDerivationConflictCheckerIntegrationTest {

    private static final String CLIENT = "client-a";
    private static final PolicyRef LICENSE_A_V1 = new PolicyRef("license-A", 1);

    @Autowired
    private AssetRepository repository;

    @Autowired
    private DerivationConflictChecker checker;

    @MockitoBean
    private PolicyLookup policies;

    private boolean licenseAAllowsDerivatives;

    @BeforeEach
    void storeChain() {
        repository.saveAndFlush(new Asset(CLIENT, DATASET_A, AssetType.DATASET, LICENSE_A_V1, null));
        repository.saveAndFlush(new Asset(CLIENT, MODEL_B, AssetType.MODEL, new PolicyRef("license-B", 1), null));
        repository.saveAndFlush(new Asset(CLIENT, MODEL_C, AssetType.MODEL, new PolicyRef("license-C", 1), null));
        licenseAAllowsDerivatives = true;
        when(policies.findPolicy(anyString(), any(PolicyRef.class))).thenAnswer(call -> {
            PolicyRef ref = call.getArgument(1);
            boolean derivatives = !ref.equals(LICENSE_A_V1) || licenseAAllowsDerivatives;
            return Optional.of(new PolicyView(ref, true, true, derivatives, true, false, false));
        });
    }

    @Test
    void reportsDatasetTwoGenerationsUpWithItsExactPolicyVersion() {
        licenseAAllowsDerivatives = false;

        assertThat(checker.check(CLIENT,
                List.of(new DerivationInput(MODEL_C, RelationType.FINE_TUNED_FROM)),
                Map.of(MODEL_C, ConflictFixtures.chainAncestorsOfModelC())))
                .containsExactly(new PolicyConflict(DATASET_A, LICENSE_A_V1, "derivatives"));
    }

    @Test
    void cleanChainHasNoConflicts() {
        assertThat(checker.check(CLIENT,
                List.of(new DerivationInput(MODEL_C, RelationType.FINE_TUNED_FROM)),
                Map.of(MODEL_C, ConflictFixtures.chainAncestorsOfModelC())))
                .isEmpty();
    }

    @Test
    void anotherClientsGraphFindsNoAssetsAndNoConflicts() {
        licenseAAllowsDerivatives = false;

        assertThat(checker.check("client-b",
                List.of(new DerivationInput(MODEL_C, RelationType.FINE_TUNED_FROM)),
                Map.of(MODEL_C, ConflictFixtures.chainAncestorsOfModelC())))
                .isEmpty();
    }
}
