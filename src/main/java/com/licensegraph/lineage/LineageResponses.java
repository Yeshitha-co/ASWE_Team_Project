package com.licensegraph.lineage;

import com.licensegraph.asset.PolicyConflict;
import com.licensegraph.common.AssetRef;
import com.licensegraph.common.LineageEdge;
import com.licensegraph.common.LineageGraph;
import com.licensegraph.common.RelationType;
import java.util.List;

/**
 * AI-assisted: Response bodies for Module C endpoints. Assets and policies are printed as
 * "model-C:v1"
 * and "license-A:v1", following the Section 10 naming convention.
 */
public final class LineageResponses {

    private LineageResponses() {
    }

    /**
     * One edge, input to output.
     */
    public record EdgeBody(String input, String output, RelationType relation) {
        static EdgeBody from(LineageEdge edge) {
            return new EdgeBody(edge.input().toString(), edge.output().toString(), edge.relation());
        }
    }

    /**
     * One upstream policy conflict reported by Module B.
     */
    public record ConflictBody(String source, String policy, String field) {
        static ConflictBody from(PolicyConflict conflict) {
            return new ConflictBody(conflict.source().toString(), conflict.policy().toString(),
                    conflict.field());
        }
    }

    /**
     * 201 body for POST /api/derivations.
     */
    public record DerivationResponse(String output, List<EdgeBody> created,
                                     List<ConflictBody> conflicts, int ancestorCount) {

        /**
         * Builds the 201 body from the recorded derivation.
         */
        public static DerivationResponse from(AssetRef output, DerivationResult result) {
            return new DerivationResponse(
                    output.toString(),
                    result.created().stream().map(EdgeBody::from).toList(),
                    result.conflicts().stream().map(ConflictBody::from).toList(),
                    result.ancestorCount());
        }
    }

    /**
     * 200 body for the ancestors and descendants endpoints. Nodes include the root, sorted by name.
     */
    public record GraphResponse(String root, List<String> nodes, List<EdgeBody> edges) {

        /**
         * Builds the 200 body from a lineage subgraph.
         */
        public static GraphResponse from(LineageGraph graph) {
            return new GraphResponse(
                    graph.root().toString(),
                    graph.nodes().stream().map(AssetRef::toString).sorted().toList(),
                    graph.edges().stream().map(EdgeBody::from).toList());
        }
    }
}
