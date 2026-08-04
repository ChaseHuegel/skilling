## Active Sprint / Current Milestone

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
- [ ] [ISSUE-220](ISSUE-220.md) - Fix milestone evaluator round-trip between the web GUI and the engine
- [ ] [ISSUE-221](ISSUE-221.md) - Align web progression serialization with the engine's base_xp/exponent schema
- [ ] [ISSUE-222](ISSUE-222.md) - Web editor destroys non-scalar cooldown requirements
- [ ] [ISSUE-223](ISSUE-223.md) - Migrate sound/particle feedback identifiers from legacy enum names to modern 1.21 namespaced values
- [ ] [ISSUE-224](ISSUE-224.md) - Web GUI layout round-trip drops per-page `gui_title`
- [ ] [ISSUE-225](ISSUE-225.md) - Web gui-layout API must reject reserved navigation slots
- [ ] [ISSUE-226](ISSUE-226.md) - Web ability editor on_failure reason list is incomplete
- [ ] [ISSUE-227](ISSUE-227.md) - Refresh stale frontend fallback registries
- [ ] [ISSUE-228](ISSUE-228.md) - Web skill editor cannot represent addon-registered evaluator types

### Improvements
- [ ] [ISSUE-214](ISSUE-214.md) - Reload and shutdown must not block the Bukkit main thread
- [ ] [ISSUE-219](ISSUE-219.md) - Hot-path logging and low-severity correctness polish

### Research

## Backlog

### Bugs

### Improvements

### Research
