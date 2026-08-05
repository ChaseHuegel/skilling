package io.github.chasehuegel.skilling.engine.ui.branding;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves a Minecraft color name or hex value into Adventure {@link TextColor}
 * objects and legacy {@code &} code strings for template injection.
 *
 * <p>Skill definitions carry their color as a free-form string (typically a
 * Bukkit {@code BarColor} name such as {@code GREEN} or {@code PINK}, but also
 * any {@link NamedTextColor} name or a {@code #rrggbb} hex value). This utility
 * maps those spellings to the two render forms the branding templates need.
 */
public final class SkillColorCode {

    private SkillColorCode() {}

    private static final Map<String, String> ALIASES = Map.of(
            "pink", "light_purple",
            "purple", "dark_purple",
            "magenta", "light_purple"
    );

    private static final Map<String, Character> CODE_BY_NAME = Map.ofEntries(
            Map.entry("black", '0'),
            Map.entry("dark_blue", '1'),
            Map.entry("dark_green", '2'),
            Map.entry("dark_aqua", '3'),
            Map.entry("dark_red", '4'),
            Map.entry("dark_purple", '5'),
            Map.entry("gold", '6'),
            Map.entry("gray", '7'),
            Map.entry("dark_gray", '8'),
            Map.entry("blue", '9'),
            Map.entry("green", 'a'),
            Map.entry("aqua", 'b'),
            Map.entry("red", 'c'),
            Map.entry("light_purple", 'd'),
            Map.entry("yellow", 'e'),
            Map.entry("white", 'f')
    );

    /**
     * Resolves a color name or {@code #rrggbb} hex string to a legacy
     * {@code &} code ({@code &a}, {@code &#rrggbb}).
     *
     * @param color the color string, may be null
     * @return the legacy code, or null when unresolvable
     */
    public static String toLegacyCode(String color) {
        String resolved = normalize(color);
        if (resolved == null) return null;
        if (resolved.startsWith("#")) {
            return hexCode(resolved);
        }
        Character code = CODE_BY_NAME.get(resolved);
        return code != null ? "&" + code : null;
    }

    /**
     * Resolves a color name or {@code #rrggbb} hex string to an Adventure
     * {@link TextColor}.
     *
     * @param color the color string, may be null
     * @return the color, or null when unresolvable
     */
    public static TextColor toTextColor(String color) {
        String resolved = normalize(color);
        if (resolved == null) return null;
        if (resolved.startsWith("#")) {
            TextColor hex = TextColor.fromHexString(resolved);
            if (hex == null) {
                throw new IllegalArgumentException("Invalid hex color: " + color);
            }
            return hex;
        }
        return NamedTextColor.NAMES.value(resolved);
    }

    private static String normalize(String color) {
        if (color == null || color.isBlank()) return null;
        String lower = color.toLowerCase(Locale.ROOT).replace(" ", "_");
        return ALIASES.getOrDefault(lower, lower);
    }

    private static String hexCode(String hex) {
        // Validate #rrggbb strictly so a malformed value never leaks into a
        // rendered string; the fail-fast contract wants a loud error at load.
        if (!hex.matches("^#[0-9a-f]{6}$")) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }
        return "&#" + hex.substring(1);
    }
}
