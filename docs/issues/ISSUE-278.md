# ISSUE-278: Fix pre-existing ChainBreakMechanicTest budget failures

## Context & User Story
- **Goal:** As a maintainer, I want `./gradlew test` green so the validation gate blocks no unrelated work. Two `ChainBreakMechanicTest` cases fail on a clean `main` baseline (confirmed on `c1ee121e`, before any web E2E work): `chainLimitCountsOriginSoChainsOneFewer()` and `oversizedChainLimitIsClampedToCap()`. They fail against the chain-break budget behavior changed by the recent `chain_limit`/`max_blocks` clamp and origin-counting commits (`774753b0`, `0b97623f`, `c7c3a032`).
- **Agent Role:** You are an expert backend/QA engineer executing this task.

## Implementation Requirements
- [ ] Determine whether the failing tests encode the intended post-clamp contract or the mechanic code regressed relative to the tests.
- [ ] Make the two failing `ChainBreakMechanicTest` cases pass without weakening the budget enforcement (chain_limit still counts the origin, oversized budgets are still clamped).
- [ ] Run `./gradlew test` and confirm all 967+ tests pass.

## Technical Specifications & Context
- **Target Files:** `src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ChainBreakMechanicTest.java` (methods around lines 129 and 189) and the chain-break mechanic implementation it exercises.
- **Dependencies:** Recent chain-break changes: `c7c3a032` (optional `target` parameter), `774753b0` (clamp `chain_limit`/`max_blocks` budgets), `0b97623f` (origin counted against the limit).
- **Constraints:** Keep the engine behavior consistent with the intent of the recent commits; do not just delete the failing assertions.

## Verification & Definition of Done
- [ ] `./gradlew test` passes (no failures).
- [ ] The chain-break budget edge cases (origin counted, oversized limit clamped) remain covered by passing tests.
- [ ] If a test's expectation changed, the changed expectation matches the mechanic's documented contract.

Cross-referenced from `docs/issues/ISSUE-277.md` (web E2E flakes), which hit this pre-existing gate failure while verifying its web-only changes.
