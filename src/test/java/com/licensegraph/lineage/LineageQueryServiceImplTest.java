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
import com.licensegraph.common.AssetRef;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.exception.NotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Tests ancestor and descendant walks against a real in-memory database, with Module B's
 * asset lookup mocked (C9).
 */
@DataJpaTest
@Import(LineageQueryServiceImpl.class)
class LineageQueryServiceImplTest {

    @Autowired
    private DerivationEdgeRepository repository;

    @Autowired
    private LineageQueryService service;

    @MockitoBean
    private AssetLookup assets;

    @BeforeEach
    void everyAssetExists() {
        when(assets.findAsset(anyString(), any())).thenReturn(Optional.of(mock(AssetView.class)));
    }

    @Test
    void ancestorsOfChainEndContainTheWholeChain() {
        LineageFixtures.store(repository, CLIENT, LineageFixtures.chainEdges());

        LineageGraph graph = service.findAncestors(CLIENT, MODEL_C);

        assertThat(graph.root()).isEqualTo(MODEL_C);
        assertThat(graph.nodes()).containsExactlyInAnyOrder(DATASET_A, MODEL_B, MODEL_C);
        assertThat(graph.edges()).containsExactlyInAnyOrderElementsOf(LineageFixtures.chainEdges());
    }

    @Test
    void diamondVisitsSharedAncestorOnce() {
        LineageFixtures.store(repository, CLIENT, LineageFixtures.diamondEdges());

        LineageGraph graph = service.findAncestors(CLIENT, MERGED);

        assertThat(graph.nodes()).containsExactlyInAnyOrder(BASE, LEFT, RIGHT, MERGED);
        assertThat(graph.edges()).hasSize(4);
    }

    @Test
    void descendantsOfDatasetAReachTheWholeChain() {
        LineageFixtures.store(repository, CLIENT, LineageFixtures.chainEdges());

        LineageGraph graph = service.findDescendants(CLIENT, DATASET_A);

        assertThat(graph.nodes()).containsExactlyInAnyOrder(DATASET_A, MODEL_B, MODEL_C);
        assertThat(graph.edges()).hasSize(2);
    }

    @Test
    void anotherClientNeverSeesTheseEdges() {
        LineageFixtures.store(repository, CLIENT, LineageFixtures.chainEdges());

        LineageGraph graph = service.findAncestors(OTHER_CLIENT, MODEL_C);

        assertThat(graph.nodes()).containsExactly(MODEL_C);
        assertThat(graph.edges()).isEmpty();
    }

    @Test
    void unknownAssetIsNotFound() {
        AssetRef missing = new AssetRef("missing", 1);
        when(assets.findAsset(CLIENT, missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAncestors(CLIENT, missing))
                .isInstanceOf(NotFoundException.class);
    }
}