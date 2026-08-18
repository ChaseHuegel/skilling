package io.github.chasehuegel.skilling.engine.mechanic;

/**
 * Optional marker for {@link SkillMechanic}s whose effect is probabilistic
 * (e.g. a dodge, block, or cancel that succeeds only when a chance roll wins).
 *
 * <p>The engine uses {@link #didProc()} to gate an ability's
 * {@code feedback.success_only} cues: success-gated feedback (action bar, chat,
 * sounds, particles) fires only when at least one executed proc-aware mechanic
 * actually produced its effect. Non-proc-aware mechanics are neutral in the
 * gate: they neither block nor force success feedback.
 *
 * <p>A mechanic must record the outcome of its mark during {@link #execute}
 * (mechanics are prototype-scoped, recreated per dispatch, so storing the
 * result on the instance is thread-safe) and report it from {@link #didProc()}.
 *
 * <p>YAML key: none. This is a behavior contract, not a config key.
 */
public interface ProcAwareMechanic extends SkillMechanic {

    /**
     * Whether the most recent execution of this mechanic actually succeeded.
     *
     * <p>Called after {@link #execute} by the engine's feedback gating. The
     * value is meaningful only for the immediately preceding execution of the
     * same instance.
     *
     * @return true if the mechanic's probabilistic effect fired
     */
    boolean didProc();
}