package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import java.util.List;
import java.util.Map;

/**
 * An immutable record representing a fully parsed skill definition.
 *
 * <p>Constructed by SkillManager from YAML and contains all
 * resolved evaluators, mechanics, triggers, and configuration data
 * needed to execute the skill at runtime.
 *
 * @param id          unique skill identifier
 * @param maxLevel    maximum achievable level
 * @param display     UI display configuration
 * @param progression XP curve configuration
 * @param xpSources   list of XP-granting trigger definitions
 * @param abilities   list of ability definitions
 */
public record SkillDefinition(
        String id,
        int maxLevel,
        Display display,
        Progression progression,
        List<XpSource> xpSources,
        List<Ability> abilities
) {

    /**
     * UI display configuration for a skill.
     *
     * @param name            display name
     * @param icon            material string (e.g., "minecraft:iron_pickaxe")
     * @param customModelData custom model data for resource packs
     * @param color           BossBar color
     * @param style           BossBar style
     */
    public record Display(
            String name,
            String icon,
            int customModelData,
            String color,
            String style
    ) {}

    /**
     * XP progression curve configuration.
     *
     * @param curve    evaluator type name
     * @param baseXp   base XP for level 1
     * @param exponent curve exponent
     * @param evaluator the resolved progression evaluator
     */
    public record Progression(
            String curve,
            double baseXp,
            double exponent,
            ParameterEvaluator evaluator
    ) {}

    /**
     * An XP source binding a trigger to a reward evaluator with optional filters.
     *
     * @param trigger the trigger identifier
     * @param filters list of filter conditions
     * @param reward  the XP reward evaluator
     */
    public record XpSource(
            String trigger,
            List<Filter> filters,
            ParameterEvaluator reward
    ) {}

    /**
     * A filter condition for XP sources and mechanics.
     *
     * @param target material/tag filter string (e.g. {@code #c:ores} or {@code minecraft:stone})
     * @param state  player state condition (e.g. {@code is_sneaking})
     * @param tool   required held-item tag or material (e.g. {@code #minecraft:pickaxes})
     */
    public record Filter(
            String target,
            String state,
            String tool
    ) {}

    /**
     * A player ability with requirements, mechanics, and feedback.
     *
     * @param id           unique ability identifier
     * @param displayName  human-readable name
     * @param unlockLevel  level at which this ability is unlocked
     * @param display      UI lore configuration
     * @param requirements pre-execution requirements
     * @param onFailure    failure feedback overrides
     * @param mechanics    list of mechanic entries
     * @param feedback     success feedback configuration
     */
    public record Ability(
            String id,
            String displayName,
            int unlockLevel,
            AbilityDisplay display,
            Requirements requirements,
            OnFailure onFailure,
            List<MechanicEntry> mechanics,
            Feedback feedback
    ) {}

    /**
     * UI lore configuration for an ability.
     *
     * @param lore list of lore strings with {placeholder} tokens
     */
    public record AbilityDisplay(
            List<String> lore
    ) {}

    /**
     * Pre-execution requirements that gate ability activation.
     *
     * @param cooldown   cooldown in seconds between uses
     * @param state      list of required player states
     * @param items      list of item requirements
     * @param exhaustion exhaustion (hunger) requirement, null if not used
     */
    public record Requirements(
            double cooldown,
            List<String> state,
            List<ItemRequirement> items,
            Exhaustion exhaustion
    ) {
        public Requirements(double cooldown, List<String> state, List<ItemRequirement> items) {
            this(cooldown, state, items, null);
        }
    }

    /**
     * Exhaustion (hunger) cost requirement for ability activation.
     *
     * @param amount  hunger points to consume (0-20)
     * @param minimum minimum food level required to activate (0-20)
     */
    public record Exhaustion(
            double amount,
            double minimum
    ) {}

    /**
     * An item requirement for ability activation.
     *
     * @param action       "possession" or "cost"
     * @param tag          material or tag string
     * @param slot         inventory slot
     * @param amount       required/consumed quantity
     * @param itemCooldown visual cooldown in seconds
     */
    public record ItemRequirement(
            String action,
            String tag,
            String slot,
            int amount,
            double itemCooldown
    ) {}

    /**
     * Failure feedback overrides keyed by failure reason.
     *
     * @param reasons map of failure reason -> feedback configuration
     */
    public record OnFailure(
            Map<String, FailureFeedback> reasons
    ) {}

    /**
     * Feedback configuration for a specific failure reason.
     *
     * @param actionBar action bar message
     * @param sounds    list of sound configurations
     */
    public record FailureFeedback(
            String actionBar,
            List<Map<String, Object>> sounds
    ) {}

    /**
     * A single mechanic entry within an ability.
     *
     * @param type       the mechanic registry key
     * @param filters    list of filter conditions
     * @param parameters map of parameter name -> evaluator
     */
    public record MechanicEntry(
            String type,
            List<Filter> filters,
            Map<String, ParameterEvaluator> parameters
    ) {}

    /**
     * Success feedback configuration for an ability activation.
     *
     * @param actionBar whether to show action bar
     * @param chat      whether to show chat message
     * @param message   the message text
     * @param particles list of particle configurations
     * @param sounds    list of sound configurations
     */
    public record Feedback(
            boolean actionBar,
            boolean chat,
            String message,
            List<Map<String, Object>> particles,
            List<Map<String, Object>> sounds
    ) {}

    /**
     * Computes the level corresponding to the given raw XP for this skill's progression curve.
     *
     * @param xp the total raw XP
     * @return the computed level (0 to maxLevel)
     */
    public int getLevelForXp(long xp) {
        for (int level = 1; level <= maxLevel; level++) {
            double required = progression.evaluator().evaluate(level, 0);
            if (xp < (long) required) return level - 1;
        }
        return maxLevel;
    }
}