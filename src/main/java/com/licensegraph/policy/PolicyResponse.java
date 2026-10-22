
package com.licensegraph.policy;

public record PolicyResponse(
        String policyId,
        int version,
        boolean commercialUse,
        boolean redistribution,
        boolean derivatives,
        boolean aiTraining,
        boolean attributionRequired,
        boolean shareAlike
) {
    public static PolicyResponse from(LicensePolicy policy) {
        return new PolicyResponse(
                policy.getPolicyId(),
                policy.getVersion(),
                policy.isCommercialUse(),
                policy.isRedistribution(),
                policy.isDerivatives(),
                policy.isAiTraining(),
                policy.isAttributionRequired(),
                policy.isShareAlike()
        );
    }
}
