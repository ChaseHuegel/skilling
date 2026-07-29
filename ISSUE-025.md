# `PoisonPillTag.key` uses lazy initialization without `volatile` or synchronization

## Issue

`PoisonPillTag.getKey()` (line 14) uses double-checked lazy initialization without `volatile` or synchronization:

```java
private static NamespacedKey key;
private static NamespacedKey getKey() {
    if (key == null) {
        key = new NamespacedKey(Skilling.getInstance(), "ui_item");
    }
    return key;
}
```

This is thread-unsafe. If two threads call `getKey()` simultaneously before `key` is set, they may create two `NamespacedKey` instances, and the second thread may see a partially constructed object (though unlikely for `NamespacedKey`). Notably, `Skilling.getInstance()` is called inside the null check, which has its own initialization ordering dependency (see ISSUES.md line 209, already fixed).

**ISSUES.md reference:** Line 118

## Root Cause

The `key` field is not declared `volatile`, so there is no happens-before relationship between the write in one thread and the read in another. In practice this is unlikely to cause observable bugs (the `NamespacedKey` constructor is simple), but it violates safe publication patterns.

## Affected Files

| File | Path | Line |
|------|------|------|
| `PoisonPillTag.java` | `engine/ui/PoisonPillTag.java` | 10 (static field), 14-19 (getter) |

## Development Plan

### Step 1: Add `volatile` keyword

```java
private static volatile NamespacedKey key;
```

### Step 2: (Optional) Eager initialization

Since `NamespacedKey` is cheap to construct and this is called frequently (every UI item tag), move to eager initialization:

```java
private static final NamespacedKey KEY = new NamespacedKey(
    Skilling.getInstance(), "ui_item"
);
```

This eliminates the thread-safety concern entirely and simplifies the code. However, this requires `Skilling.getInstance()` to be available at class-load time, which was the original concern in ISSUES.md line 209. If that was fixed, eager init is safe.

### Step 3: Verify fixture

Check that no test accesses `PoisonPillTag` without a running server instance (which would fail eager init with a NullPointerException on `Skilling.getInstance()`).

## Self-Review

- Adding `volatile` is the minimal fix (1 keyword change)
- Eager initialization with `final` is preferred if the `Skilling.getInstance()` ordering is resolved
- The original ISSUES.md line 209 ("depends on `Skilling.getInstance()` at class-load time — fragile initialization order") suggests this has been identified and potentially fixed already
