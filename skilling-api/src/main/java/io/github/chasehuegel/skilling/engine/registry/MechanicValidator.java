package io.github.chasehuegel.skilling.engine.registry;

import java.util.Map;

/**
 * Validates a mechanic's constant-valued parameters at skill load time.
 *
 * <p>Mechanics that consume string-valued parameters (potion effects,
 * attributes, materials, particles) register a validator so a YAML typo is
 * rejected with a clear {@link IllegalArgumentException} while the skill file
 * is parsed — before it can throw inside an event handler mid-game.
 *
 * <p>The validator receives only the parameters that carry a constant value
 * (string or numeric). Level-scaled evaluator parameters are not included
 * because they cannot be resolved without a level context; validators skip
 * params absent from the map unless the param is required, in which case a
 * missing required param is rejected here so the failure happens at load.
 */
@FunctionalInterface
public interface MechanicValidator {

    /**
     * Validates the constant-valued parameters of a mechanic.
     *
     * @param context            human-readable load context (skill/ability) for error messages
     * @param constantParameters the constant-valued parameters (string or numeric constants)
     * @throws IllegalArgumentException if a parameter value is invalid
     */
    void validate(String context, Map<String, Object> constantParameters);
}
