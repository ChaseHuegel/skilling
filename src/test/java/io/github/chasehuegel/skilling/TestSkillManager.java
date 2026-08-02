package io.github.chasehuegel.skilling;

import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;

import java.util.function.Consumer;

/**
 * Builds a {@link SkillManager} with the built-in evaluators, mechanics, and
 * triggers registered, mirroring the production {@link Skilling#registerBuiltins}
 * so tests can parse skills that reference real mechanic types.
 */
public final class TestSkillManager {

    private TestSkillManager() {}

    public static SkillManager newBuiltIn() {
        return newWith(reg -> {});
    }

    public static SkillManager newWith(Consumer<MechanicRegistry> extraMechanics) {
        var evalReg = new EvaluatorRegistry();
        Skilling.registerBuiltinEvaluators(evalReg);
        var mechReg = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        extraMechanics.accept(mechReg);
        var trigReg = new TriggerRegistry();
        Skilling.registerBuiltinTriggers(trigReg);
        return new SkillManager(evalReg, mechReg, trigReg, new TagResolver(new CustomTagLoader()));
    }
}
