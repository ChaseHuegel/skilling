# Issue Resolution Workflow

Resolving issues is fully autonomous and implicit. Asking to "work the active sprint / milestone" or "resolve the backlog" invokes this workflow. No per-issue prompt is required. The subsystem rules for every path a ticket touches (via the `src/`, `skilling-api/`, `web/`, and `docs/` AGENTS files) apply on top of these steps.

## Scope Gating

- **Active Sprint / Current Milestone** items in `docs/issues/INDEX.md` are the default target whenever issue work is requested.
- **Backlog** items are worked ONLY when explicitly asked to work on the backlog.

## Per-Issue Sequence (one issue at a time, no parallelization)

1. **Load the ticket.** Open the `ISSUE-<n>.md` write-up referenced in `INDEX.md`. It is the authoritative spec (Context & User Story, Implementation Requirements, Technical Specifications, Verification & Definition of Done). Read the DOX chain for every path you expect to touch.
2. **Plan.** For any non-trivial ticket, write a brief plan before coding. Research freely (existing implementations, Paper/Vue APIs, subsystem AGENTS docs, `docs/dev/DESIGN.md` / `docs/dev/REQUIREMENTS.md`). Do not guess APIs.
3. **Build.** Make the change, then run `./gradlew build` (and `cd web/frontend && npm run build` for web-only changes). Fix all errors before continuing.
4. **Test.** Run `./gradlew test` (plus relevant web checks). Fix all failures. Do not block on flaky E2E infrastructure.
5. **Self-review loop.** Read `git diff`. Verify correctness, style, and subsystem conventions (thread safety, fail-fast parsing, ECS composition, inventory security, Vue Composition API). Cross-check EVERY checkbox in the ticket's Implementation Requirements and Verification & Definition of Done. Resolve anything you find. Fix, rebuild, and retest until the ticket is fully satisfied. If ambiguous, make your best effort from codebase patterns. Run the DOX "Update After Editing" pass for durable changes.
6. **Escalation.** If a hard blocker cannot be resolved or a required change is deemed out of scope, do NOT stall or ask. Create a follow-up backlog ticket (`docs/issues/ISSUE-<n>.md`, next free number, following the template in `docs/AGENTS.md`). Add it to the Backlog section of `INDEX.md`. Then complete and commit the current issue with a note cross-referencing the follow-up.
7. **Mark complete.** Check off the satisfied checkboxes in the ticket AND flip the `INDEX.md` bullet to `[x]`. Mark complete only when the Definition of Done is genuinely met.
8. **Commit.** `git add -A && git commit -m "<type>: ..."` following `docs/dev/CONVENTIONS-COMMITS.md`. One commit per issue.
9. **Next issue.** Repeat from step 1 for the next unchecked item in the requested scope.

## Validation Gate

After each development phase, the applicable build and test commands must pass before proceeding:

- `./gradlew build`
- `./gradlew test`
- `cd web/frontend && npm run build` (for web-only changes)
