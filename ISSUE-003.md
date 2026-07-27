# ISSUE-003: Static Global State in `ChainBreakMechanic`

## Description

Two `static` mutable fields in `ChainBreakMechanic` create global
state that is shared across all players on the server:

1. **`static boolean CHAINING`** — Prevents re-entrant chain breaks by
   rejecting all chain_break executions while any chain is in progress.
   This serializes ALL chain breaks across ALL players globally.
   If player A's chain takes 50ms, player B's chain_break during that
   window is silently dropped (returns `false`).

2. **`static Set<Location> PROCESSING`** — Prevents the same block
   from being processed twice during a chain. Being static, it
   accumulates locations from all players' chains. While the `try/finally`
   removes each location after processing, a crashing chain could leave
   stale entries, permanently blocking those blocks from ever being
   chain-broken.

## Proposed Solution

### Fix 1: Per-player `CHAINING` guard

Replace `static boolean CHAINING` with a `Set<UUID>` of players
currently in a chain break:

```java
private static final Set<UUID> CHAINING_PLAYERS = new HashSet<>();
```

- Check: `if (!CHAINING_PLAYERS.add(player.getUniqueId())) return false;`
- Cleanup: `CHAINING_PLAYERS.remove(player.getUniqueId())` in `finally`

This allows concurrent chain breaks from different players while
preventing re-entrance for the same player.

### Fix 2: Instance-level `PROCESSING` set

Move `PROCESSING` from `static` to an instance field, or make it
a `ThreadLocal<Set<Location>>`. Since all execution happens on the
main thread (Minecraft processes events sequentially per tick),
`ThreadLocal` is the simplest fix:

```java
private static final ThreadLocal<Set<Location>> PROCESSING =
    ThreadLocal.withInitial(HashSet::new);
```

Replace all `PROCESSING.add(loc)` / `PROCESSING.remove(loc)` /
`PROCESSING.contains(loc)` with `PROCESSING.get().add(loc)` etc.

## Steps

1. Replace `static boolean CHAINING` with `static final Set<UUID>`.
2. Replace `static final Set<Location> PROCESSING` with `ThreadLocal`.
3. Update all references in `execute()`.
4. Build, test, commit.

## Risk

Low. Correctness fix — no behavior change for single-player scenarios.
Multi-player scenarios gain concurrent chain-break support.
