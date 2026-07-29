# `DatabaseManager` does not set `PRAGMA foreign_keys = ON`

## Issue

`DatabaseManager.initialize()` enables `PRAGMA journal_mode=WAL` (line 37) but does not set `PRAGMA foreign_keys = ON`. While the current schema has no foreign keys, future schema changes that add them would silently be ignored — foreign key violations would not cause errors, and cascading operations would not work.

**ISSUES.md reference:** Line 136

## Root Cause

SQLite requires an explicit `PRAGMA foreign_keys = ON` at the start of each connection — it's not enabled by default. Without this pragma, `REFERENCES` clauses in `CREATE TABLE` statements are parsed but ignored.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `DatabaseManager.java` | `engine/db/DatabaseManager.java` | 34-47 |

## Development Plan

### Step 1: Add `PRAGMA foreign_keys = ON`

In the `initialize()` method, after setting WAL mode, add:

```java
try (var stmt = conn.createStatement()) {
    stmt.execute("PRAGMA foreign_keys = ON;");
}
```

This should be done on every connection (set at the connection level, not the database level). Since HikariCP manages the connection pool, the pragma should ideally be set in the HikariConfig:

```java
hikariConfig.setConnectionInitSql("PRAGMA foreign_keys = ON;");
```

### Step 2: Verify

- The pragma is set on every new connection from the pool
- Future schema updates with foreign keys will be enforced

## Self-Review

- Proactive fix — no current impact but prevents silent future breakage
- Using `setConnectionInitSql` is the correct approach for connection pools
- The `PRAGMA` is connection-scoped in SQLite, so `setConnectionInitSql` ensures it's applied to every pooled connection
