package com.licensegraph.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.licensegraph.common.PolicyRef;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyLookupImplTest {

    @Mock
    private LicensePolicyRepository repository;

    @InjectMocks
    private PolicyLookupImpl policyLookup;

    @Test
    void findPolicyWhenPresentReturnsPolicyView() {
        PolicyRef ref = new PolicyRef("cc-by", 2);

        LicensePolicy policy = new LicensePolicy(
                "client-1",
                "cc-by",
                2,
                true,
                true,
                true,
                false,
                true,
                false);

        when(repository.findByClientIdAndPolicyIdAndVersion(
                "client-1", "cc-by", 2))
                .thenReturn(Optional.of(policy));

        Optional<PolicyView> result =
                policyLookup.findPolicy("client-1", ref);

        assertTrue(result.isPresent());

        PolicyView view = result.orElseThrow();
        assertEquals(ref, view.ref());
        assertTrue(view.commercialUse());
        assertTrue(view.redistribution());
        assertTrue(view.derivatives());
        assertEquals(false, view.aiTraining());
        assertTrue(view.attributionRequired());
        assertEquals(false, view.shareAlike());

        verify(repository).findByClientIdAndPolicyIdAndVersion(
                "client-1", "cc-by", 2);
    }

    @Test
    void findPolicyWhenMissingReturnsEmpty() {
        PolicyRef ref = new PolicyRef("unknown-policy", 1);

        when(repository.findByClientIdAndPolicyIdAndVersion(
                "client-1", "unknown-policy", 1))
                .thenReturn(Optional.empty());

        Optional<PolicyView> result =
                policyLookup.findPolicy("client-1", ref);

        assertFalse(result.isPresent());
    }


    @Test
    void findPolicyForDifferentClientReturnsEmpty() {
        PolicyRef ref = new PolicyRef("cc-by", 2);

        when(repository.findByClientIdAndPolicyIdAndVersion(
                "client-2", "cc-by", 2))
                .thenReturn(Optional.empty());

        Optional<PolicyView> result =
                policyLookup.findPolicy("client-2", ref);

        assertFalse(result.isPresent());

        verify(repository).findByClientIdAndPolicyIdAndVersion(
                "client-2", "cc-by", 2);
    }

}