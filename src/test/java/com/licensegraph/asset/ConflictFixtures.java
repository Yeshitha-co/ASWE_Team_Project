package com.licensegraph.asset;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.LineageEdge;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.RelationType;
import java.util.List;
import java.util.Set;

/**
 * Lineage graphs for the conflict checker tests (Module B, task B10).
 *
 * <p>These mirror Module C's shared {@code LineageFixtures} (A-B-C chain and diamond) exactly.
 * C's fixtures are not on {@code main} yet; once they are, replace these with
 * {@code LineageFixtures.chainAncestorsOfModelC()} and {@code diamondAncestorsOfMerged()}.
 *
 * <p>AI-assisted (Claude): drafted with Claude, reviewed by the Module B owner.
 */
final class ConflictFixtures {

    static final AssetRef DATASET_A = new AssetRef("dataset-A", 1);
    static final AssetRef MODEL_B = new AssetRef("model-B", 1);
    static final AssetRef MODEL_C = new AssetRef("model-C", 1);

    static final AssetRef BASE = new AssetRef("base", 1);
    static final AssetRef LEFT = new AssetRef("left", 1);
    static final AssetRef RIGHT = new AssetRef("right", 1);

    private ConflictFixtures() {
    }

    /** Ancestors of model-C:v1 in the chain dataset-A:v1 -> model-B:v1 -> model-C:v1. */
    static LineageGraph chainAncestorsOfModelC() {
        return new LineageGraph(MODEL_C, Set.of(DATASET_A, MODEL_B, MODEL_C), List.of(
                new LineageEdge(DATASET_A, MODEL_B, RelationType.TRAINED_ON),
                new LineageEdge(MODEL_B, MODEL_C, RelationType.FINE_TUNED_FROM)));
    }

    /** Ancestors of model-B:v1: model-B:v1 trained on dataset-A:v1. */
    static LineageGraph ancestorsOfModelB() {
        return new LineageGraph(MODEL_B, Set.of(DATASET_A, MODEL_B), List.of(
                new LineageEdge(DATASET_A, MODEL_B, RelationType.TRAINED_ON)));
    }

    /** A single asset with no ancestors. */
    static LineageGraph alone(AssetRef asset) {
        return new LineageGraph(asset, Set.of(asset), List.of());
    }

    /** Ancestors of left:v1 in the diamond (left fine-tuned from base). */
    static LineageGraph ancestorsOfLeft() {
        return new LineageGraph(LEFT, Set.of(BASE, LEFT), List.of(
                new LineageEdge(BASE, LEFT, RelationType.FINE_TUNED_FROM)));
    }

    /** Ancestors of right:v1 in the diamond (right filtered from base). */
    static LineageGraph ancestorsOfRight() {
        return new LineageGraph(RIGHT, Set.of(BASE, RIGHT), List.of(
                new LineageEdge(BASE, RIGHT, RelationType.FILTERED_FROM)));
    }
}
