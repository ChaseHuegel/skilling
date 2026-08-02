package io.github.chasehuegel.skilling.api;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the published {@link Registries} container: it exposes all three
 * registries and routes typed registration calls to them.
 */
class RegistriesTest {

    public static class TestMechanic implements SkillMechanic {
        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            return true;
        }
    }

    public static class TestTrigger implements SkillTrigger {
        @Override
        public String getKey() {
            return "test";
        }

        @Override
        public Class<? extends Event> getEventClass() {
            return Event.class;
        }
    }

    public static class TestEvaluator implements ParameterEvaluator {
        @Override
        public double evaluate(int currentLevel, int unlockLevel) {
            return currentLevel;
        }
    }

    private Registries newContainer() {
        return new Registries(new MechanicRegistry(), new TriggerRegistry(), new EvaluatorRegistry());
    }

    @Test
    void containerExposesAllThreeRegistries() {
        var registries = newContainer();
        assertNotNull(registries.getMechanicRegistry());
        assertNotNull(registries.getTriggerRegistry());
        assertNotNull(registries.getEvaluatorRegistry());
    }

    @Test
    void registerMechanicStoresTypedEntry() {
        var registries = newContainer();
        registries.registerMechanic("test:mech", TestMechanic.class);
        assertTrue(registries.getMechanicRegistry().contains("test:mech"));
        assertInstanceOf(TestMechanic.class, registries.getMechanicRegistry().create("test:mech"));
    }

    @Test
    void registerTriggerStoresTypedEntry() {
        var registries = newContainer();
        registries.registerTrigger("test_trigger", TestTrigger.class);
        assertTrue(registries.getTriggerRegistry().contains("test_trigger"));
        assertInstanceOf(TestTrigger.class, registries.getTriggerRegistry().create("test_trigger"));
    }

    @Test
    void registerEvaluatorByClassCreatesFreshInstance() {
        var registries = newContainer();
        registries.registerEvaluator("test_eval", TestEvaluator.class);
        assertInstanceOf(TestEvaluator.class, registries.getEvaluatorRegistry().create("test_eval"));
    }

    @Test
    void registerEvaluatorByInstanceReturnsSameInstance() {
        var registries = newContainer();
        TestEvaluator instance = new TestEvaluator();
        registries.registerEvaluator("test_eval", instance);
        assertSame(instance, registries.getEvaluatorRegistry().create("test_eval"));
    }
}
