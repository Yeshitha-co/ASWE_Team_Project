package com.licensegraph.policy;

import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.exception.NotFoundException;
import com.licensegraph.security.ClientContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles HTTP requests for creating and retrieving license policies.
 */
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;
    private final ClientContext clientContext;

    public PolicyController(
            PolicyService policyService,
            ClientContext clientContext) {
        this.policyService = policyService;
        this.clientContext = clientContext;
    }

    /**
     * Creates a license policy for the current client.
     *
     * @param request the validated policy creation request
     * @return the response representing the created policy
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyResponse createPolicy(
            @Valid @RequestBody CreatePolicyRequest request) {

        String clientId = clientContext.currentClientId();

        LicensePolicy policy = new LicensePolicy(
                clientId,
                request.policyId(),
                request.version(),
                request.commercialUse(),
                request.redistribution(),
                request.derivatives(),
                request.aiTraining(),
                request.attributionRequired(),
                request.shareAlike());

        LicensePolicy saved = policyService.createPolicy(clientId, policy);
        return PolicyResponse.from(saved);
    }

    /**
     * Retrieves all versions of a policy belonging to the current client.
     *
     * @param policyId the identifier of the policy
     * @return the available versions of the policy
     */
    @GetMapping("/{policyId}/versions")
    public List<PolicyResponse> findPolicyVersions(
            @PathVariable String policyId) {

        String clientId = clientContext.currentClientId();

        return policyService.findPolicyVersions(clientId, policyId)
                .stream()
                .map(PolicyResponse::from)
                .toList();
    }

    /**
     * Retrieves a specific version of a policy belonging to the current client.
     *
     * @param policyId the identifier of the policy
     * @param version the version number to retrieve
     * @return the requested policy version
     */
    @GetMapping("/{policyId}/versions/{version}")
    public PolicyResponse findPolicyVersion(
            @PathVariable String policyId,
            @PathVariable int version) {

        String clientId = clientContext.currentClientId();
        // A version below 1 can never exist, so it is not found (not a 500 from PolicyRef).
        if (version < 1) {
            throw NotFoundException.policy(policyId + ":v" + version);
        }
        PolicyRef ref = new PolicyRef(policyId, version);

        LicensePolicy policy = policyService.findPolicyVersion(clientId, ref);
        return PolicyResponse.from(policy);
    }
}