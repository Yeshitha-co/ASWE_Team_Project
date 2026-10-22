
package com.licensegraph.policy;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

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
