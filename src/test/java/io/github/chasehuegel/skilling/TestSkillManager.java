package io.github.chasehuegel.skilling;

import io.github.chasehuegel.skilling.engine.AbilityManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;

import java.io.File;
import java.util.function.Consumer;

/**
 * Builds a {@link SkillManager} with the built-in evaluators, mechanics, and
 * triggers registered, mirroring the production {@link Skilling#registerBuiltins}
 * so tests can parse skills that reference real mechanic types.
 *
 * <p>The bundled {@code abilities/} resources are preloaded from the classpath
 * into an {@link AbilityManager} so bundled skills that reference abilities by
 * id (e.g. {@code mining.yml} referencing {@code vein_miner}) still parse.
 */
public final class TestSkillManager {

    private TestSkillManager() {}

    public static SkillManager newBuiltIn() {
        return newWith(reg -> {});
    }

    public static SkillManager newWith(Consumer<MechanicRegistry> extraMechanics) {
        return newWith(extraMechanics, new TagResolver(new CustomTagLoader()));
    }

    public static SkillManager newWith(Consumer<MechanicRegistry> extraMechanics, TagResolver tagResolver) {
        var evalReg = new EvaluatorRegistry();
        Skilling.registerBuiltinEvaluators(evalReg);
        var mechReg = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        extraMechanics.accept(mechReg);
        var trigReg = new TriggerRegistry();
        Skilling.registerBuiltinTriggers(trigReg);
        var stateFilters = new io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(stateFilters, tagResolver,
                new io.github.chasehuegel.skilling.engine.tag.EntityTagResolver(new CustomTagLoader()));
        SkillManager manager = new SkillManager(evalReg, mechReg, trigReg, tagResolver, stateFilters);
        var abilityManager = new AbilityManager();
        abilityManager.loadAbilities(bundledAbilitiesDir());
        manager.setAbilityManager(abilityManager);
        return manager;
    }

    /**
     * Resolves the bundled {@code abilities/} directory from the classpath, or a
     * nonexistent directory (an empty registry) when the resources are absent.
     *
     * @return the bundled abilities directory
     */
    private static File bundledAbilitiesDir() {
        java.net.URL url = TestSkillManager.class.getClassLoader().getResource("abilities");
        if (url == null) return new File("/nonexistent/abilities");
        try {
            return new File(url.toURI());
        } catch (java.net.URISyntaxException e) {
            return new File("/nonexistent/abilities");
        }
    }
}
