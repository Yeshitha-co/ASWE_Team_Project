package com.licensegraph.policy;

import com.licensegraph.common.PolicyRef;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements read-only lookup of license policies.
 */
@Service
public class PolicyLookupImpl implements PolicyLookup {

    private final LicensePolicyRepository repository;

    public PolicyLookupImpl(LicensePolicyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyView> findPolicy(String clientId, PolicyRef ref) {
        return repository.findByClientIdAndPolicyIdAndVersion(
                        clientId, ref.policyId(), ref.version())
                .map(policy -> new PolicyView(
                        ref,
                        policy.isCommercialUse(),
                        policy.isRedistribution(),
                        policy.isDerivatives(),
                        policy.isAiTraining(),
                        policy.isAttributionRequired(),
                        policy.isShareAlike()));
    }
}
