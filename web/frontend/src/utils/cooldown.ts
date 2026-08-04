/**
 * Cooldown display helpers.
 *
 * The API returns `requirements.cooldown` as an evaluator object
 * (e.g. `{ "type": "constant", "params": { "value": 5 } }`) for every ability,
 * even ones with no cooldown at all (a constant `0`). These helpers normalize
 * that into numbers / display labels so the UI never renders raw JSON.
 */

export interface CooldownEvaluator {
    type: string;
    params: Record<string, any>;
}

export type CooldownInput = number | CooldownEvaluator | undefined | null;

/** Extracts the numeric value of a cooldown (constant evaluators and plain numbers). */
export function cooldownToNumber(cooldown: CooldownInput): number {
    if (typeof cooldown === 'number') return cooldown;
    if (cooldown && typeof cooldown === 'object') {
        const value = cooldown.params?.value;
        if (typeof value === 'number') return value;
    }
    return 0;
}

/**
 * Whether the cooldown is a dynamic (non-constant) evaluator such as a linear
 * or milestone block. Dynamic cooldowns must be preserved as evaluator objects
 * through the editor round-trip — collapsing them to a bare number would reset
 * them to `0` on save.
 */
export function isDynamicCooldown(cooldown: CooldownInput): cooldown is CooldownEvaluator {
    return !!cooldown && typeof cooldown === 'object' && cooldown.type !== 'constant';
}

/**
 * Returns a displayable cooldown string, or null when there is no cooldown to
 * show. Constant cooldowns below 1 are hidden; non-constant evaluators (linear,
 * milestones, ...) get a stable type label instead of raw JSON.
 */
export function cooldownLabel(cooldown: CooldownInput): string | null {
    if (typeof cooldown === 'number') {
        return cooldown > 0 ? `${cooldown}s` : null;
    }
    if (cooldown && typeof cooldown === 'object') {
        const value = cooldown.params?.value;
        if (typeof value === 'number') {
            return value > 0 ? `${value}s` : null;
        }
        if (cooldown.type && cooldown.type !== 'constant') {
            return `${cooldown.type} (dynamic)`;
        }
    }
    return null;
}
