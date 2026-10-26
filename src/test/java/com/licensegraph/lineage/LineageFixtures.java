package com.licensegraph.lineage;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.LineageEdge;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.RelationType;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * AI-assisted: Shared lineage fixtures for tests in B, C and D (C8): a simple chain and a diamond.
 *
 * <p>Chain: dataset-A:v1 -> model-B:v1 -> model-C:v1.
 * Diamond: base:v1 -> left:v1, base:v1 -> right:v1, left:v1 -> merged:v1, right:v1 -> merged:v1.
 */
public final class LineageFixtures {

    public static final String CLIENT = "client-a";
    public static final String OTHER_CLIENT = "client-b";

    public static final AssetRef DATASET_A = new AssetRef("dataset-A", 1);
    public static final AssetRef MODEL_B = new AssetRef("model-B", 1);
    public static final AssetRef MODEL_C = new AssetRef("model-C", 1);

    public static final AssetRef BASE = new AssetRef("base", 1);
    public static final AssetRef LEFT = new AssetRef("left", 1);
    public static final AssetRef RIGHT = new AssetRef("right", 1);
    public static final AssetRef MERGED = new AssetRef("merged", 1);

    private LineageFixtures() {
    }

    /** The two edges of the chain, input to output. */
    public static List<LineageEdge> chainEdges() {
        return List.of(
                new LineageEdge(DATASET_A, MODEL_B, RelationType.TRAINED_ON),
                new LineageEdge(MODEL_B, MODEL_C, RelationType.FINE_TUNED_FROM));
    }

    /** The four edges of the diamond, input to output. */
    public static List<LineageEdge> diamondEdges() {
        return List.of(
                new LineageEdge(BASE, LEFT, RelationType.FINE_TUNED_FROM),
                new LineageEdge(BASE, RIGHT, RelationType.FILTERED_FROM),
                new LineageEdge(LEFT, MERGED, RelationType.MERGED_FROM),
                new LineageEdge(RIGHT, MERGED, RelationType.MERGED_FROM));
    }

    /** What findAncestors(model-C) returns for the chain. Ready-made input for B and D tests. */
    public static LineageGraph chainAncestorsOfModelC() {
        return new LineageGraph(MODEL_C, Set.of(DATASET_A, MODEL_B, MODEL_C), chainEdges());
    }

    /** What findAncestors(merged) returns for the diamond. The shared base appears once. */
    public static LineageGraph diamondAncestorsOfMerged() {
        return new LineageGraph(MERGED, Set.of(BASE, LEFT, RIGHT, MERGED), diamondEdges());
    }

    /** Saves the given edges for one client, for tests that use the real repository. */
    public static void store(
            DerivationEdgeRepository repository, String clientId, List<LineageEdge> edges) {
        Instant now = Instant.now();
        for (LineageEdge edge : edges) {
            repository.save(new DerivationEdge(
                    clientId, edge.input(), edge.output(), edge.relation(), now));
        }
    }
}