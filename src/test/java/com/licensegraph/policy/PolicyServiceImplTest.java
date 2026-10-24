
package com.licensegraph.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.exception.BadRequestException;
import com.licensegraph.common.exception.ConflictException;
import com.licensegraph.common.exception.ErrorCode;
import com.licensegraph.common.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyServiceImplTest {

    @Mock
    private LicensePolicyRepository repository;

    @InjectMocks
    private PolicyServiceImpl service;

    @Test
    void createPolicyWithValidPolicySavesIt() {
        LicensePolicy policy = new LicensePolicy(
                "ignored-client",
                "cc-by",
                1,
                true,
                true,
                true,
                false,
                true,
                false);

        when(repository.existsByClientIdAndPolicyIdAndVersion(
                "client-1", "cc-by", 1)).thenReturn(false);

        when(repository.save(any(LicensePolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LicensePolicy saved = service.createPolicy("client-1", policy);

        assertEquals("client-1", saved.getClientId());
        assertEquals("cc-by", saved.getPolicyId());
        assertEquals(1, saved.getVersion());
        verify(repository).save(any(LicensePolicy.class));
    }

    @Test
    void createPolicyWithShareAlikeAndNoDerivativesThrowsBadRequest() {
        LicensePolicy policy = new LicensePolicy(
                "ignored-client",
                "invalid-policy",
                1,
                true,
                true,
                false,
                false,
                true,
                true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createPolicy("client-1", policy));

        assertEquals(ErrorCode.INVALID_POLICY_COMBINATION, exception.code());
        assertEquals(400, exception.status());
    }

    @Test
    void createPolicyWithDuplicateVersionThrowsConflict() {
        LicensePolicy policy = new LicensePolicy(
                "ignored-client",
                "cc-by",
                1,
                true,
                true,
                true,
                false,
                true,
                false);

        when(repository.existsByClientIdAndPolicyIdAndVersion(
                "client-1", "cc-by", 1)).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.createPolicy("client-1", policy));

        assertEquals(ErrorCode.DUPLICATE_POLICY_VERSION, exception.code());
        assertEquals(409, exception.status());

        verify(repository, never()).save(any(LicensePolicy.class));
    }

    @Test
    void findPolicyVersionWhenMissingThrowsNotFound() {
        PolicyRef ref = new PolicyRef("cc-by", 1);

        when(repository.findByClientIdAndPolicyIdAndVersion(
                "client-1", "cc-by", 1))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.findPolicyVersion("client-1", ref));

        assertEquals(ErrorCode.POLICY_NOT_FOUND, exception.code());
        assertEquals(404, exception.status());
    }

    /**
     * AI-assisted: Tests that finding policy versions returns them in ascending order.
     */
    @Test
    void findPolicyVersionsReturnsVersionsInAscendingOrder() {
        LicensePolicy version1 = new LicensePolicy(
                "client-1", "cc-by", 1,
                true, true, true, false, true, false);

        LicensePolicy version2 = new LicensePolicy(
                "client-1", "cc-by", 2,
                true, true, true, true, true, false);

        when(repository.findByClientIdAndPolicyIdOrderByVersionAsc(
                "client-1", "cc-by"))
                .thenReturn(List.of(version1, version2));

        List<LicensePolicy> versions =
                service.findPolicyVersions("client-1", "cc-by");

        assertEquals(2, versions.size());
        assertEquals(1, versions.get(0).getVersion());
        assertEquals(2, versions.get(1).getVersion());

        verify(repository).findByClientIdAndPolicyIdOrderByVersionAsc(
                "client-1", "cc-by");
    }

    @Test
    void findPolicyVersionsWhenNoneExistThrowsNotFound() {
        when(repository.findByClientIdAndPolicyIdOrderByVersionAsc(
                "client-1", "unknown-policy"))
                .thenReturn(java.util.Collections.emptyList());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> service.findPolicyVersions("client-1", "unknown-policy"));

        assertEquals(ErrorCode.POLICY_NOT_FOUND, exception.code());
        assertEquals(404, exception.status());
    }

    @Test
    void findPolicyVersionReturnsRequestedVersion() {
        PolicyRef ref = new PolicyRef("cc-by", 2);

        LicensePolicy version2 = new LicensePolicy(
                "client-1", "cc-by", 2,
                true, true, true, false, true, false);

        when(repository.findByClientIdAndPolicyIdAndVersion(
                "client-1", "cc-by", 2))
                .thenReturn(Optional.of(version2));

        LicensePolicy result = service.findPolicyVersion("client-1", ref);

        assertEquals("client-1", result.getClientId());
        assertEquals("cc-by", result.getPolicyId());
        assertEquals(2, result.getVersion());
    }

    /**
     * AI-assisted: Tests that creating a new version of a policy does
     * not affect the previous version.
     */
    @Test
    void creatingVersion2KeepsVersion1Readable() {
        LicensePolicy version1 = new LicensePolicy(
                "ignored-client", "cc-by", 1,
                true, true, true, false, true, false);

        LicensePolicy version2 = new LicensePolicy(
                "ignored-client", "cc-by", 2,
                true, true, true, true, true, false);

        when(repository.existsByClientIdAndPolicyIdAndVersion(
                "client-1", "cc-by", 2)).thenReturn(false);

        when(repository.save(any(LicensePolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createPolicy("client-1", version2);

        when(repository.findByClientIdAndPolicyIdAndVersion(
                "client-1", "cc-by", 1))
                .thenReturn(Optional.of(new LicensePolicy(
                        "client-1", "cc-by", 1,
                        true, true, true, false, true, false)));

        when(repository.findByClientIdAndPolicyIdAndVersion(
                "client-1", "cc-by", 2))
                .thenReturn(Optional.of(new LicensePolicy(
                        "client-1", "cc-by", 2,
                        true, true, true, true, true, false)));

        LicensePolicy retrievedV1 = service.findPolicyVersion(
                "client-1", new PolicyRef("cc-by", 1));

        LicensePolicy retrievedV2 = service.findPolicyVersion(
                "client-1", new PolicyRef("cc-by", 2));

        assertEquals(1, retrievedV1.getVersion());
        assertEquals(false, retrievedV1.isAiTraining());

        assertEquals(2, retrievedV2.getVersion());
        assertEquals(true, retrievedV2.isAiTraining());
    }

}