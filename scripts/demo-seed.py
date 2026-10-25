
"""
LicenseGraph Demo Seed Script
=============================

Purpose:
    Demonstrate the main LicenseGraph API features by registering clients,
    creating license policies and assets, building lineage relationships,
    and checking license compliance.

What this script does:
    1. Registers a client and obtains an API key for authenticated requests.
    2. Creates three license policies with different usage restrictions.
    3. Registers a source dataset and two derived models.
    4. Connects the assets to form a lineage graph.
    5. Verifies ancestor and descendant queries.
    6. Tests that the API detects and rejects a cyclic lineage relationship.
    7. Checks that commercial use is denied when an inherited license
       prohibits it, and verifies the saved decision and obligations.
    8. Verifies client isolation and rejects unauthenticated requests.

Requirements:
    - The LicenseGraph Spring Boot application must be running.
    - The API must be accessible at http://localhost:8080.
    - Python 3 must be installed.

Usage:
    Run this script from the project root:

        python ./scripts/demo-seed.py

Expected result:
    Each stage prints PASS when its checks succeed. The script finishes
    with a success message if all eight stages complete successfully.

"""

import json
import urllib.error
import urllib.request

BASE_URL = "http://localhost:8080"


def api_request(method, path, api_key=None, body=None, expected_status=200):
    """Send an HTTP request and validate its status code."""
    url = BASE_URL + path

    headers = {"Content-Type": "application/json"}

    if api_key:
        headers["X-API-Key"] = api_key

    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")

    request = urllib.request.Request(
        url=url,
        data=data,
        headers=headers,
        method=method,
    )

    try:
        with urllib.request.urlopen(request) as response:
            status = response.status
            response_body = response.read().decode("utf-8")

    except urllib.error.HTTPError as error:
        status = error.code
        response_body = error.read().decode("utf-8")

    except urllib.error.URLError as error:
        raise RuntimeError(
            f"Could not connect to {url}. "
            "Is the Spring Boot application running?"
        ) from error

    if status != expected_status:
        raise RuntimeError(
            f"{method} {path}: expected HTTP {expected_status}, "
            f"received HTTP {status}. Response: {response_body}"
        )

    if not response_body.strip():
        return None

    try:
        return json.loads(response_body)
    except json.JSONDecodeError:
        return response_body


def check(condition, message):
    """Stop the demo if an expected result is not satisfied."""
    if not condition:
        raise RuntimeError(f"CHECK FAILED: {message}")

    print(f"[PASS] {message}")


if __name__ == "__main__":
    print("LicenseGraph demo seed")
    print(f"API URL: {BASE_URL}")

    # Step 1: Register the primary client.
    # Creates a client account and obtains its API key.
    # The API key is used to authenticate subsequent requests.
    print("\n[1/8] Registering primary client...")

    client = api_request(
        method="POST",
        path="/api/clients",
        expected_status=201,
    )

    check(
        isinstance(client, dict)
        and bool(client.get("clientId"))
        and bool(client.get("apiKey")),
        "Client registered and API key received.",
        )

    client_id = client["clientId"]
    api_key = client["apiKey"]

    print(f"Client ID: {client_id}")
    print("API key captured (not displayed).")

    # Step 2: Create license policies.
    # Creates three policies with different permissions and restrictions.
    # These policies let us test how license rules affect derived assets.
    print("\n[2/8] Creating policies...")

    policies = [
        {
            "policyId": "license-A",
            "version": 1,
            "commercialUse": False,
            "redistribution": True,
            "derivatives": True,
            "aiTraining": True,
            "attributionRequired": True,
            "shareAlike": False,
        },
        {
            "policyId": "license-B",
            "version": 1,
            "commercialUse": True,
            "redistribution": True,
            "derivatives": True,
            "aiTraining": True,
            "attributionRequired": False,
            "shareAlike": False,
        },
        {
            "policyId": "license-C",
            "version": 1,
            "commercialUse": True,
            "redistribution": False,
            "derivatives": True,
            "aiTraining": True,
            "attributionRequired": False,
            "shareAlike": False,
        },
    ]

    for policy in policies:
        api_request(
            method="POST",
            path="/api/policies",
            api_key=api_key,
            body=policy,
            expected_status=201,
        )

        print(f"[PASS] Created policy {policy['policyId']}:v1")

    # Step 3: Create assets.
    # Registers one source dataset and two models derived from it.
    # Each asset is associated with a license policy and version.
    print("\n[3/8] Creating assets...")

    assets = [
        {
            "assetId": "Dataset-A",
            "version": 1,
            "type": "dataset",
            "policyId": "license-A",
            "policyVersion": 1,
            "metadata": "Source dataset",
        },
        {
            "assetId": "Model-B",
            "version": 1,
            "type": "model",
            "policyId": "license-B",
            "policyVersion": 1,
            "metadata": "Trained model",
        },
        {
            "assetId": "Model-C",
            "version": 1,
            "type": "model",
            "policyId": "license-C",
            "policyVersion": 1,
            "metadata": "Fine-tuned model",
        },
    ]

    for asset in assets:
        api_request(
            method="POST",
            path="/api/assets",
            api_key=api_key,
            body=asset,
            expected_status=201,
        )

        print(f"[PASS] Created asset {asset['assetId']}:v1")

    # Step 4: Create derivation relationships.
    # Connects Dataset-A to Model-B and Model-B to Model-C.
    # These relationships form the lineage graph used by later checks.
    print("\n[4/8] Creating derivation relationships...")

    derivations = [
        {
            "output": {"assetId": "Model-B", "version": 1},
            "inputs": [
                {
                    "assetId": "Dataset-A",
                    "version": 1,
                    "relation": "trained-on",
                }
            ],
        },
        {
            "output": {"assetId": "Model-C", "version": 1},
            "inputs": [
                {
                    "assetId": "Model-B",
                    "version": 1,
                    "relation": "fine-tuned-from",
                }
            ],
        },
    ]

    for derivation in derivations:
        api_request(
            method="POST",
            path="/api/derivations",
            api_key=api_key,
            body=derivation,
            expected_status=201,
        )

        output_id = derivation["output"]["assetId"]
        input_id = derivation["inputs"][0]["assetId"]
        print(f"[PASS] Created lineage: {output_id} from {input_id}")

    # Step 5: Verify ancestors and descendants.
    # Checks that Model-C can trace its lineage back to Dataset-A.
    # Also checks that Dataset-A's descendants include both derived models.
    print("\n[5/8] Verifying ancestors and descendants...")

    ancestors = api_request(
        method="GET",
        path="/api/assets/Model-C/versions/1/ancestors",
        api_key=api_key,
    )

    ancestor_nodes = ancestors.get("nodes", [])

    check(
        "Dataset-A:v1" in ancestor_nodes
        and "Model-B:v1" in ancestor_nodes
        and "Model-C:v1" in ancestor_nodes,
        "Model-C ancestors include Dataset-A, Model-B, and Model-C.",
        )

    descendants = api_request(
        method="GET",
        path="/api/assets/Dataset-A/versions/1/descendants",
        api_key=api_key,
    )

    descendant_nodes = descendants.get("nodes", [])

    check(
        "Dataset-A:v1" in descendant_nodes
        and "Model-B:v1" in descendant_nodes
        and "Model-C:v1" in descendant_nodes,
        "Dataset-A descendants include Dataset-A, Model-B, and Model-C.",
        )

    # Step 6: Test cycle detection.
    # Attempts to create a relationship that would form a cycle.
    # Verifies that the API rejects the invalid relationship and that
    # the rejected relationship does not corrupt the existing lineage graph.
    print("\n[6/8] Testing cycle detection...")

    cycle_body = {
        "output": {"assetId": "Dataset-A", "version": 1},
        "inputs": [
            {
                "assetId": "Model-C",
                "version": 1,
                "relation": "derived-from",
            }
        ],
    }

    cycle_response = api_request(
        method="POST",
        path="/api/derivations",
        api_key=api_key,
        body=cycle_body,
        expected_status=409,
    )

    cycle_text = json.dumps(cycle_response)

    check(
        "CYCLE_DETECTED" in cycle_text,
        "Cycle creation was rejected with CYCLE_DETECTED.",
        )

    # Confirm the rejected relationship was not saved.
    dataset_ancestors = api_request(
        method="GET",
        path="/api/assets/Dataset-A/versions/1/ancestors",
        api_key=api_key,
    )

    dataset_ancestor_nodes = dataset_ancestors.get("nodes", [])

    check(
        "Model-C:v1" not in dataset_ancestor_nodes,
        "Rejected cycle did not add Model-C as a Dataset-A ancestor.",
        )

    # Step 7: Test commercial-use decisions.
    # Requests a commercial-use decision for Model-C.
    # Verifies that the inherited restriction from Dataset-A causes a DENY.
    # Checks the blocking policy, inherited obligation, and saved decision.
    print("\n[7/8] Testing commercial-use decision...")

    decision_response = api_request(
        method="POST",
        path="/api/use-decisions",
        api_key=api_key,
        body={
            "assetId": "Model-C",
            "version": 1,
            "intendedUse": "commercial",
        },
        expected_status=201,
    )

    check(
        isinstance(decision_response, dict)
        and bool(decision_response.get("decisionId")),
        "Commercial-use decision was created.",
        )

    decision_id = decision_response["decisionId"]
    result = decision_response.get("result", {})

    check(
        result.get("decision") == "DENY",
        "Commercial use is denied because of the inherited restriction.",
        )

    blocking_paths = result.get("blockingPaths", [])

    check(
        any(
            item.get("blockingPolicy") == "license-A:v1"
            and item.get("blockingField") == "commercialUse"
            for item in blocking_paths
        ),
        "license-A:v1 is identified as the blocking policy.",
    )

    obligations = result.get("obligations", [])

    check(
        any(
            item.get("source") == "Dataset-A:v1"
            and item.get("policy") == "license-A:v1"
            for item in obligations
        ),
        "Attribution obligation from Dataset-A is preserved.",
    )

    saved_decision = api_request(
        method="GET",
        path=f"/api/use-decisions/{decision_id}",
        api_key=api_key,
    )

    check(
        saved_decision.get("decisionId") == decision_id,
        "Saved decision can be retrieved.",
        )

    print(f"Decision ID: {decision_id}")
    print(f"Decision: {result.get('decision')}")

    # Step 8: Test client isolation and authentication.
    # Registers a second client with a separate API key.
    # Verifies that the second client cannot access the first client's asset
    # or decision, and that requests without an API key are rejected.
    print("\n[8/8] Testing client isolation and authentication...")

    second_client = api_request(
        method="POST",
        path="/api/clients",
        expected_status=201,
    )

    check(
        isinstance(second_client, dict)
        and bool(second_client.get("clientId"))
        and bool(second_client.get("apiKey")),
        "Second client registered.",
        )

    second_api_key = second_client["apiKey"]

    api_request(
        method="GET",
        path="/api/assets/Dataset-A/versions/1",
        api_key=second_api_key,
        expected_status=404,
    )

    check(
        True,
        "Second client cannot access the first client's asset.",
    )

    api_request(
        method="GET",
        path=f"/api/use-decisions/{decision_id}",
        api_key=second_api_key,
        expected_status=404,
    )

    check(
        True,
        "Second client cannot access the first client's decision.",
    )

    api_request(
        method="GET",
        path="/api/assets/Dataset-A/versions/1",
        expected_status=401,
    )

    check(
        True,
        "Unauthenticated asset request is rejected.",
    )

    print("\nAll eight demo stages completed successfully!")
