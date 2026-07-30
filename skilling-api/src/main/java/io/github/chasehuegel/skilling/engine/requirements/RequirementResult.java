package io.github.chasehuegel.skilling.engine.requirements;

import java.util.Map;

/**
 * The result of a requirement check, following the Result Object pattern.
 *
 * <p>Contains the success state, an optional {@link FailureReason}, and
 * a map of dynamic placeholders (e.g., remaining cooldown time) for
 * use in failure feedback messages.
 *
 * @param success      whether all requirements passed
 * @param failureReason the reason for failure, or null if successful
 * @param placeholders dynamic values for feedback message interpolation
 */
public record RequirementResult(
        boolean success,
        FailureReason failureReason,
        Map<String, String> placeholders
) {

    /** Successful result with no failure reason. */
    public static final RequirementResult PASSED = new RequirementResult(true, null, Map.of());

    /**
     * Creates a failed result with the given reason and placeholders.
     *
     * @param reason       the failure reason
     * @param placeholders placeholders for feedback messages
     * @return a new failed result
     */
    public static RequirementResult failed(FailureReason reason, Map<String, String> placeholders) {
        return new RequirementResult(false, reason, placeholders);
    }
}