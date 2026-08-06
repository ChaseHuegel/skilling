package io.github.chasehuegel.skilling.engine.mechanic;

/**
 * Marks a {@link SkillMechanic} as a persistent, one-time unlock effect.
 *
 * <p>Unlock mechanics grant permanent player state (for example a recipe book
 * recipe via {@code core:unlock_recipe}) and are intrinsically idempotent: a
 * second execution detects the state already exists and returns {@code false}
 * without doing work. The engine exploits this on the {@code level_up} trigger,
 * which re-fires on every later level-up, and at join/reload time so unlocks are
 * retroactive for players already past the milestone.
 *
 * <p>Because the engine reconciles unlock mechanics on player join and after
 * {@code /skills reload}, the {@code event} argument of {@link #execute} may be
 * {@code null}. Implementations must not assume the event is non-null.
 */
public interface UnlockMechanic extends SkillMechanic {
}
