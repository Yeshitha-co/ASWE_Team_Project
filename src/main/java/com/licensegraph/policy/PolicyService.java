
package com.licensegraph.policy;

import com.licensegraph.common.PolicyRef;
import java.util.List;

public interface PolicyService {

    LicensePolicy createPolicy(String clientId, LicensePolicy policy);

    List<LicensePolicy> findPolicyVersions(String clientId, String policyId);

    LicensePolicy findPolicyVersion(String clientId, PolicyRef ref);
}
