package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code {placeholder}} tokens in UI lore strings by querying
 * the corresponding {@link ParameterEvaluator} and formatting the numeric
 * output.
 *
 * <p>Placeholder syntax: {@code {name}} where {@code name} maps to a
 * key in the evaluators map provided at resolution time.
 *
 * <p>Evaluator output is formatted with one decimal place
 * (e.g., {@code 12.5}) unless the value is a whole number, in which
 * case it is formatted as an integer (e.g., {@code 3}).
 */
public final class LoreResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    private LoreResolver() {}

    /**
     * Resolves all {@code {placeholder}} tokens in the given lore line
     * using the provided evaluators.
     *
     * @param line        the lore line containing placeholder tokens
     * @param evaluators  map of placeholder name → parameter evaluator
     * @param currentLevel the player's current level
     * @param unlockLevel  the ability's unlock level
     * @return the resolved lore line with placeholders replaced
     */
    public static String resolve(String line, Map<String, ParameterEvaluator> evaluators,
                                  int currentLevel, int unlockLevel) {
        if (line == null || line.isBlank()) return line;

        var matcher = PLACEHOLDER_PATTERN.matcher(line);
        var buffer = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            ParameterEvaluator evaluator = evaluators.get(placeholder);
            String replacement;

            if (evaluator != null) {
                if (evaluator instanceof io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator constant
                        && constant.stringValue() != null) {
                    replacement = constant.stringValue();
                } else {
                    double value = evaluator.evaluate(currentLevel, unlockLevel);
                    replacement = formatValue(value);
                }
            } else {
                var instance = io.github.chasehuegel.skilling.Skilling.getInstance();
                if (instance != null) {
                    instance.getLogger().warning("Unresolved lore placeholder: {" + placeholder + "}");
                }
                replacement = "{" + placeholder + "}";
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * Resolves all provided lore lines.
     *
     * @param lore         list of lore lines
     * @param evaluators   map of placeholder name → parameter evaluator
     * @param currentLevel the player's current level
     * @param unlockLevel  the ability's unlock level
     * @return list with resolved lore lines
     */
    public static java.util.List<String> resolveAll(java.util.List<String> lore,
                                                     Map<String, ParameterEvaluator> evaluators,
                                                     int currentLevel, int unlockLevel) {
        return lore.stream()
                .map(line -> resolve(line, evaluators, currentLevel, unlockLevel))
                .toList();
    }

    private static String formatValue(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }
}