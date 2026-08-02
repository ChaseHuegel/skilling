package io.github.chasehuegel.skilling.engine.registry;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the published registry surface: typed registration/creation,
 * fail-fast public no-arg constructor validation, immutable key snapshots, and
 * defensive copies of caller-supplied parameter lists.
 */
class RegistryApiTest {

    public static class ValidMechanic implements SkillMechanic {
        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            return true;
        }
    }

    public static class NoPublicNoArgMechanic implements SkillMechanic {
        private NoPublicNoArgMechanic() {}

        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            return true;
        }
    }

    public static class ValidTrigger implements SkillTrigger {
        @Override
        public String getKey() {
            return "test";
        }

        @Override
        public Class<? extends Event> getEventClass() {
            return Event.class;
        }
    }

    public static class ValidEvaluator implements ParameterEvaluator {
        @Override
        public double evaluate(int currentLevel, int unlockLevel) {
            return currentLevel * 10.0;
        }
    }

    @Test
    void registeringClassWithoutPublicNoArgConstructorFailsAtRegistration() {
        var reg = new MechanicRegistry();
        assertThrows(IllegalArgumentException.class,
                () -> reg.register("bad", NoPublicNoArgMechanic.class));
        assertTrue(reg.keys().isEmpty());
    }

    @Test
    void duplicateRegistrationFailsFast() {
        var reg = new MechanicRegistry();
        reg.register("a", ValidMechanic.class);
        assertThrows(IllegalArgumentException.class, () -> reg.register("a", ValidMechanic.class));
    }

    @Test
    void keysIsImmutableSnapshot() {
        var reg = new MechanicRegistry();
        reg.register("a", ValidMechanic.class);
        assertThrows(UnsupportedOperationException.class, reg.keys()::clear);
        assertTrue(reg.contains("a"));
    }

    @Test
    void callerParamNamesMutationDoesNotAffectRegistry() {
        var reg = new MechanicRegistry();
        List<String> callerList = new ArrayList<>(List.of("multiplier"));
        reg.register("a", ValidMechanic.class, callerList);
        callerList.add("injected");
        assertEquals(List.of("multiplier"), reg.getParameterNames("a"));
    }

    @Test
    void createReturnsTypedInstances() {
        var reg = new MechanicRegistry();
        reg.register("a", ValidMechanic.class);
        assertInstanceOf(ValidMechanic.class, reg.create("a"));
        assertNull(reg.create("unknown"));
    }

    @Test
    void triggerRegistryTypedRegistrationAndCreate() {
        var reg = new TriggerRegistry();
        reg.register("a", ValidTrigger.class);
        assertInstanceOf(ValidTrigger.class, reg.create("a"));
        assertNull(reg.create("unknown"));
    }

    @Test
    void evaluatorRegistrySupportsClassAndInstance() {
        var reg = new EvaluatorRegistry();
        reg.register("by_class", ValidEvaluator.class);
        ParameterEvaluator instance = (l, u) -> l * 2.0;
        reg.register("by_instance", instance);

        assertInstanceOf(ValidEvaluator.class, reg.create("by_class"));
        assertSame(instance, reg.create("by_instance"));
        assertEquals(200.0, reg.create("by_class").evaluate(20, 0), 1e-9);
    }

    @Test
    void evaluatorRegistryKeysAreImmutable() {
        var reg = new EvaluatorRegistry();
        reg.register("a", ValidEvaluator.class);
        assertThrows(UnsupportedOperationException.class, reg.keys()::clear);
        assertTrue(reg.contains("a"));
    }
}
