/**
 * Skill list ordering: color, then display name (falling back to id).
 * Shared by the dashboard, the GUI layout palette, and the topbar flyout so
 * the three views can never diverge.
 */
export interface SkillLike {
    id: string;
    displayName?: string;
    color?: string;
}

export function byColorThenName(a: SkillLike, b: SkillLike): number {
    const colorCmp = (a.color || '').localeCompare(b.color || '');
    if (colorCmp !== 0) return colorCmp;
    return (a.displayName || a.id || '').localeCompare(b.displayName || b.id || '');
}
