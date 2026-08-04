# ISSUE-257: Fix trigger Javadocs that contradict the actual dispatch gating

## Context & User Story
- **Goal:** As an engine maintainer, I want trigger documentation to match what the event listener actually dispatches so config authors are not misled.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Low (docs) — three trigger Javadocs describe broader gating than the listener implements:
  - `SneakTrigger`/`SprintTrigger` claim "starts or stops", but the listener only fires on start (`SkillEventListener.java:334`, `:341`).
  - `FishingTrigger` says "casts or reels in", but only `CAUGHT_FISH` fires (`SkillEventListener.java:295`).

## Implementation Requirements
- [x] Update the three trigger Javadocs to state the actual dispatch conditions (fire only on start; only on `CAUGHT_FISH`), matching the listener behavior.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/SneakTrigger.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/SprintTrigger.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/FishingTrigger.java`
- **Dependencies:** none.
- **Constraints:** Docs only — no behavior change. If `docs/users/capabilities.md` describes these triggers, align it too.

## Verification & Definition of Done
- [x] Javadocs match `SkillEventListener` dispatch gating.
- [x] `./gradlew build` and `./gradlew test` pass.
