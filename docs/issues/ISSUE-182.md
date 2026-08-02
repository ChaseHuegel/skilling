# ISSUE-182: Document that backwards compatibility is not a concern in agent guidelines

## Context & User Story
- **Goal:** As an agent working on Skilling, I want the guidelines to state explicitly that this project is green and breaking changes are allowed, so I never spend effort or design around backwards compatibility.
- **Agent Role:** You are an expert documentation engineer executing this task.

Skilling is a very green project with no production use. Schemas, APIs, and behaviors are all allowed to introduce breaking changes. The agent guidelines should say this explicitly so future changes and proposals don't unnecessarily preserve compatibility.

## Implementation Requirements
- [ ] Add an explicit note to the root `AGENTS.md` stating the project is greenfield / not in production and that breaking changes to schemas, APIs, and behaviors are acceptable (and that proposals need not worry about backwards compatibility).
- [ ] Place it where agents will see it early (e.g., root AGENTS.md Purpose / Local Contracts / Work Guidance).
- [ ] Reconcile with the existing `skilling-api` and web DOX docs so no child doc implies a backwards-compatibility requirement; update any child docs that contradict this.
- [ ] Consider a brief mention in `docs/dev/CONVENTIONS-COMMITS.md` (e.g., that breaking changes may use `!` commit markers) if consistent with the current commit spec.

## Technical Specifications & Context
- **Target Files:**
  - `AGENTS.md` (root)
  - `skilling-api/AGENTS.md` (check for compat language)
  - `docs/dev/CONVENTIONS-COMMITS.md` (optional)
- **Dependencies:** none.
- **Constraints:** Keep the note short and operational; do not duplicate it across many files.

## Verification & Definition of Done
- [ ] Root `AGENTS.md` contains an explicit "breaking changes are allowed / no backwards-compatibility concern" statement
- [ ] No child doc contradicts it (grep for "backwards"/"backward" compatibility language and remove/qualify stale statements)
- [ ] Docs render as clean Markdown
