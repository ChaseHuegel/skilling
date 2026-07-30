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
 * Cloud argument parser for {@code /skills log} type argument with tab completion.
 *
 * <p>Validates that the type is one of the known log types: xp, levels, unlocks, abilities.
 * Provides tab completion for all known types, filtering by the current input prefix.
 */
public final class LogTypeParser<C> implements ArgumentParser<C, String>, BlockingSuggestionProvider<C> {

    private static final List<String> LOG_TYPES = List.of("xp", "levels", "unlocks", "abilities");

    @Override
    public ArgumentParseResult<String> parse(CommandContext<C> ctx, CommandInput input) {
        String value = input.readString().toLowerCase();
        if (LOG_TYPES.contains(value)) {
            return ArgumentParseResult.success(value);
        }
        return ArgumentParseResult.failure(new IllegalArgumentException("Unknown log type: " + value
                + ". Valid types: " + String.join(", ", LOG_TYPES)));
    }

    @Override
    public Iterable<Suggestion> suggestions(CommandContext<C> ctx, CommandInput input) {
        String prefix = input.peekString().toLowerCase();
        return LOG_TYPES.stream()
                .filter(type -> type.startsWith(prefix))
                .map(Suggestion::suggestion)
                .toList();
    }

    @SuppressWarnings("unchecked")
    public static <C> ParserDescriptor<C, String> logTypeParser() {
        return ParserDescriptor.of(new LogTypeParser<>(), (Class<String>) (Class<?>) String.class);
    }
}
