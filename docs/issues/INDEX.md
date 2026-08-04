## Active Sprint / Current Milestone

### Bugs
- [x] [ISSUE-248](ISSUE-248.md) - ModifyFurnaceOutputMechanic can create an oversized ItemStack on large multipliers [Severity: Low]
- [x] [ISSUE-249](ISSUE-249.md) - LinearEvaluator does not validate finite base/step/min/max [Severity: Low]
- [x] [ISSUE-250](ISSUE-250.md) - XpBonusMechanic duration overflow and zero-duration no-op [Severity: Low]
- [x] [ISSUE-251](ISSUE-251.md) - LevelThresholds global lock on the hot level-lookup path [Severity: Low]
- [x] [ISSUE-252](ISSUE-252.md) - Remove or fix dead `PolynomialEvaluator.xpToNextLevel` [Severity: Low]
- [x] [ISSUE-253](ISSUE-253.md) - Harden `SkillManager.parseFeedback` boolean casts [Severity: Low]
- [x] [ISSUE-254](ISSUE-254.md) - Rename `bStatsHook` to follow Java naming conventions [Severity: Low]
- [x] [ISSUE-255](ISSUE-255.md) - Make FeedbackDebouncer check-then-act atomic [Severity: Low]
- [x] [ISSUE-256](ISSUE-256.md) - Unify the duplicated ConstantEvaluator / ConstantValueEvaluator handling [Severity: Low]
- [x] [ISSUE-257](ISSUE-257.md) - Fix trigger Javadocs that contradict the actual dispatch gating [Severity: Low]
- [x] [ISSUE-258](ISSUE-258.md) - Empty feedback scalars cause NPEs that abort the whole ability dispatch [Severity: High]
- [x] [ISSUE-259](ISSUE-259.md) - Quit-flush hydration race silently loses XP on quit→immediate-rejoin [Severity: High]
- [x] [ISSUE-260](ISSUE-260.md) - Renaming a skill to an existing ID silently overwrites the live skill on Apply [Severity: High]
- [x] [ISSUE-261](ISSUE-261.md) - Transient attribute modifiers and PENDING_REMOVALS leak on disable/reload [Severity: Medium]
- [ ] [ISSUE-262](ISSUE-262.md) - CraftItemEvent null-recipe NPE in resolveEventMaterial [Severity: Medium]
- [ ] [ISSUE-263](ISSUE-263.md) - Unbounded chain_limit / max_blocks can freeze the main thread [Severity: Medium]
- [ ] [ISSUE-264](ISSUE-264.md) - onPrepareAnvil MONITOR handler is missing ignoreCancelled [Severity: Medium]
- [ ] [ISSUE-265](ISSUE-265.md) - Cooldown arithmetic overflow silently disables cooldowns [Severity: Medium]
- [ ] [ISSUE-266](ISSUE-266.md) - Apply-then-reload ordering leaves a false, permanent 409 conflict [Severity: Medium]
- [ ] [ISSUE-267](ISSUE-267.md) - Staged-skill validation races the reload rebuild, causing spurious 400s [Severity: Medium]
- [ ] [ISSUE-268](ISSUE-268.md) - ConfigHandler accepts untyped and out-of-range config values [Severity: Medium]
- [ ] [ISSUE-269](ISSUE-269.md) - Skill and GUI-layout editors show the live file, hiding staged edits after save [Severity: Medium]
- [ ] [ISSUE-270](ISSUE-270.md) - Bundled alchemy abilities pair brew_potion with modify_brew_time and never fire [Severity: Medium]

### Improvements

### Research

## Backlog

### Bugs

### Improvements

### Research
