## Active Sprint / Current Milestone

### Bugs
- [x] [ISSUE-149](ISSUE-149.md) - Validate `{id}` path params in `/api/skills/{id}` against path traversal
- [x] [ISSUE-159](ISSUE-159.md) - Fix stored XSS via unescaped `v-html` in the skill lore preview
- [x] [ISSUE-154](ISSUE-154.md) - Redact the web password from API responses and make web credential/port changes take effect
- [x] [ISSUE-155](ISSUE-155.md) - Harden web authentication (defaults, rate limiting, constant-time compare)
- [x] [ISSUE-111](ISSUE-111.md) - Fix `markSaved()` lost-update race that silently drops player XP
- [x] [ISSUE-112](ISSUE-112.md) - Make profile load/unload atomic on player reconnect to prevent XP loss
- [x] [ISSUE-143](ISSUE-143.md) - Consume `fanfare_pending` on profile load so offline admin XP/level changes get fanfare
- [x] [ISSUE-145](ISSUE-145.md) - Move all SQLite flushes/writes off the Bukkit main thread
- [x] [ISSUE-146](ISSUE-146.md) - Invalidate the page-inventory cache on every XP/level mutation path
- [x] [ISSUE-138](ISSUE-138.md) - Return `PlayerProfileView` from the public API instead of the mutable `PlayerProfile`
- [x] [ISSUE-116](ISSUE-116.md) - Move ability Check/Execute/Consume to per-ability in `fireAbilities`
- [x] [ISSUE-126](ISSUE-126.md) - Fix `TeleportMechanic` consuming cooldown/items when the teleport fails
- [x] [ISSUE-130](ISSUE-130.md) - Honor `slot` and `amount` in item requirements
- [x] [ISSUE-134](ISSUE-134.md) - Unify state-filter logic between the requirements engine and XP filters
- [x] [ISSUE-117](ISSUE-117.md) - Guard `ProjectileHitEvent` handler against non-`LivingEntity` hits
- [x] [ISSUE-114](ISSUE-114.md) - Fix 3x loot duplication in `YieldMultiplierMechanic`
- [x] [ISSUE-119](ISSUE-119.md) - Fix `AutoSmeltMechanic` destroying Fortune/Silk-Touch drops and inflating nugget ores
- [x] [ISSUE-115](ISSUE-115.md) - Fix 3x catch duplication in `FishingYieldMechanic`
- [x] [ISSUE-118](ISSUE-118.md) - Fix `ModifyFurnaceOutputMechanic` handing out furnace blocks instead of smelted product
- [x] [ISSUE-113](ISSUE-113.md) - Persist off-hand durability in `OffhandStrikeMechanic` and filter interact actions
- [x] [ISSUE-120](ISSUE-120.md) - Fix `XpBonusMechanic` permanent multiplier (static map never cleared)
- [x] [ISSUE-122](ISSUE-122.md) - Fix inverted probability in `ModifyTameChanceMechanic`
- [x] [ISSUE-125](ISSUE-125.md) - Fix `ModifyCraftOutputMechanic` shift-click craft duplication
- [x] [ISSUE-128](ISSUE-128.md) - Fix `ChainBreakMechanic` durability cost and pipeline re-dispatch
- [x] [ISSUE-127](ISSUE-127.md) - Cap `AreaHarvestMechanic` scan bounds and gate per-block breaking
- [x] [ISSUE-129](ISSUE-129.md) - Resolve projectile damagers in damage mechanics
- [ ] [ISSUE-174](ISSUE-174.md) - Rework `ProjectileReturnMechanic` — preserve projectile metadata, prevent duplication, support arrows
- [ ] [ISSUE-121](ISSUE-121.md) - Run damage-cancelling mechanics at an early priority instead of MONITOR
- [ ] [ISSUE-123](ISSUE-123.md) - Wire the `level_up` trigger to `SkillingLevelUpEvent` instead of vanilla level changes
- [ ] [ISSUE-124](ISSUE-124.md) - Dispatch `BrewEvent` and `PrepareAnvilEvent` so blocked mechanics become reachable
- [ ] [ISSUE-171](ISSUE-171.md) - Fix `resolveEventBulkScalar` — drop the `consume_item` scalar and add `craft_item` scaling
- [ ] [ISSUE-173](ISSUE-173.md) - Fix `resolveEventBulkScalar` — `furnace_extract` must scale by extracted item count, not dropped XP orbs
- [ ] [ISSUE-133](ISSUE-133.md) - Replace silent parsing failures with fail-fast errors in `SkillManager` and `CustomTagLoader`
- [ ] [ISSUE-132](ISSUE-132.md) - Fail-fast at load for unknown tags/materials instead of throwing inside event handlers
- [ ] [ISSUE-131](ISSUE-131.md) - Add fail-fast numeric validation to parameter evaluators
- [ ] [ISSUE-137](ISSUE-137.md) - Wire `EvaluatorRegistry` into parsing so custom evaluators actually work
- [ ] [ISSUE-139](ISSUE-139.md) - Make registries and the skill map thread-safe for reload
- [ ] [ISSUE-153](ISSUE-153.md) - Eliminate `ConcurrentModificationException`/torn reads on shared engine state during web reads
- [ ] [ISSUE-156](ISSUE-156.md) - Make `StagingManager` thread-safe with atomic writes and conflict-detection fixes
- [ ] [ISSUE-150](ISSUE-150.md) - Preserve backups after a reload instead of wiping them in `clear()`
- [ ] [ISSUE-151](ISSUE-151.md) - Make reload apply atomic and fail-safe (no permanent plugin freeze)
- [ ] [ISSUE-152](ISSUE-152.md) - Fail loudly (not silently) when a reload drops skills due to malformed YAML
- [ ] [ISSUE-142](ISSUE-142.md) - Fix `BossBarPool` LRU scope, locking, and non-applied config
- [ ] [ISSUE-141](ISSUE-141.md) - Clear `FeedbackDebouncer` and `BossBarPool` state on player quit
- [ ] [ISSUE-147](ISSUE-147.md) - Make `/skills set` configurable keys actually take effect at runtime
- [ ] [ISSUE-144](ISSUE-144.md) - Fix the `%skilling_total_levels%` placeholder (always returns "0")
- [ ] [ISSUE-148](ISSUE-148.md) - Fix UI navigation slot collision and close poison-pill vaporization gaps
- [ ] [ISSUE-157](ISSUE-157.md) - Fix `GuiLayout` round-trip data loss and add server-side validation
- [ ] [ISSUE-158](ISSUE-158.md) - Web error-handling hygiene (correct status codes, no internal message leakage)
- [ ] [ISSUE-163](ISSUE-163.md) - Implement the E2E "automatic mode" (server bootstrap + fixture seeding)
- [ ] [ISSUE-160](ISSUE-160.md) - Fix `TagsPage` deleting non-matching tags when editing under a search filter
- [ ] [ISSUE-161](ISSUE-161.md) - Never navigate away when a save fails (SkillEditor, Config, Tags, GuiLayout)
- [ ] [ISSUE-162](ISSUE-162.md) - Keep the Pinia auth store in sync with 401/session expiry
- [ ] [ISSUE-170](ISSUE-170.md) - Fix cooldown display on ability cards (row shows on all abilities and renders raw evaluator JSON)
- [ ] [ISSUE-172](ISSUE-172.md) - Follow-up to ISSUE-110 — sort the actual navigation flyout (topbar dropdown) by color then name

### Improvements

### Research

## Backlog

### Improvements
- [ ] [ISSUE-135](ISSUE-135.md) - Pre-flatten and cache tag/material resolution for O(1) event lookups
- [ ] [ISSUE-136](ISSUE-136.md) - Optimize `getLevelForXp` and remove per-ability/per-event recomputation
- [ ] [ISSUE-140](ISSUE-140.md) - Harden the registry API surface (typed generics, defensive copies, immutable views)
- [ ] [ISSUE-164](ISSUE-164.md) - Improve E2E isolation and make screenshot assertions real
- [ ] [ISSUE-165](ISSUE-165.md) - Replace index-keyed `v-for` in reorderable lists with stable IDs
- [ ] [ISSUE-166](ISSUE-166.md) - Remove dead stores/components and split the `AbilitiesSection` monolith
- [ ] [ISSUE-167](ISSUE-167.md) - Standardize all lore/description rendering on the escaping helper (no raw `v-html`)
- [ ] [ISSUE-168](ISSUE-168.md) - Add unit-test coverage for critical untested code paths
- [ ] [ISSUE-169](ISSUE-169.md) - Clean up test-quality issues (misnamed, no-op, duplicated, and guard-only tests)
- [ ] [ISSUE-176](ISSUE-176.md) - Add "bury bones" as a `player_interact` XP source and ability for the `piety` skill

### Research
- [ ] [ISSUE-175](ISSUE-175.md) - Research integrating the DualWield plugin API (ranull) for the `dual_wield` skill
- [ ] [ISSUE-177](ISSUE-177.md) - Research & design a plan to revise XP sources for all skills (multi-source, 50-hour-to-100 target)
