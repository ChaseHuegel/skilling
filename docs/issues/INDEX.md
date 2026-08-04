## Active Sprint / Current Milestone

### Bugs
- [x] [ISSUE-190](ISSUE-190.md) - Fix pre-existing Projectile test failures (mock eye location)
- [x] [ISSUE-192](ISSUE-192.md) - Define and enforce consistent cost/cooldown consumption for chance-based mechanics
- [x] [ISSUE-193](ISSUE-193.md) - Guard ThornsDamageMechanic against synchronous reflect recursion
- [x] [ISSUE-194](ISSUE-194.md) - Fail-fast validation of string mechanic parameters at load, not on the event path
- [x] [ISSUE-195](ISSUE-195.md) - Add a damager-role guard to ModifyDamageMechanic
- [x] [ISSUE-196](ISSUE-196.md) - Correct tool durability handling in block-break mechanics
- [x] [ISSUE-197](ISSUE-197.md) - Remove static mutable state and test-only hooks from break mechanics
- [x] [ISSUE-199](ISSUE-199.md) - Aura mechanics must not buff hostile mobs; fix AllyAuraMechanic radius-0 edge
- [x] [ISSUE-200](ISSUE-200.md) - Make ShieldDisableMechanic target semantics independent of trigger binding
- [x] [ISSUE-201](ISSUE-201.md) - Fix ModifyCraftOutputMechanic shift-click overflow dropping bonus items
- [x] [ISSUE-202](ISSUE-202.md) - Validate mechanic parameter bounds at parse time
- [x] [ISSUE-203](ISSUE-203.md) - Fix off-by-one in the exhaustion (hunger) requirement check
- [x] [ISSUE-205](ISSUE-205.md) - Gate ProjectileMechanic and TeleportMechanic on interact action
- [x] [ISSUE-206](ISSUE-206.md) - Fix AutoSmeltMechanic collapsing multiple drop types into one stack

### Improvements
- [x] [ISSUE-191](ISSUE-191.md) - Apply §4 constants and source set to the remaining multi-source skills
- [x] [ISSUE-198](ISSUE-198.md) - Complete mechanic Javadoc and fix registry-key documentation drift
- [x] [ISSUE-204](ISSUE-204.md) - Reduce per-event overhead on the ability/XP hot path
- [x] [ISSUE-207](ISSUE-207.md) - Consolidate the three identical damage-cancel mechanics

### Research

## Backlog

### Bugs
- [ ] [ISSUE-208](ISSUE-208.md) - Gate the `fishing` trigger on the caught-fish event state
- [ ] [ISSUE-209](ISSUE-209.md) - Fail-fast on scalar YAML values where an evaluator block is required
- [ ] [ISSUE-210](ISSUE-210.md) - Preserve the `entity_tags:` section when saving tags.yml via the web GUI
- [ ] [ISSUE-211](ISSUE-211.md) - Quit/reconnect race can evict the live profile of an online player
- [ ] [ISSUE-212](ISSUE-212.md) - DB hydration failure must not install an empty profile that overwrites persisted XP
- [ ] [ISSUE-213](ISSUE-213.md) - Isolate mechanic execution failures so a throwing mechanic cannot skip ability cost/consume
- [ ] [ISSUE-215](ISSUE-215.md) - Engine data-integrity hardening (cooldowns, offline grants, negative XP, DB init, flush interval)
- [ ] [ISSUE-216](ISSUE-216.md) - Mechanic safety (teleport, tool-break chains, radius bounds, kill attribution, durability, NPE)
- [ ] [ISSUE-217](ISSUE-217.md) - Reload atomicity and state-filter load validation
- [ ] [ISSUE-218](ISSUE-218.md) - Web GUI config/data-loss and auth hardening

### Improvements
- [ ] [ISSUE-214](ISSUE-214.md) - Reload and shutdown must not block the Bukkit main thread
- [ ] [ISSUE-219](ISSUE-219.md) - Hot-path logging and low-severity correctness polish

### Research
