package io.github.chasehuegel.skilling.engine.ui.branding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders {@code branding} templates into legacy {@code &}-code strings and
 * Adventure {@link Component}s.
 *
 * <p>Two kinds of tokens are supported:
 * <ul>
 *   <li><b>Scalars</b> ({@code {level}}, {@code {bar}}, {@code {color}}, ...) replaced
 *       with a single string via the {@code scalars} map.</li>
 *   <li><b>Inserts</b> ({@code {lore}}, {@code {abilities}}, {@code {ability}}) replaced
 *       with a list of already-rendered lines. A template line containing an insert
 *       whose expansion is empty is dropped entirely, and each expansion line is
 *       spliced in place of the token.</li>
 * </ul>
 *
 * <p>Unresolved tokens are left verbatim, mirroring {@code LoreResolver}. Templates are
 * rendered verbatim: no spacing, separators, or styling is injected between lines.
 */
public final class TemplateRenderer {

    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    private TemplateRenderer() {}

    /**
     * Renders a list template, splicing insert expansions and substituting scalars.
     *
     * @param template the template lines
     * @param inserts  map of insert token name → already-rendered sub-lines
     * @param scalars  map of scalar token name → replacement string
     * @return the rendered legacy lines
     */
    public static List<String> renderLines(List<String> template,
                                           Map<String, List<String>> inserts,
                                           Map<String, String> scalars) {
        List<String> result = new ArrayList<>();
        for (String line : template) {
            result.addAll(expandLine(line, inserts, scalars));
        }
        return result;
    }

    /**
     * Renders a single template line with scalar substitution.
     *
     * @param line    the template line
     * @param scalars map of scalar token name → replacement string
     * @return the rendered legacy line
     */
    public static String renderLine(String line, Map<String, String> scalars) {
        Matcher matcher = TOKEN.matcher(line);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group(1);
            String replacement = scalars.get(token);
            if (replacement == null) {
                // Leave unresolved tokens verbatim so an admin typo is visible.
                continue;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Builds the XP bar string from a {@link BrandingConfig.BarTemplate}.
     *
     * @param bar         the bar template
     * @param width       effective width (clamped into the template's own range)
     * @param filledCount number of filled units (clamped to {@code [0, width]})
     * @return the rendered bar as a legacy string
     */
    public static String renderBar(BrandingConfig.BarTemplate bar, int width, int filledCount) {
        int effectiveWidth = Math.max(0, width);
        int filled = Math.clamp(filledCount, 0, effectiveWidth);
        StringBuilder sb = new StringBuilder(bar.start());
        for (int i = 0; i < filled; i++) {
            sb.append(bar.filled());
        }
        for (int i = filled; i < effectiveWidth; i++) {
            sb.append(bar.empty());
        }
        return sb.append(bar.end()).toString();
    }

    /**
     * Deserializes a legacy {@code &}-code line (including {@code &#rrggbb} hex).
     *
     * @param legacyLine the rendered legacy line
     * @return the Adventure component
     */
    public static Component toComponent(String legacyLine) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacyLine);
    }

    /**
     * Deserializes a list of legacy lines.
     *
     * @param lines the rendered legacy lines
     * @return the Adventure components
     */
    public static List<Component> toComponents(List<String> lines) {
        return lines.stream().map(TemplateRenderer::toComponent).toList();
    }

    private static List<String> expandLine(String line, Map<String, List<String>> inserts,
                                           Map<String, String> scalars) {
        Matcher matcher = TOKEN.matcher(line);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (inserts.containsKey(token)) {
                List<String> expansion = inserts.get(token);
                if (expansion.isEmpty()) {
                    return List.of();
                }
                String prefix = line.substring(0, matcher.start());
                String suffix = line.substring(matcher.end());
                List<String> out = new ArrayList<>(expansion.size());
                for (String sub : expansion) {
                    out.add(renderLine(prefix + sub + suffix, scalars));
                }
                return out;
            }
        }
        return List.of(renderLine(line, scalars));
    }
}
