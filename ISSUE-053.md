# `Registries.registerMechanic()` and `registerTrigger()` accept `Class<?>`, while `registerEvaluator()` accepts `Object` — inconsistent API

## Issue

The `Registries` API has inconsistent parameter types for registration:

```java
public void registerMechanic(String key, Class<?> clazz)    // takes Class
public void registerTrigger(String key, Class<?> clazz)     // takes Class
public void registerEvaluator(String key, Object evaluator) // takes Object (instance)
```

Mechanics and triggers are registered as `Class<?>` (the registry instantiates them via no-arg constructor), while evaluators are registered as instances. This inconsistency is confusing for addon developers registering custom components.

**ISSUES.md reference:** Line 309

## Root Cause

Mechanics and triggers use prototype-based registration (new instance per ability execution), while evaluators are stateless and can be shared. The API surfaces this implementation detail instead of hiding it.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `Registries.java` | `api/Registries.java` | 38-60 |

## Development Plan

### Step 1: Standardize on instance-based registration

For simplicity and consistency, change `registerMechanic()` and `registerTrigger()` to accept instances like `registerEvaluator()` does:

```java
// Remove Class-based overloads
public void registerMechanic(String key, SkillMechanic mechanic) {
    mechanicRegistry.register(key, mechanic);
}
public void registerTrigger(String key, SkillTrigger trigger) {
    triggerRegistry.register(key, trigger);
}
```

### Step 2: Update internal usage

`Skilling.registerBuiltins()` currently registers classes:
```java
mechReg.register("core:chain_break", ChainBreakMechanic.class, List.of("chain_limit", "exhaustion"));
```

This is a deeper change — the registry currently uses `Class` to instantiate new instances per execution. Changing to instance-based registration means either:
- Share one instance (must be thread-safe/reentrant, which the current `SkillMechanic` implementations may not be)
- Keep the `Class`-based approach internally but hide it behind the API

### Step 3: Better approach — hide internal detail

Keep `MechanicRegistry` and `TriggerRegistry` using `Class<?>` internally, but expose a type-safe API:

```java
public void registerMechanic(String key, Class<? extends SkillMechanic> clazz) {
    mechanicRegistry.register(key, clazz);
}
public void registerTrigger(String key, Class<? extends SkillTrigger> clazz) {
    triggerRegistry.register(key, clazz);
}
```

This adds compile-time type safety without changing the instantiation pattern.

### Step 4: Also provide instance convenience

For stateless mechanics/triggers (records like `DodgeMechanic`, `ApplyStatusMechanic`), allow instance registration:

```java
public void registerMechanic(String key, SkillMechanic instance) {
    mechanicRegistry.registerSingleton(key, instance);
}
```

## Self-Review

- The Class→instance inconsistency is a legitimate API quality issue
- Adding compile-time bounds (`Class<? extends SkillMechanic>`) is the minimal fix — no behavioral change
- Instance-based registration is a larger refactor that should be evaluated separately
- Recommend: fix the generics first (compile-time safety), then consider instance registration as a future enhancement
