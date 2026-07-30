package io.github.chasehuegel.skilling.engine.command;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.incendo.cloud.suggestion.Suggestion;
import java.util.List;

public final class SkillParser<C> implements ArgumentParser<C, String>, BlockingSuggestionProvider<C> {

    private final SkillManager skillManager;
    private final boolean acceptAll;

    public SkillParser(SkillManager skillManager) {
        this(skillManager, false);
    }

    public SkillParser(SkillManager skillManager, boolean acceptAll) {
        this.skillManager = skillManager;
        this.acceptAll = acceptAll;
    }

    @Override
    public ArgumentParseResult<String> parse(CommandContext<C> ctx, CommandInput input) {
        String value = input.readString();
        if (acceptAll && value.equalsIgnoreCase("all")) {
            return ArgumentParseResult.success("all");
        }
        SkillDefinition def = resolve(value);
        if (def != null) {
            return ArgumentParseResult.success(def.id());
        }
        return ArgumentParseResult.failure(new IllegalArgumentException("Unknown skill: " + value));
    }

    @Override
    public Iterable<Suggestion> suggestions(CommandContext<C> ctx, CommandInput input) {
        String prefix = input.peekString().toLowerCase();
        var stream = skillManager.getSkills().values().stream()
                .map(SkillDefinition::id);
        if (acceptAll) {
            stream = java.util.stream.Stream.concat(stream, java.util.stream.Stream.of("all"));
        }
        return stream
                .distinct()
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .map(Suggestion::suggestion)
                .toList();
    }

    private SkillDefinition resolve(String input) {
        SkillDefinition def = skillManager.getSkill(input);
        if (def == null) {
            def = skillManager.getSkills().values().stream()
                    .filter(s -> s.id().equalsIgnoreCase(input)
                            || (s.display() != null && s.display().name() != null
                            && s.display().name().equalsIgnoreCase(input)))
                    .findFirst()
                    .orElse(null);
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    public static <C> ParserDescriptor<C, String> skillParser(SkillManager skillManager) {
        return ParserDescriptor.of(new SkillParser<>(skillManager), (Class<String>) (Class<?>) String.class);
    }

    @SuppressWarnings("unchecked")
    public static <C> ParserDescriptor<C, String> skillParserAllowingAll(SkillManager skillManager) {
        return ParserDescriptor.of(new SkillParser<>(skillManager, true), (Class<String>) (Class<?>) String.class);
    }
}
