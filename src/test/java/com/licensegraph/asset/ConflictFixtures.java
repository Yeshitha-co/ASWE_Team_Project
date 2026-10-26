package com.licensegraph.asset;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.LineageEdge;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.RelationType;
import com.licensegraph.lineage.LineageFixtures;
import java.util.List;
import java.util.Set;

/**
 * AI-assisted: Lineage graphs for the conflict checker tests (Module B, task B10).
 *
 * <p>The A-B-C chain and the asset names come from Module C's shared {@link LineageFixtures}.
 * The extra single-input graphs below (one parent of the chain or of the diamond) are built
 * from the same assets, because C's fixtures only describe the whole graph.
 */
final class ConflictFixtures {

    static final AssetRef DATASET_A = LineageFixtures.DATASET_A;
    static final AssetRef MODEL_B = LineageFixtures.MODEL_B;
    static final AssetRef MODEL_C = LineageFixtures.MODEL_C;

    static final AssetRef BASE = LineageFixtures.BASE;
    static final AssetRef LEFT = LineageFixtures.LEFT;
    static final AssetRef RIGHT = LineageFixtures.RIGHT;

    private ConflictFixtures() {
    }

    /** Ancestors of model-C:v1 in the chain dataset-A:v1 -> model-B:v1 -> model-C:v1. */
    static LineageGraph chainAncestorsOfModelC() {
        return LineageFixtures.chainAncestorsOfModelC();
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
