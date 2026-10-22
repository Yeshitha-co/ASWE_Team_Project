
package com.licensegraph.policy;

import com.licensegraph.common.AssetRef;
import com.licensegraph.common.IntendedUse;
import com.licensegraph.common.Obligation;

import java.util.List;

public class PolicyRulesImpl implements PolicyRules {

    @Override
    public RuleOutcome check(PolicyView policy, IntendedUse use) {
        return switch (use) {
            case COMMERCIAL ->
                    new RuleOutcome(policy.commercialUse(),
                            policy.commercialUse() ? null : "commercialUse");

            case REDISTRIBUTION ->
                    new RuleOutcome(policy.redistribution(),
                            policy.redistribution() ? null : "redistribution");

            case DERIVATIVE ->
                    new RuleOutcome(policy.derivatives(),
                            policy.derivatives() ? null : "derivatives");

            case AI_TRAINING ->
                    new RuleOutcome(policy.aiTraining(),
                            policy.aiTraining() ? null : "aiTraining");

            case INTERNAL -> new RuleOutcome(true, null);
        };
    }

    @Override
    public List<Obligation> obligations(
            AssetRef source, PolicyView policy) {

        List<Obligation> obligations = new java.util.ArrayList<>();

        if (policy.attributionRequired()) {
            obligations.add(new Obligation(
                    com.licensegraph.common.ObligationType.ATTRIBUTION,
                    source,
                    policy.ref()));
        }

        if (policy.shareAlike()) {
            obligations.add(new Obligation(
                    com.licensegraph.common.ObligationType.SHARE_ALIKE,
                    source,
                    policy.ref()));
        }

        return obligations;
    }


    @Override
    public List<Obligation> mergeObligations(List<Obligation> all) {
        java.util.Map<String, Obligation> unique = new java.util.LinkedHashMap<>();

        for (Obligation obligation : all) {
            String key = obligation.source().assetId()
                    + ":" + obligation.source().version()
                    + "|" + obligation.type().toJson();

            unique.putIfAbsent(key, obligation);
        }

        return unique.values().stream()
                .sorted(
                        java.util.Comparator
                                .comparing((Obligation o) -> o.source().assetId())
                                .thenComparingInt(o -> o.source().version())
                                .thenComparing(o -> o.type().toJson())
                )
                .toList();
    }
}