/**
 * Generates a short, collision-resistant client-side row key.
 *
 * These keys identify a row in reorderable lists so Vue can move the DOM node
 * (preserving focus and per-row state) instead of reusing it by position after a
 * drag reorder. Keys are client-only: they are assigned at row creation, carried
 * through edits via object spread, and stripped before the payload is sent to the
 * backend, so they never reach skill YAML.
 */
let seed = 0;

export function stableKey(): string {
    seed += 1;
    return `k${Date.now().toString(36)}${seed.toString(36)}${Math.random().toString(36).slice(2, 7)}`;
}
