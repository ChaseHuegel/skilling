package io.github.chasehuegel.skilling.engine.command;

import org.bukkit.Bukkit;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.incendo.cloud.suggestion.Suggestion;
import java.util.List;

/**
 * Parser that accepts any string (supporting offline players) but provides
 * online player name suggestions for tab completion.
 */
public final class PlayerNameParser<C> implements ArgumentParser<C, String>, BlockingSuggestionProvider<C> {

    @Override
    public ArgumentParseResult<String> parse(CommandContext<C> ctx, CommandInput input) {
        return ArgumentParseResult.success(input.readString());
    }

    @Override
    public Iterable<Suggestion> suggestions(CommandContext<C> ctx, CommandInput input) {
        String prefix = input.peekString().toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(org.bukkit.entity.Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .map(Suggestion::suggestion)
                .toList();
    }

    @SuppressWarnings("unchecked")
    public static <C> ParserDescriptor<C, String> playerNameParser() {
        return ParserDescriptor.of(new PlayerNameParser<>(), (Class<String>) (Class<?>) String.class);
    }
}
