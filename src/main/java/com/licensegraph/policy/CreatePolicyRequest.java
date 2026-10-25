
package com.licensegraph.policy;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * AI-assisted: Contains the fields and validation rules for creating a license policy.
 *
 * @param policyId the policy identifier
 * @param version the positive policy version
 * @param commercialUse whether commercial use is permitted
 * @param redistribution whether redistribution is permitted
 * @param derivatives whether derivative works are permitted
 * @param aiTraining whether AI training is permitted
 * @param attributionRequired whether attribution is required
 * @param shareAlike whether derivative works must use the same license
 *
 */
public record CreatePolicyRequest(

        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
        String policyId,

        @NotNull
        @Min(1)
        Integer version,

        @NotNull
        Boolean commercialUse,

        @NotNull
        Boolean redistribution,

        @NotNull
        Boolean derivatives,

        @NotNull
        Boolean aiTraining,

        @NotNull
        Boolean attributionRequired,

        @NotNull
        Boolean shareAlike
) {
}
