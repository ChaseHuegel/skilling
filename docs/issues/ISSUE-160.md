# ISSUE-160: Fix `TagsPage` deleting non-matching tags when editing under a search filter

**Status:** Open
**Type:** Bug
**Severity:** High (data-loss: silent tag deletion on save)

---

## Context & User Story

- **Goal:** As an admin, I want to search tags and edit a visible tag without silently losing every tag that did not match the search.
- **Agent Role:** You are an expert frontend engineer executing this task.

## Implementation Requirements

- [ ] Make `TagListEditor` operate on the full tag set (or have the `filteredTags` setter merge edits back into the full set) so saving never drops non-matching tags
- [ ] Preserve add/remove/reorder semantics for both filtered and unfiltered views
- [ ] Add a test (unit or E2E) covering: search active → edit one tag → save → all tags still present

## Technical Specifications & Context

- **Target Files:**
  - `web/frontend/src/views/TagsPage.vue:98-115` (`filteredTags` getter/setter)
  - `web/frontend/src/components/tags/TagListEditor.vue`
- **Dependencies:** `saveTags()` (`TagsPage.vue:170-181`) persists the store state.
- **Constraints:** Do not change the search UX; the full set must remain the source of truth.

### Root Cause

When `searchQuery` is non-empty, the computed getter returns a filtered subset and `TagListEditor` operates on that subset. Its setter deletes **all** keys from `tags` and re-adds only the entries in the emitted value. Any edit while a search is active wipes every non-matching tag from state, and `saveTags()` persists the loss.

### Proposed Fix

Apply `TagListEditor` edits to the full tag set (diff against the filtered subset or merge by key), so filtering only affects display.

## Verification & Definition of Done

- [ ] `cd web/frontend && npm run build` passes
- [ ] Test: with a search active, editing one tag and saving preserves all other tags
- [ ] Manual smoke: filter, edit, save, clear filter, all tags intact
