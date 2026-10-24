
package com.licensegraph.policy;

/**
 * Represents a license policy returned by the API.
 *
 * @param policyId the policy identifier
 * @param version the policy version
 * @param commercialUse whether commercial use is permitted
 * @param redistribution whether redistribution is permitted
 * @param derivatives whether derivative works are permitted
 * @param aiTraining whether AI training is permitted
 * @param attributionRequired whether attribution is required
 * @param shareAlike whether derivative works must use the same license
 */
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
    /**
     * AI-assisted: Creates an API response from a license policy entity.
     *
     * @param policy the license policy entity
     * @return the corresponding policy response
     */
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
