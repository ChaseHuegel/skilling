package io.github.chasehuegel.skilling.engine.registry;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.mechanic.UnlockMechanic;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the hardened registry API surface: typed returns, fail-fast
 * registration (public no-arg constructor), immutable snapshot views, and
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

    public static class NoPublicNoArgTrigger implements SkillTrigger {
        private NoPublicNoArgTrigger() {}

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

    public static class UnlockMechanicStub implements UnlockMechanic {
        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            return true;
        }
    }

    public static class NoPublicNoArgEvaluator implements ParameterEvaluator {
        private NoPublicNoArgEvaluator() {}

        @Override
        public double evaluate(int currentLevel, int unlockLevel) {
            return currentLevel;
        }
    }

    @Test
    void registerClassWithoutPublicNoArgConstructorFailsAtRegistration() {
        var reg = new MechanicRegistry();
        assertThrows(IllegalArgumentException.class,
                () -> reg.register("bad", NoPublicNoArgMechanic.class));
        assertTrue(reg.keys().isEmpty(), "the failed registration must not be stored");
    }

    @Test
    void triggerRegistryRejectsMissingPublicNoArgConstructor() {
        var reg = new TriggerRegistry();
        assertThrows(IllegalArgumentException.class,
                () -> reg.register("bad", NoPublicNoArgTrigger.class));
        assertTrue(reg.keys().isEmpty());
    }

    @Test
    void evaluatorRegistryRejectsMissingPublicNoArgConstructor() {
        var reg = new EvaluatorRegistry();
        assertThrows(IllegalArgumentException.class,
                () -> reg.register("bad", NoPublicNoArgEvaluator.class));
        assertTrue(reg.keys().isEmpty());
    }

    @Test
    void duplicateRegistrationFailsFast() {
        var reg = new MechanicRegistry();
        reg.register("a", ValidMechanic.class);
        assertThrows(IllegalArgumentException.class, () -> reg.register("a", ValidMechanic.class));
    }

    @Test
    void keysIsImmutableAndDoesNotExposeLiveMap() {
        var reg = new MechanicRegistry();
        reg.register("a", ValidMechanic.class);

        var keys = reg.keys();
        assertThrows(UnsupportedOperationException.class, keys::clear);
        assertTrue(reg.contains("a"), "keys().clear() must not empty the registry");

        var triggerKeys = new TriggerRegistry();
        triggerKeys.register("a", ValidTrigger.class);
        var trigKeys = triggerKeys.keys();
        assertThrows(UnsupportedOperationException.class, trigKeys::clear);
        assertTrue(triggerKeys.contains("a"));
    }

    @Test
    void callerParamNamesMutationDoesNotAffectRegistry() {
        var reg = new MechanicRegistry();
        List<String> callerList = new ArrayList<>(List.of("multiplier"));
        reg.register("a", ValidMechanic.class, callerList);

        callerList.add("injected");
        callerList.set(0, "mutated");

        assertEquals(List.of("multiplier"), reg.getParameterNames("a"),
                "mutating the caller's list after registration must not change the registry");
    }

    @Test
    void getAllParameterNamesIsDeepImmutable() {
        var reg = new MechanicRegistry();
        reg.register("a", ValidMechanic.class, List.of("multiplier"));

        var snapshot = reg.getAllParameterNames();
        var inner = snapshot.get("a");
        assertThrows(UnsupportedOperationException.class, () -> inner.add("injected"));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertEquals(List.of("multiplier"), reg.getParameterNames("a"),
                "mutating the snapshot must not corrupt registry state");
    }

    @Test
    void createReturnsTypedInstances() {
        var reg = new MechanicRegistry();
        reg.register("a", ValidMechanic.class);
        var mechanic = reg.create("a");
        assertInstanceOf(ValidMechanic.class, mechanic);
        assertNull(reg.create("unknown"), "an unregistered key must return null");
    }

    @Test
    void isUnlockDetectsUnlockMechanicsOnly() {
        var reg = new MechanicRegistry();
        reg.register("plain", ValidMechanic.class);
        reg.register("unlock", UnlockMechanicStub.class);
        assertTrue(reg.isUnlock("unlock"), "an UnlockMechanic must be reported as an unlock");
        assertTrue(!reg.isUnlock("plain"), "a plain SkillMechanic must not be an unlock");
        assertTrue(!reg.isUnlock("unknown"), "an unregistered key must not be an unlock");
    }

    @Test
    void createReturnsAFreshInstancePerCall() {
        var reg = new MechanicRegistry();
        reg.register("a", ValidMechanic.class);
        var first = reg.create("a");
        var second = reg.create("a");
        assertTrue(first != second,
                "the cached constructor must still prototype-scope mechanics, not reuse an instance");
    }

    @Test
    void evaluatorRegistryTypedRegistrationAndCreate() {
        var reg = new EvaluatorRegistry();
        reg.register("valid_class", ValidEvaluator.class);
        ParameterEvaluator instance = (l, u) -> l * 2.0;
        reg.register("valid_instance", instance);

        assertInstanceOf(ValidEvaluator.class, reg.create("valid_class"));
        assertEquals(instance, reg.create("valid_instance"));
        assertEquals(200.0, reg.create("valid_class").evaluate(20, 0), 1e-9);
    }
}
