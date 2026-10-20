package com.licensegraph.decision;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.PolicyRef;
import java.util.List;

/**
 * One shortest path from the requested asset to a blocking ancestor (Section 10, Module D).
 *
 * <p>Path direction is queried asset toward the source, for example
 * {@code [model-C:v1, model-B:v1, dataset-A:v1]}.
 */
public record BlockingPath(List<AssetRef> path, PolicyRef blockingPolicy, String blockingField) {
}
