package com.licensegraph.lineage;

import static com.licensegraph.lineage.LineageFixtures.BASE;
import static com.licensegraph.lineage.LineageFixtures.CLIENT;
import static com.licensegraph.lineage.LineageFixtures.DATASET_A;
import static com.licensegraph.lineage.LineageFixtures.LEFT;
import static com.licensegraph.lineage.LineageFixtures.MERGED;
import static com.licensegraph.lineage.LineageFixtures.MODEL_B;
import static com.licensegraph.lineage.LineageFixtures.MODEL_C;
import static com.licensegraph.lineage.LineageFixtures.OTHER_CLIENT;
import static com.licensegraph.lineage.LineageFixtures.RIGHT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.licensegraph.asset.AssetLookup;
import com.licensegraph.asset.AssetView;
import com.licensegraph.asset.DerivationConflictChecker;
import com.licensegraph.asset.PolicyConflict;
import com.licensegraph.common.AssetRef;
import com.licensegraph.common.DerivationInput;
import com.licensegraph.common.LineageEdge;
import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.RelationType;
import com.licensegraph.common.exception.ConflictException;
import com.licensegraph.common.exception.ErrorCode;
import com.licensegraph.common.exception.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests recording derivations: validation, cycle and duplicate rejection, rollback, stored
 * conflicts, client isolation and concurrent requests (C9). Each test commits for real so the
 * concurrency test sees what other threads wrote; tables are cleared after every test.
 */
// AI-assisted: drafted with Claude, reviewed by Srishti Singh.
@DataJpaTest
@Import({DerivationServiceImpl.class, LineageQueryServiceImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DerivationServiceImplTest {

    @Autowired
    private DerivationService service;

    @Autowired
    private DerivationEdgeRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private AssetLookup assets;

    @MockitoBean
    private DerivationConflictChecker conflictChecker;

    @BeforeEach
    void everyAssetExists() {
        when(assets.findAsset(anyString(), any())).thenReturn(Optional.of(mock(AssetView.class)));
    }

    @AfterEach
    void clearTables() {
        repository.deleteAll();
    }

    private static DerivationInput in(AssetRef asset, RelationType relation) {
        return new DerivationInput(asset, relation);
    }

    @Test
    void simpleChainAddsOneEdgeAndCountsAncestors() {
        LineageFixtures.store(repository, CLIENT,
                List.of(new LineageEdge(DATASET_A, MODEL_B, RelationType.TRAINED_ON)));

        DerivationResult result = service.recordDerivation(
                CLIENT, MODEL_C, List.of(in(MODEL_B, RelationType.FINE_TUNED_FROM)));

        assertThat(result.created())
                .containsExactly(new LineageEdge(MODEL_B, MODEL_C, RelationType.FINE_TUNED_FROM));
        assertThat(result.ancestorCount()).isEqualTo(2);
        assertThat(result.conflicts()).isEmpty();
    }

    @Test
    void multipleParentsCreateOneEdgeEach() {
        LineageFixtures.store(repository, CLIENT, List.of(
                new LineageEdge(BASE, LEFT, RelationType.FINE_TUNED_FROM),
                new LineageEdge(BASE, RIGHT, RelationType.FILTERED_FROM)));

        DerivationResult result = service.recordDerivation(CLIENT, MERGED, List.of(
                in(LEFT, RelationType.MERGED_FROM), in(RIGHT, RelationType.MERGED_FROM)));

        assertThat(result.created()).hasSize(2);
        assertThat(result.ancestorCount()).isEqualTo(3);
    }

    @Test
    void unknownInputRejectsWholeBatch() {
        AssetRef missing = new AssetRef("missing", 1);
        when(assets.findAsset(CLIENT, missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordDerivation(CLIENT, MODEL_C, List.of(
                in(MODEL_B, RelationType.FINE_TUNED_FROM), in(missing, RelationType.DERIVED_FROM))))
                .isInstanceOf(UnprocessableEntityException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void duplicateStoredEdgeIsRejected() {
        LineageFixtures.store(repository, CLIENT, LineageFixtures.chainEdges());

        assertThatThrownBy(() -> service.recordDerivation(
                CLIENT, MODEL_B, List.of(in(DATASET_A, RelationType.TRAINED_ON))))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.DUPLICATE_EDGE));
    }

    @Test
    void duplicateInputInOneRequestIsRejected() {
        assertThatThrownBy(() -> service.recordDerivation(CLIENT, MODEL_B, List.of(
                in(DATASET_A, RelationType.TRAINED_ON), in(DATASET_A, RelationType.TRAINED_ON))))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.DUPLICATE_EDGE));
        assertThat(repository.count()).isZero();
    }

    @Test
    void selfCycleIsRejected() {
        assertThatThrownBy(() -> service.recordDerivation(
                CLIENT, MODEL_B, List.of(in(MODEL_B, RelationType.DERIVED_FROM))))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.CYCLE_DETECTED));
    }

    @Test
    void longCycleIsRejectedWithItsPath() {
        LineageFixtures.store(repository, CLIENT, LineageFixtures.chainEdges());

        assertThatThrownBy(() -> service.recordDerivation(
                CLIENT, DATASET_A, List.of(in(MODEL_C, RelationType.DERIVED_FROM))))
                .isInstanceOfSatisfying(ConflictException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.CYCLE_DETECTED);
                    assertThat(e.details().get("cyclePath"))
                            .isEqualTo(List.of("dataset-A:v1", "model-C:v1", "model-B:v1", "dataset-A:v1"));
                });
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void conflictsAreReturnedAndKeptOnTheEdge() {
        PolicyConflict conflict = new PolicyConflict(DATASET_A, new PolicyRef("license-A", 1), "derivatives");
        when(conflictChecker.check(any(), any(), any())).thenReturn(List.of(conflict));

        DerivationResult result = service.recordDerivation(
                CLIENT, MODEL_B, List.of(in(DATASET_A, RelationType.TRAINED_ON)));

        assertThat(result.conflicts()).containsExactly(conflict);
        List<PolicyConflict> stored = new TransactionTemplate(transactionManager)
                .execute(status -> repository.findAll().get(0).getConflicts());
        assertThat(stored).containsExactly(conflict);
    }

    @Test
    void otherClientsEdgesDoNotCauseCycles() {
        LineageFixtures.store(repository, CLIENT, LineageFixtures.chainEdges());

        DerivationResult result = service.recordDerivation(
                OTHER_CLIENT, DATASET_A, List.of(in(MODEL_C, RelationType.DERIVED_FROM)));

        assertThat(result.created()).hasSize(1);
    }

    @Test
    void concurrentOppositeEdgesCannotBothSucceed() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<String> forward = attempt(start, MODEL_B, DATASET_A);
            Callable<String> backward = attempt(start, DATASET_A, MODEL_B);
            List<Future<String>> futures = new ArrayList<>(List.of(pool.submit(forward), pool.submit(backward)));
            start.countDown();

            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get());
            }
            assertThat(outcomes).containsExactlyInAnyOrder("ok", "CYCLE_DETECTED");
            assertThat(repository.count()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    /** Waits for the start signal, then records output-from-input; returns "ok" or the error code. */
    private Callable<String> attempt(CountDownLatch start, AssetRef output, AssetRef input) {
        return () -> {
            start.await();
            try {
                service.recordDerivation(CLIENT, output, List.of(in(input, RelationType.DERIVED_FROM)));
                return "ok";
            } catch (ConflictException e) {
                return e.code().name();
            }
        };
    }
}