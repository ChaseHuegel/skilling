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

    public SkillParser(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @Override
    public ArgumentParseResult<String> parse(CommandContext<C> ctx, CommandInput input) {
        String value = input.readString();
        if (resolve(value) != null) {
            return ArgumentParseResult.success(value);
        }
        return ArgumentParseResult.failure(new IllegalArgumentException("Unknown skill: " + value));
    }

    @Override
    public Iterable<Suggestion> suggestions(CommandContext<C> ctx, CommandInput input) {
        String prefix = input.peekString();
        return skillManager.getSkills().values().stream()
                .map(def -> {
                    if (def.display() != null && def.display().name() != null) {
                        return def.display().name();
                    }
                    return def.id();
                })
                .filter(name -> name.startsWith(prefix))
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
}
