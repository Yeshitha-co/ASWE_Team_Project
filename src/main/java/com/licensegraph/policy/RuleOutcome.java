package com.licensegraph.policy;

/**
 * Result of checking one policy against one intended use (Section 10, Module A).
 *
 * <p>{@code blockingField} names the policy field that forbids the use when {@code allowed} is false.
 */
public record RuleOutcome(boolean allowed, String blockingField) {
}
