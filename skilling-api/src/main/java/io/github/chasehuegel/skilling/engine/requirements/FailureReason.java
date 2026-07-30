package io.github.chasehuegel.skilling.engine.requirements;

/**
 * Enumeration of possible failure reasons for a requirement check.
 *
 * <p>Each reason maps to a key in the ability's {@code on_failure}
 * YAML section for custom feedback messages.
 */
public enum FailureReason {
    COOLDOWN,
    MISSING_ITEM,
    MISSING_STATE,
    INSUFFICIENT_ITEMS,
    UNKNOWN
}