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
- [ ] [ISSUE-200](ISSUE-200.md) - Make ShieldDisableMechanic target semantics independent of trigger binding
- [ ] [ISSUE-201](ISSUE-201.md) - Fix ModifyCraftOutputMechanic shift-click overflow dropping bonus items
- [ ] [ISSUE-202](ISSUE-202.md) - Validate mechanic parameter bounds at parse time
- [ ] [ISSUE-203](ISSUE-203.md) - Fix off-by-one in the exhaustion (hunger) requirement check
- [ ] [ISSUE-205](ISSUE-205.md) - Gate ProjectileMechanic and TeleportMechanic on interact action
- [ ] [ISSUE-206](ISSUE-206.md) - Fix AutoSmeltMechanic collapsing multiple drop types into one stack

### Improvements
- [ ] [ISSUE-191](ISSUE-191.md) - Apply §4 constants and source set to the remaining multi-source skills
- [ ] [ISSUE-198](ISSUE-198.md) - Complete mechanic Javadoc and fix registry-key documentation drift
- [ ] [ISSUE-204](ISSUE-204.md) - Reduce per-event overhead on the ability/XP hot path
- [ ] [ISSUE-207](ISSUE-207.md) - Consolidate the three identical damage-cancel mechanics

### Research

## Backlog

### Bugs

### Improvements

### Research
