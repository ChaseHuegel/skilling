package io.github.chasehuegel.skilling.engine.command;

import java.util.List;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.incendo.cloud.suggestion.Suggestion;

/**
 * Cloud argument parser for known config keys with tab completion.
 *
 * <p>Validates that the key exists in the loaded config. Provides tab completion
 * for all known config keys.
 */
public final class ConfigKeyParser<C> implements ArgumentParser<C, String>, BlockingSuggestionProvider<C> {

    private static final List<String> KNOWN_KEYS = List.of(
        "debug_logging",
        "titles.stay_duration",
        "global_xp_modifier",
        "database.pool_size",
        "bossbar.max_active",
        "bossbar.fade_ticks",
        "debouncer.interval_ms",
        "skills_guide_book.enabled"
    );

    @Override
    public ArgumentParseResult<String> parse(CommandContext<C> ctx, CommandInput input) {
        String value = input.readString();
        if (KNOWN_KEYS.contains(value)) {
            return ArgumentParseResult.success(value);
        }
        return ArgumentParseResult.failure(new IllegalArgumentException("Unknown config key: " + value
                + ". Known keys: " + String.join(", ", KNOWN_KEYS)));
    }

    @Override
    public Iterable<Suggestion> suggestions(CommandContext<C> ctx, CommandInput input) {
        String prefix = input.peekString().toLowerCase();
        return KNOWN_KEYS.stream()
                .filter(key -> key.toLowerCase().startsWith(prefix))
                .map(Suggestion::suggestion)
                .toList();
    }

    @SuppressWarnings("unchecked")
    public static <C> ParserDescriptor<C, String> configKeyParser() {
        return ParserDescriptor.of(new ConfigKeyParser<>(), (Class<String>) (Class<?>) String.class);
    }
}
