## Active Sprint / Current Milestone

### Bugs
- [x] [ISSUE-229](ISSUE-229.md) - Bound the crowd_control mechanic's AoE radius [Severity: High]
- [x] [ISSUE-230](ISSUE-230.md) - Fix the PlaceholderAPI hook, which can never register (proxy over an abstract class) [Severity: Critical]
- [x] [ISSUE-231](ISSUE-231.md) - Attribute-modifier refresh truncates the buff duration [Severity: High]
- [x] [ISSUE-232](ISSUE-232.md) - Bound and guard the knockback mechanic (unclamped impulse, radius, no friendly-fire check) [Severity: High]
- [x] [ISSUE-233](ISSUE-233.md) - OffhandStrikeMechanic can damage any living entity, including other players [Severity: High]
- [x] [ISSUE-234](ISSUE-234.md) - Enforce mechanic registered parameter lists at load (missing/string-valued params fail at runtime) [Severity: High]
- [x] [ISSUE-235](ISSUE-235.md) - Chain/level break off-by-one and durability waste on unbreakable blocks [Severity: High]
- [x] [ISSUE-236](ISSUE-236.md) - Validate XP-source trigger keys against the TriggerRegistry at load [Severity: Medium]
- [x] [ISSUE-237](ISSUE-237.md) - `%skilling_evaluator_%` placeholders break for skill IDs with underscores [Severity: Medium]
- [x] [ISSUE-238](ISSUE-238.md) - Fix the `linear` progression curve's level-1 threshold offset [Severity: Medium]
- [x] [ISSUE-239](ISSUE-239.md) - Close custom-tag fail-fast holes (unknown `#c:` refs and scalar tag values pass silently) [Severity: Medium]
- [ ] [ISSUE-240](ISSUE-240.md) - BossBarPool lifecycle — hide pooled bars on disable and document the main-thread requirement [Severity: Medium]
- [ ] [ISSUE-241](ISSUE-241.md) - Guard LevelUpDispatcher scheduled unlock tasks against a quitting player [Severity: Medium]
- [ ] [ISSUE-242](ISSUE-242.md) - `/skills set database.pool_size` reports success but has no effect until restart [Severity: Medium]
- [ ] [ISSUE-243](ISSUE-243.md) - Bound `max_level` to prevent huge threshold-table allocations [Severity: Medium]
- [ ] [ISSUE-244](ISSUE-244.md) - SkillsGuideBook recipe is not removed when the book is disabled [Severity: Medium]
- [ ] [ISSUE-245](ISSUE-245.md) - `bossbar.max_active: 0` should disable the bar, not degrade to a cap of 1 [Severity: Medium]
- [ ] [ISSUE-246](ISSUE-246.md) - `/skills reset` leaves a stale XP boss bar visible [Severity: Medium]
- [ ] [ISSUE-247](ISSUE-247.md) - ExecuteMechanic does not guarantee the kill and lacks target-state guards [Severity: Low]
- [ ] [ISSUE-248](ISSUE-248.md) - ModifyFurnaceOutputMechanic can create an oversized ItemStack on large multipliers [Severity: Low]
- [ ] [ISSUE-249](ISSUE-249.md) - LinearEvaluator does not validate finite base/step/min/max [Severity: Low]
- [ ] [ISSUE-250](ISSUE-250.md) - XpBonusMechanic duration overflow and zero-duration no-op [Severity: Low]
- [ ] [ISSUE-251](ISSUE-251.md) - LevelThresholds global lock on the hot level-lookup path [Severity: Low]
- [ ] [ISSUE-252](ISSUE-252.md) - Remove or fix dead `PolynomialEvaluator.xpToNextLevel` [Severity: Low]
- [ ] [ISSUE-253](ISSUE-253.md) - Harden `SkillManager.parseFeedback` boolean casts [Severity: Low]
- [ ] [ISSUE-254](ISSUE-254.md) - Rename `bStatsHook` to follow Java naming conventions [Severity: Low]
- [ ] [ISSUE-255](ISSUE-255.md) - Make FeedbackDebouncer check-then-act atomic [Severity: Low]
- [ ] [ISSUE-256](ISSUE-256.md) - Unify the duplicated ConstantEvaluator / ConstantValueEvaluator handling [Severity: Low]
- [ ] [ISSUE-257](ISSUE-257.md) - Fix trigger Javadocs that contradict the actual dispatch gating [Severity: Low]

### Improvements

### Research

## Backlog

### Bugs

### Improvements

### Research
