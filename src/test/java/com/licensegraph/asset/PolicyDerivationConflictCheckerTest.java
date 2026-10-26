package com.licensegraph.asset;

import static com.licensegraph.asset.ConflictFixtures.BASE;
import static com.licensegraph.asset.ConflictFixtures.DATASET_A;
import static com.licensegraph.asset.ConflictFixtures.LEFT;
import static com.licensegraph.asset.ConflictFixtures.MODEL_B;
import static com.licensegraph.asset.ConflictFixtures.MODEL_C;
import static com.licensegraph.asset.ConflictFixtures.RIGHT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.AssetType;
import com.licensegraph.common.DerivationInput;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.RelationType;
import com.licensegraph.policy.PolicyLookup;
import com.licensegraph.policy.PolicyView;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AI-assisted: Unit tests for {@link PolicyDerivationConflictChecker} (Module B, tasks B7 and B10).
 *
 * <p>Each test gives every asset its own policy ({@code license-<assetId>:v1}) and says which
 * policies forbid derivatives or AI training; everything else allows both. Graphs come from
 * {@link ConflictFixtures}, which mirror Module C's shared fixtures.
 */
@ExtendWith(MockitoExtension.class)
class PolicyDerivationConflictCheckerTest {

    private static final String CLIENT = "client-a";

    @Mock
    private AssetLookup assets;

    @Mock
    private PolicyLookup policies;

    private PolicyDerivationConflictChecker checker;

    /** Policy flags per asset id: {derivatives, aiTraining}. Unlisted assets allow both. */
    private final Map<String, boolean[]> policyFlags = new HashMap<>();

    @BeforeEach
    void setUp() {
        checker = new PolicyDerivationConflictChecker(assets, policies);
        lenient().when(assets.findAsset(anyString(), any(AssetRef.class))).thenAnswer(call -> {
            AssetRef ref = call.getArgument(1);
            return Optional.of(new AssetView(ref, AssetType.MODEL, policyOf(ref)));
        });
        lenient().when(policies.findPolicy(anyString(), any(PolicyRef.class))).thenAnswer(call -> {
            PolicyRef ref = call.getArgument(1);
            String assetId = ref.policyId().substring("license-".length());
            boolean[] flags = policyFlags.getOrDefault(assetId, new boolean[] {true, true});
            return Optional.of(new PolicyView(ref, true, true, flags[0], flags[1], false, false));
        });
    }

    private static PolicyRef policyOf(AssetRef asset) {
        return new PolicyRef("license-" + asset.assetId(), 1);
    }

    private void forbid(AssetRef asset, boolean derivatives, boolean aiTraining) {
        policyFlags.put(asset.assetId(), new boolean[] {!derivatives, !aiTraining});
    }

    private static Map<AssetRef, LineageGraph> graphs(LineageGraph... graphs) {
        Map<AssetRef, LineageGraph> byInput = new LinkedHashMap<>();
        for (LineageGraph graph : graphs) {
            byInput.put(graph.root(), graph);
        }
        return byInput;
    }

    @Test
    void conflictOnDirectInputWhenDerivativesForbidden() {
        // The work plan's example: Model-B trained on Dataset-A, license-A has derivatives=false.
        forbid(DATASET_A, true, false);

        List<PolicyConflict> conflicts = checker.check(CLIENT,
                List.of(new DerivationInput(DATASET_A, RelationType.TRAINED_ON)),
                graphs(ConflictFixtures.alone(DATASET_A)));

        assertThat(conflicts).containsExactly(
                new PolicyConflict(DATASET_A, policyOf(DATASET_A), "derivatives"));
    }

    @Test
    void trainedOnInputFlagsAiTrainingForbidden() {
        forbid(DATASET_A, false, true);

        List<PolicyConflict> conflicts = checker.check(CLIENT,
                List.of(new DerivationInput(DATASET_A, RelationType.TRAINED_ON)),
                graphs(ConflictFixtures.alone(DATASET_A)));

        assertThat(conflicts).containsExactly(
                new PolicyConflict(DATASET_A, policyOf(DATASET_A), "aiTraining"));
    }

    @Test
    void aiTrainingIsNotFlaggedForOtherRelations() {
        forbid(DATASET_A, false, true);

        for (RelationType relation : List.of(RelationType.FINE_TUNED_FROM, RelationType.MERGED_FROM,
                RelationType.FILTERED_FROM, RelationType.DERIVED_FROM)) {
            assertThat(checker.check(CLIENT,
                    List.of(new DerivationInput(DATASET_A, relation)),
                    graphs(ConflictFixtures.alone(DATASET_A))))
                    .as(relation.name())
                    .isEmpty();
        }
    }

    @Test
    void derivativesForbiddenIsFlaggedForEveryRelation() {
        forbid(DATASET_A, true, false);

        for (RelationType relation : RelationType.values()) {
            assertThat(checker.check(CLIENT,
                    List.of(new DerivationInput(DATASET_A, relation)),
                    graphs(ConflictFixtures.alone(DATASET_A))))
                    .as(relation.name())
                    .extracting(PolicyConflict::field)
                    .containsExactly("derivatives");
        }
    }

    @Test
    void conflictTwoGenerationsUp() {
        // New derivation: model-D fine-tuned from model-C, whose upstream is the A-B-C chain.
        forbid(DATASET_A, true, false);

        List<PolicyConflict> conflicts = checker.check(CLIENT,
                List.of(new DerivationInput(MODEL_C, RelationType.FINE_TUNED_FROM)),
                graphs(ConflictFixtures.chainAncestorsOfModelC()));

        assertThat(conflicts).containsExactly(
                new PolicyConflict(DATASET_A, policyOf(DATASET_A), "derivatives"));
    }

    @Test
    void aiTrainingForbiddenTwoGenerationsUpIsFlaggedForTrainedOnInput() {
        forbid(DATASET_A, false, true);

        List<PolicyConflict> conflicts = checker.check(CLIENT,
                List.of(new DerivationInput(MODEL_C, RelationType.TRAINED_ON)),
                graphs(ConflictFixtures.chainAncestorsOfModelC()));

        assertThat(conflicts).containsExactly(
                new PolicyConflict(DATASET_A, policyOf(DATASET_A), "aiTraining"));
    }

    @Test
    void noConflictOnCleanChain() {
        List<PolicyConflict> conflicts = checker.check(CLIENT,
                List.of(new DerivationInput(MODEL_C, RelationType.TRAINED_ON)),
                graphs(ConflictFixtures.chainAncestorsOfModelC()));

        assertThat(conflicts).isEmpty();
    }

    @Test
    void sharedAncestorInDiamondIsReportedOnce() {
        forbid(BASE, true, false);

        List<PolicyConflict> conflicts = checker.check(CLIENT,
                List.of(new DerivationInput(LEFT, RelationType.MERGED_FROM),
                        new DerivationInput(RIGHT, RelationType.MERGED_FROM)),
                graphs(ConflictFixtures.ancestorsOfLeft(), ConflictFixtures.ancestorsOfRight()));

        assertThat(conflicts).containsExactly(
                new PolicyConflict(BASE, policyOf(BASE), "derivatives"));
    }

    @Test
    void aiTrainingOnlyChecksTheTrainedOnInputsUpstream() {
        // right is trained-on, left is merged-from; base forbids AI training and is upstream
        // of both.
        forbid(BASE, false, true);
        forbid(LEFT, false, true);

        List<PolicyConflict> conflicts = checker.check(CLIENT,
                List.of(new DerivationInput(LEFT, RelationType.MERGED_FROM),
                        new DerivationInput(RIGHT, RelationType.TRAINED_ON)),
                graphs(ConflictFixtures.ancestorsOfLeft(), ConflictFixtures.ancestorsOfRight()));

        // base is reached through the trained-on input; left is only upstream of the
        // merged-from input.
        assertThat(conflicts).containsExactly(
                new PolicyConflict(BASE, policyOf(BASE), "aiTraining"));
    }

    @Test
    void multipleConflictsAreSortedByAssetThenField() {
        forbid(MODEL_B, true, true);
        forbid(DATASET_A, true, true);

        List<PolicyConflict> conflicts = checker.check(CLIENT,
                List.of(new DerivationInput(MODEL_B, RelationType.TRAINED_ON)),
                graphs(ConflictFixtures.ancestorsOfModelB()));

        assertThat(conflicts).containsExactly(
                new PolicyConflict(DATASET_A, policyOf(DATASET_A), "aiTraining"),
                new PolicyConflict(DATASET_A, policyOf(DATASET_A), "derivatives"),
                new PolicyConflict(MODEL_B, policyOf(MODEL_B), "aiTraining"),
                new PolicyConflict(MODEL_B, policyOf(MODEL_B), "derivatives"));
    }

    @Test
    void inputWithoutGraphIsCheckedOnItsOwn() {
        forbid(DATASET_A, true, false);

        List<PolicyConflict> conflicts = checker.check(CLIENT,
                List.of(new DerivationInput(DATASET_A, RelationType.DERIVED_FROM)), Map.of());

        assertThat(conflicts).containsExactly(
                new PolicyConflict(DATASET_A, policyOf(DATASET_A), "derivatives"));
    }

    @Test
    void unknownAssetOrPolicyIsSkippedNotFailed() {
        forbid(DATASET_A, true, false);
        lenient().when(assets.findAsset(CLIENT, MODEL_B)).thenReturn(Optional.empty());
        lenient().when(policies.findPolicy(CLIENT, policyOf(MODEL_C))).thenReturn(Optional.empty());

        List<PolicyConflict> conflicts = checker.check(CLIENT,
                List.of(new DerivationInput(MODEL_C, RelationType.FINE_TUNED_FROM)),
                graphs(ConflictFixtures.chainAncestorsOfModelC()));

        assertThat(conflicts).containsExactly(
                new PolicyConflict(DATASET_A, policyOf(DATASET_A), "derivatives"));
    }
}
