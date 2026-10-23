package com.licensegraph.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.licensegraph.common.PolicyRef;
import com.licensegraph.common.exception.ApiException;
import com.licensegraph.common.exception.ErrorCode;
import com.licensegraph.policy.PolicyLookup;
import com.licensegraph.policy.PolicyView;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * {@link AssetService} against the real H2 database, with Module A's {@link PolicyLookup}
 * mocked so that every policy version exists (Module B, tasks B3 and B4).
 *
 * <p>AI-assisted (Claude): tests drafted with Claude, reviewed by the Module B owner.
 */
@DataJpaTest
@Import(AssetService.class)
class AssetServiceIntegrationTest {

    @Autowired
    private AssetService service;

    @MockitoBean
    private PolicyLookup policyLookup;

    @BeforeEach
    void everyPolicyExists() {
        when(policyLookup.findPolicy(anyString(), any(PolicyRef.class))).thenAnswer(call -> Optional.of(
                new PolicyView(call.getArgument(1), true, true, true, true, false, false)));
    }

    @Test
    void oldVersionIsUnchangedAfterNewVersionWithAnotherPolicy() {
        service.register("client-1", new RegisterAssetRequest("dataset-A", 1, "dataset", "license-A", 1, "first"));
        service.register("client-1", new RegisterAssetRequest("dataset-A", 2, "dataset", "license-A", 2, "second"));

        AssetResponse v1 = service.get("client-1", "dataset-A", 1);
        AssetResponse v2 = service.get("client-1", "dataset-A", 2);

        assertThat(v1.policy()).isEqualTo("license-A:v1");
        assertThat(v1.metadata()).isEqualTo("first");
        assertThat(v2.policy()).isEqualTo("license-A:v2");
    }

    @Test
    void registeringSameVersionAgainKeepsOriginal() {
        service.register("client-1", new RegisterAssetRequest("dataset-A", 1, "dataset", "license-A", 1, "first"));

        assertThatThrownBy(() -> service.register("client-1",
                new RegisterAssetRequest("dataset-A", 1, "model", "license-B", 1, "second")))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo(ErrorCode.DUPLICATE_ASSET_VERSION));

        AssetResponse stored = service.get("client-1", "dataset-A", 1);
        assertThat(stored.policy()).isEqualTo("license-A:v1");
        assertThat(stored.metadata()).isEqualTo("first");
    }

    @Test
    void anotherClientsAssetGives404() {
        service.register("client-1", new RegisterAssetRequest("dataset-A", 1, "dataset", "license-A", 1, null));

        assertThatThrownBy(() -> service.get("client-2", "dataset-A", 1))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.code()).isEqualTo(ErrorCode.ASSET_NOT_FOUND);
                    assertThat(ex.status()).isEqualTo(404);
                });
    }

    @Test
    void anotherClientMayRegisterTheSameAssetVersion() {
        service.register("client-1", new RegisterAssetRequest("dataset-A", 1, "dataset", "license-A", 1, null));

        AssetResponse other = service.register("client-2",
                new RegisterAssetRequest("dataset-A", 1, "model", "license-B", 1, null));

        assertThat(other.policy()).isEqualTo("license-B:v1");
        assertThat(service.get("client-1", "dataset-A", 1).policy()).isEqualTo("license-A:v1");
    }
}
