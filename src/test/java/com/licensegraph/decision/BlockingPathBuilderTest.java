
package com.licensegraph.decision;

import com.licensegraph.common.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockingPathBuilderTest {

    @Test
    void shouldFindPathFromModelCToDatasetA() {

        // Step 1: Create three assets
        AssetRef datasetA = new AssetRef("Dataset-A", 1);
        AssetRef modelB = new AssetRef("Model-B", 1);
        AssetRef modelC = new AssetRef("Model-C", 1);

        // Step 2: Create connections between assets
        LineageEdge edge1 = new LineageEdge(
                datasetA, modelB, null
        );

        LineageEdge edge2 = new LineageEdge(
                modelB, modelC, null
        );

        // Step 3: Create the lineage graph
        LineageGraph graph = new LineageGraph(
                modelC,
                Set.of(datasetA, modelB, modelC),
                List.of(edge1, edge2)
        );

        // Step 4: Identify the blocking license
        PolicyRef licenseA = new PolicyRef("License-A", 1);

        // Step 5: Run our BFS implementation
        BlockingPathBuilder builder = new BlockingPathBuilder();

        BlockingPath result = builder.build(
                graph,
                modelC,
                datasetA,
                licenseA,
                "commercialUse"
        );

        // Step 6: Check the result
        assertEquals(
                List.of(modelC, modelB, datasetA),
                result.path()
        );

        assertEquals(licenseA, result.blockingPolicy());
        assertEquals("commercialUse", result.blockingField());
    }

    @Test
    void shouldChooseShortestBlockingPath() {

        AssetRef datasetA = new AssetRef("Dataset-A", 1);
        AssetRef modelB = new AssetRef("Model-B", 1);
        AssetRef modelC = new AssetRef("Model-C", 1);
        AssetRef modelD = new AssetRef("Model-D", 1);

        PolicyRef licenseA = new PolicyRef("License-A", 1);

        LineageGraph graph = new LineageGraph(
                modelC,
                Set.of(datasetA, modelB, modelC, modelD),
                List.of(
                        new LineageEdge(datasetA, modelB, null),
                        new LineageEdge(modelB, modelC, null),
                        new LineageEdge(datasetA, modelD, null),
                        new LineageEdge(modelD, modelC, null),
                        new LineageEdge(datasetA, modelC, null)
                )
        );

        BlockingPathBuilder builder = new BlockingPathBuilder();

        BlockingPath result = builder.build(
                graph,
                modelC,
                datasetA,
                licenseA,
                "commercialUse"
        );

        assertEquals(
                List.of(modelC, datasetA),
                result.path()
        );

        assertEquals(licenseA, result.blockingPolicy());
        assertEquals("commercialUse", result.blockingField());
    }

}
