
package com.licensegraph.policy;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AI-assisted: Provides database operations for client-specific license policies.
 */
public interface LicensePolicyRepository
        extends JpaRepository<LicensePolicy, Long> {

    Optional<LicensePolicy> findByClientIdAndPolicyIdAndVersion(
            String clientId,
            String policyId,
            int version);

    List<LicensePolicy> findByClientIdAndPolicyIdOrderByVersionAsc(
            String clientId,
            String policyId);

    boolean existsByClientIdAndPolicyIdAndVersion(
            String clientId,
            String policyId,
            int version);
}
