package com.licensegraph.common;

/**
 * One input of a derivation request: an asset version plus its relation to the output (Section 10).
 */
public record DerivationInput(AssetRef asset, RelationType relation) {
}
