# `RideHorseTrigger` uses `PlayerInteractEntityEvent` — should be `VehicleMountEvent`

## Issue

`RideHorseTrigger.getEventClass()` returns `PlayerInteractEntityEvent` (line 16). This event fires on ANY right-click interaction with any entity (villagers, item frames, armor stands, etc.), not just mounting a horse. The correct event is `VehicleMountEvent`, which fires specifically when a player mounts a vehicle.

**ISSUES.md reference:** Line 128

## Root Cause

`PlayerInteractEntityEvent` is a generic interaction event — every right-click on any entity triggers it. `VehicleMountEvent` (or `VehicleEnterEvent` on older versions) fires specifically when a player mounts an entity like a horse, boat, or minecart.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `RideHorseTrigger.java` | `engine/trigger/impl/RideHorseTrigger.java` | 16 |

## Development Plan

### Step 1: Change event class

```java
@Override
public Class<? extends Event> getEventClass() { return org.bukkit.event.vehicle.VehicleMountEvent.class; }
```

### Step 2: Update Javadoc

Fix the grammar and description: "Trigger fired when a player mounts a vehicle."

### Step 3: Verify

- Riding a horse should fire the trigger
- Right-clicking a villager or item frame should NOT fire the trigger

## Self-Review

- `VehicleMountEvent` is available in Paper/Bukkit API
- Corrects false positives from generic entity interaction
- One-line change plus Javadoc update
