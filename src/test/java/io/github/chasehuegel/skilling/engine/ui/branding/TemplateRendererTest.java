package io.github.chasehuegel.skilling.engine.ui.branding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateRendererTest {

    @Test
    void renderLineSubstitutesScalars() {
        String out = TemplateRenderer.renderLine("&aLevel {level} / {max_level}",
                Map.of("level", "5", "max_level", "100"));
        assertEquals("&aLevel 5 / 100", out);
    }

    @Test
    void renderLineLeavesUnknownTokensVerbatim() {
        String out = TemplateRenderer.renderLine("XP: {xp_into} / {bogus}",
                Map.of("xp_into", "10"));
        assertEquals("XP: 10 / {bogus}", out);
    }

    @Test
    void renderLineResetsColorCodes() {
        // The {reset} token is a scalar replacement, like any other.
        String out = TemplateRenderer.renderLine("{positive}Hi{reset}",
                Map.of("positive", "&a", "reset", "&r"));
        assertEquals("&aHi&r", out);
    }

    @Test
    void renderLinesSplicesInserts() {
        List<String> template = List.of("{lore}", "done");
        List<String> out = TemplateRenderer.renderLines(template,
                Map.of("lore", List.of("one", "two")), Map.of());
        assertEquals(List.of("one", "two", "done"), out);
    }

    @Test
    void emptyInsertDropsContainingLine() {
        List<String> template = List.of("header", "{lore}", "footer");
        List<String> out = TemplateRenderer.renderLines(template,
                Map.of("lore", List.of()), Map.of());
        assertEquals(List.of("header", "footer"), out);
    }

    @Test
    void scalarAndInsertMixInOneLine() {
        List<String> template = List.of("> {lore}");
        List<String> out = TemplateRenderer.renderLines(template,
                Map.of("lore", List.of("a", "b")), Map.of());
        assertEquals(List.of("> a", "> b"), out);
    }

    @Test
    void renderBarHonorsWidthAndChars() {
        var bar = new BrandingConfig.BarTemplate(5, "&aX", "&8-", "<", ">");
        assertEquals("<&aX&aX&8-&8-&8->", TemplateRenderer.renderBar(bar, 5, 2));
    }

    @Test
    void renderBarClampsFilledCount() {
        var bar = new BrandingConfig.BarTemplate(4, "&a#", "&8.", "[", "]");
        assertEquals("[&8.&8.&8.&8.]", TemplateRenderer.renderBar(bar, 4, -3));
        assertEquals("[&a#&a#&a#&a#]", TemplateRenderer.renderBar(bar, 4, 99));
    }

    @Test
    void renderBarZeroWidth() {
        var bar = new BrandingConfig.BarTemplate(0, "&aX", "&8-", "[", "]");
        assertEquals("[]", TemplateRenderer.renderBar(bar, 0, 0));
    }

    @Test
    void toComponentRendersLegacyColorCodes() {
        Component c = TemplateRenderer.toComponent("&aLevel 5 &7/ &f100");
        assertEquals("Level 5 / 100", strip(c));
    }

    @Test
    void toComponentRendersHexColorCodes() {
        Component c = TemplateRenderer.toComponent("&#ff8800Branded");
        assertEquals("Branded", strip(c));
    }

    @Test
    void toComponentsDeserializesEachLine() {
        List<Component> out = TemplateRenderer.toComponents(List.of("&aA", "&bB"));
        assertEquals(2, out.size());
        assertEquals("A", strip(out.get(0)));
        assertEquals("B", strip(out.get(1)));
    }

    private static String strip(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component).replaceAll("\u00a7.", "");
    }

    @Test
    void fullSkillTemplateWithAbilitiesAndBar() {
        var bar = new BrandingConfig.BarTemplate(20, "&a█", "&8█", "&7[", "&7]");
        String barString = TemplateRenderer.renderBar(bar, 20, 5);

        List<String> abilityBlock = TemplateRenderer.renderLines(
                List.of("{ability}", ""),
                Map.of("ability", List.of("&a✔ Mining &8· Active")),
                Map.of());
        List<String> lore = List.of("&7Lore line");

        List<String> out = TemplateRenderer.renderLines(
                List.of("{bar}", "{lore}", "", "{abilities}"),
                Map.of("lore", lore, "abilities", abilityBlock),
                Map.of("bar", barString));

        assertEquals(List.of(barString, "&7Lore line", "", "&a✔ Mining &8· Active", ""), out);
    }
}
