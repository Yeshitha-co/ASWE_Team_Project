
package com.licensegraph.policy;

import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.exception.BadRequestException;
import com.licensegraph.common.exception.ConflictException;
import com.licensegraph.common.exception.ErrorCode;
import com.licensegraph.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements license policy creation and retrieval operations.
 */
@Service
@Transactional
public class PolicyServiceImpl implements PolicyService {

    private final LicensePolicyRepository repository;

    public PolicyServiceImpl(LicensePolicyRepository repository) {
        this.repository = repository;
    }

    @Override
    public LicensePolicy createPolicy(String clientId, LicensePolicy policy) {
        if (policy.isShareAlike() && !policy.isDerivatives()) {
            throw new BadRequestException(
                    ErrorCode.INVALID_POLICY_COMBINATION,
                    "A policy requiring share-alike must allow derivatives");
        }

        if (repository.existsByClientIdAndPolicyIdAndVersion(
                clientId, policy.getPolicyId(), policy.getVersion())) {
            throw ConflictException.duplicatePolicyVersion(
                    policy.getPolicyId() + ":v" + policy.getVersion());
        }

        LicensePolicy clientPolicy = new LicensePolicy(
                clientId,
                policy.getPolicyId(),
                policy.getVersion(),
                policy.isCommercialUse(),
                policy.isRedistribution(),
                policy.isDerivatives(),
                policy.isAiTraining(),
                policy.isAttributionRequired(),
                policy.isShareAlike());

        return repository.save(clientPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LicensePolicy> findPolicyVersions(String clientId, String policyId) {
        List<LicensePolicy> policies =
                repository.findByClientIdAndPolicyIdOrderByVersionAsc(clientId, policyId);

        if (policies.isEmpty()) {
            throw NotFoundException.policy(policyId);
        }

        return policies;
    }

    @Override
    @Transactional(readOnly = true)
    public LicensePolicy findPolicyVersion(String clientId, PolicyRef ref) {
        return repository.findByClientIdAndPolicyIdAndVersion(
                        clientId, ref.policyId(), ref.version())
                .orElseThrow(() -> NotFoundException.policy(ref.toString()));
    }
}
