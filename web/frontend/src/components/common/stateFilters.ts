/**
 * Shared state-filter suggestion list used by both FilterBuilder (mechanic
 * filters) and AbilitiesSection (requirement state chips). Keeping a single
 * source prevents the two lists from drifting.
 */
export const STATE_SUGGESTIONS: string[] = [
  'is_sneaking', 'is_sprinting', 'is_in_water', 'is_on_ground',
  'is_on_fire', 'is_riding',
  'player_placed:false', 'player_placed:true',
  'dimension:overworld', 'dimension:nether', 'dimension:end',
  'weather:clear', 'weather:rain', 'weather:thunder',
  'time:day', 'time:night',
  'light_level:below:7', 'light_level:above:7', 'light_level:exactly:0',
  'health:below:50%', 'health:above:75%',
  'hunger:below:6', 'hunger:above:15',
  'biome:minecraft:plains', 'biome:minecraft:forest',
  'target_type:#c:undead', 'target_type:minecraft:zombie',
  'hand:empty', 'offhand:empty', 'offhand:weapon',
  'equipped_all:#c:heavy_armor', 'equipped_all:#c:medium_armor',
  'equipped_all:#c:light_armor', 'equipped_all:#c:unarmored',
  'equipped_any:#c:heavy_armor', 'equipped_any:#c:medium_armor',
  'equipped_any:#c:light_armor',
  'is_blocking',
]

/**
 * Appends a {@code target_type:<entity>} suggestion for each known entity to
 * a state-filter suggestion list. The entity list carries both entity types
 * ({@code minecraft:zombie}) and entity tags ({@code #minecraft:zombies}), so
 * the target_type filter gets complete coverage. Duplicates are removed.
 *
 * @param stateSuggestions the base state-filter suggestions
 * @param entities the known entity types and entity tags
 * @return the merged, deduplicated suggestion list
 */
export function withTargetTypeSuggestions(stateSuggestions: string[], entities: string[]): string[] {
  const targetTypeSuggestions = entities.map(e => `target_type:${e}`)
  return Array.from(new Set([...stateSuggestions, ...targetTypeSuggestions]))
}
