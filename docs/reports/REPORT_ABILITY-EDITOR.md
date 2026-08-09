# REPORT_ABILITY-EDITOR.md: Dedicated Editor for Standalone Base/Shared Abilities

**Status:** Research report (no production changes applied)
**Issue:** [ISSUE-275](../issues/ISSUE-275.md)
**Date:** 2026-08-08
**Inputs:** `web/frontend/src/views/AbilitiesPage.vue`, `web/frontend/src/views/SkillEditorPage.vue`, `web/frontend/src/components/skills/{AbilitiesSection,AbilityCard}.vue`, `web/frontend/src/api/client.ts`, `web/frontend/src/router.ts`, `web/frontend/src/components/layout/AppTopbar.vue`, `src/main/java/io/github/chasehuegel/skilling/engine/{AbilityManager,SkillManager}.java`, `src/main/java/io/github/chasehuegel/skilling/web/{WebServer,handler/SkillHandler,dto/SkillSerializer,staging/StagingManager}.java`, `src/main/resources/abilities/vein_miner.yml`, `docs/users/creating-skills.md`

---

## 1. Executive Summary

Skilling stores reusable abilities as standalone YAML files under `abilities/`.
Any skill references one by id. The skill's own fields overwrite the inherited
fields at load. Today the web GUI has no place to edit these shared files. The
`/abilities` page lists abilities read-only. The skill editor edits only the
skill's own yml and treats a reference as an override-only card.

This report proposes a dedicated list page and editor page for standalone
abilities. The two pages are distinct from the existing `/abilities` search
page. The plan reuses the skill editor's ability sub-editors where possible. It
adds a small backend API and staging support for `abilities/*.yml`.

The recommendation is a phased plan. The first phase adds the backend API and
the list page. The second phase adds the editor page. This order keeps each
phase shippable and testable alone.

---

## 2. Current State Map

### 2.1 The read-only abilities page

`web/frontend/src/views/AbilitiesPage.vue` fetches every skill summary, then
fetches each skill detail. It flattens all inline abilities into one grid. The
page has a search box and renders each entry with `AbilityCard.vue`.

`AbilityCard.vue` shows the display name, unlock level, id, cooldown, and
mechanic types. A click navigates to the owning skill's editor. The card is
read-only. It has no edit affordance for a standalone ability file.

Key limitation: the page shows abilities as they appear inside skills. It does
not show the standalone `abilities/` registry. It cannot create, edit, or
delete a shared ability.

### 2.2 The inline ability editor in the skill editor

`SkillEditorPage.vue` holds the whole skill form. `AbilitiesSection.vue` is the
ability list sub-editor inside it. It handles the identity fields, trigger,
lore, requirements, mechanics, feedback, and on-failure blocks. It reuses
`MechanicsEditor.vue`, `EvaluatorParameter.vue`, `SoundConfigEditor.vue`, and
`OnFailureEditor.vue`.

ISSUE-274 taught this editor to open a reference-shaped entry. A reference (an
`id` with no trigger) loads with a "Base/Shared" badge. The admin can override
individual fields. A no-op save keeps the bare reference. The editor never
writes to `abilities/`.

Key limitation: the skill editor cannot edit the shared definition itself. It
can only write per-skill overrides.

### 2.3 The abilities data-folder loader and merge engine

`AbilityManager` (`engine/AbilityManager.java`) scans the `abilities/` folder
recursively. Each `.yml` file registers one raw ability map keyed by `id`. On
an id conflict the first file wins and a warning is logged. A malformed file is
skipped with a warning. The registry stores raw maps only. Schema validation
happens later in `SkillManager`.

`SkillManager.parseAbilities` (`engine/SkillManager.java`) merges each skill
ability with the registered base. The merge is a top-level field overwrite.
There is no deep merge of nested structures. The merged map then parses through
the normal ability schema. The trigger, mechanics, requirements, and feedback
all come from the merged result.

Key fact: `AbilityManager` validates only shape plus an id. Full validation
happens when a skill references the id. A standalone file with a bad trigger or
an unknown mechanic would only fail when a skill uses it.

### 2.4 The web API surface

`WebServer.java` registers routes inline. Relevant routes:

| Method | Path | Handler | Notes |
|---|---|---|---|
| `GET` | `/api/skills` | `SkillHandler.list` | Skill summaries |
| `GET` | `/api/skills/{id}` | `SkillHandler.get` | Full skill detail |
| `POST` | `/api/skills` | `SkillHandler.create` | Staged create |
| `PUT` | `/api/skills/{id}` | `SkillHandler.update` | Staged update, rename support |
| `DELETE` | `/api/skills/{id}` | `SkillHandler.delete` | Staged delete |
| `GET` | `/api/abilities` | inline | Registered ability summaries |

`GET /api/abilities` returns each registered ability's id, display name,
trigger, and unlock level. ISSUE-274 added it. It feeds the skill editor's
reference validation.

`SkillSerializer.java` owns the skill DTO. `parseAbility` and `abilityToMap`
convert an ability between raw YAML and `SkillDetailDTO.AbilityDTO`. Both are
private and bound to the skill DTO. They already tolerate a reference-shaped
entry. The DTO carries the full ability surface, so it can model a standalone
file too.

`SkillHandler` validates a staged skill by parsing it through the live
`SkillManager` under the registry read lock (`validateStagedSkill`). This is the
fail-fast model to reuse for standalone abilities.

### 2.5 Staging support

`StagingManager` stages edits under `.web_staging/`. It knows these live paths:
`skills/*.yml`, `tags/base.yml`, `config.yml`, and `gui.yml`. It has no
`abilities/` handling. `applyAndBackup()` copies staged skills, tags, config,
and gui files into place. Deletions use marker files under
`deleted_skills/`.

Conflict detection snapshots live file fingerprints at stage time. It compares
them before apply and rejects with HTTP 409 on external modification. Backups
are created per apply under `.web_staging/backup/`.

To support standalone abilities, `StagingManager` needs new methods. It needs a
staged file path for `abilities/<id>.yml`, a stage method, a stage-deletion
method, and an apply path. `resolveLiveFile` needs an `abilities/` case. The
delete and apply loops need `abilities/` handling.

---

## 3. Proposed Scope

### 3.1 Two new pages, distinct from the search page

| Page | Route | Purpose |
|---|---|---|
| Ability list | `/abilities/manage` or `/base-abilities` | List registered standalone abilities, with add and edit entry points |
| Ability editor | `/base-abilities/:id` and `/base-abilities/new` | Create, edit, and delete one standalone ability |

The existing `/abilities` page stays as the cross-skill search view. The new
list page shows only the `abilities/` registry.

Recommended route names: `/base-abilities` and `/base-abilities/:id`. The name
matches the user-facing term "base/shared ability". It avoids confusion with
the existing `/abilities` search page.

### 3.2 Topbar navigation

Add one topbar link labeled "Base Abilities" pointing at the new list page.
Keep the existing "Abilities" link unchanged. The topbar already wraps on
narrow viewports, so the extra link does not cause overflow.

---

## 4. Proposed Backend API

### 4.1 New handler and routes

Create `web/handler/AbilityHandler.java`. It mirrors `SkillHandler` but works
against the `abilities/` data folder. Suggested routes:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/abilities` | List registered ability summaries (extend the current inline route) |
| `GET` | `/api/abilities/{id}` | Get one ability as a DTO |
| `POST` | `/api/abilities` | Create a standalone ability (staged) |
| `PUT` | `/api/abilities/{id}` | Update a standalone ability (staged) |
| `DELETE` | `/api/abilities/{id}` | Delete a standalone ability (staged) |

Move the current inline `GET /api/abilities` route into the new handler. Keep
the response shape. Add a `list` detail flag or a separate field so the editor
can fetch the full map when needed.

### 4.2 Ability DTO and serializer

Recommend extraction, not reuse of the private skill-bound methods.

Option A: Extract shared conversion into a new `AbilitySerializer`. It converts
an ability map to and from a standalone `AbilityDTO`. The skill serializer calls
it for the per-ability section. This removes duplication and keeps one source of
truth for the ability schema.

Option B: Keep `SkillSerializer.parseAbility`/`abilityToMap` private and write a
small standalone serializer. The skill DTO and the standalone DTO drift over
time.

Recommendation: Option A. Extract the ability-level conversion. The `AbilityDTO`
records map directly to the ability YAML keys. The standalone serializer wraps
them with id validation and registry-aware validation.

### 4.3 Id validation

Validate the ability id with the same pattern as skills:
`[a-z_][a-z0-9_]*`. Add a duplicate-id check against the live `AbilityManager`.
Reject a create that collides with a registered id. Reject a rename onto a
registered id.

### 4.4 Fail-fast validation of the yml

Model on `SkillHandler.validateStagedSkill`. Parse the staged ability YAML
through the live registries so an unknown trigger, mechanic, evaluator, or tag
fails at stage time.

New validation path: parse the ability through a registry-aware parser that
rejects unknown triggers and mechanics. Do not require a skill to reference the
id first. The parser must take the registry read lock, exactly as
`SkillHandler` does, so a concurrent reload cannot empty the registries
mid-parse.

### 4.5 Staging support

Add to `StagingManager`:

| Method | Purpose |
|---|---|
| `stagedAbilityFile(id)` | Return the staged `abilities/<id>.yml` path |
| `stageAbilityFile(id, yaml)` | Write a staged ability file and update status |
| `stageAbilityDeletion(id)` | Mark a staged deletion for an ability |
| apply path | Copy staged `abilities/*.yml` into place, back up first |
| delete path | Remove live `abilities/<id>.yml`, back up first |

Update `resolveLiveFile` with an `abilities/` case. The delete loop and apply
loop need the same treatment as skills. The conflict, backup, and status
behavior then work unchanged.

---

## 5. Proposed Frontend

### 5.1 API functions

Extend `api.abilities` in `web/frontend/src/api/client.ts`:

```ts
abilities: {
    list: () => apiFetch<{ abilities: AbilitySummary[] }>('/api/abilities'),
    get: (id: string) => apiFetch<AbilityDetail>(`/api/abilities/${id}`),
    create: (data: AbilityDetail) => apiFetch<any>('/api/abilities', { method: 'POST', body: JSON.stringify(data) }),
    update: (id: string, data: AbilityDetail) => apiFetch<any>(`/api/abilities/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id: string) => apiFetch<any>(`/api/abilities/${id}`, { method: 'DELETE' }),
},
```

The detail type mirrors `SkillDetailDTO.AbilityDTO`. It uses the same field
names as the skill editor's ability model.

### 5.2 List view

Create `web/frontend/src/views/BaseAbilitiesPage.vue`. It fetches
`api.abilities.list()`. It renders one card per ability. Reuse `AbilityCard.vue`
where the card shape fits. `AbilityCard` currently needs `skillId` and
`skillDisplayName`. For a standalone list, add optional props so the card can
render without a skill context, or wrap it in a thin list-item component.

Add a "New Ability" button next to the list header, mirroring the skill
dashboard.

### 5.3 Editor view

Create `web/frontend/src/views/BaseAbilityEditorPage.vue`. It mirrors
`SkillEditorPage.vue` but for a single ability.

The editor reuses the ability sub-editors from the skill editor. These are
`AbilitiesSection.vue`'s internals: identity fields, trigger combobox, lore,
requirements, mechanics, feedback, and on-failure. Extract the ability body from
`AbilitiesSection.vue` into a reusable `AbilityForm` component. The skill editor
and the standalone editor both render it. This avoids a duplicate editor.

A rename flow must update the file. Follow the skill pattern: stage the new id,
stage a deletion of the old id. Reject a rename onto a registered id.

A delete must warn the admin. Deleting a shared ability breaks every skill that
references the id. The skill editor's reference validation already rejects
unknown ids, so affected skills fail on reload. The delete flow should surface
this risk.

### 5.4 Router registration

Add routes in `web/frontend/src/router.ts`:

| Route | Name | Component |
|---|---|---|
| `/base-abilities` | `BaseAbilities` | `BaseAbilitiesPage.vue` |
| `/base-abilities/new` | `BaseAbilityNew` | `BaseAbilityEditorPage.vue` |
| `/base-abilities/:id` | `BaseAbilityEdit` | `BaseAbilityEditorPage.vue` |

All three require auth, matching the existing guarded routes.

---

## 6. Shared-Edit Interaction Analysis

Editing a base ability affects every skill that references it. The merge is a
top-level field overwrite. A change to the base's trigger, mechanics, or
requirements changes behavior in all referencing skills.

Proposed UI behavior:

- Show the number of referencing skills on the ability list and editor. The
  backend can compute this from the skill registry.
- Warn before delete. List the affected skills.
- Warn before a change that removes or changes the trigger. The skills that
  inherit the trigger may stop working.
- Show a read-only "Referenced by N skills" line in the editor.

The affected-skills count is cheap to compute. `SkillManager.getSkills()`
already holds every parsed skill. A helper counts how many abilities lists
contain the id.

---

## 7. Phased Implementation Plan

### Phase 1: Backend API and list page

1. Extract ability conversion into `AbilitySerializer`.
2. Add `AbilityHandler` with the full CRUD routes.
3. Add `StagingManager` support for `abilities/*.yml`.
4. Add validation through the live registries.
5. Move `GET /api/abilities` into the handler.
6. Add `api.abilities.*` to the frontend client.
7. Add `BaseAbilitiesPage.vue` and the topbar link.
8. Test with backend unit tests and frontend build.

Deliverable: an admin can list, create, and delete standalone abilities from
the GUI. Editing comes in Phase 2.

### Phase 2: Editor page

1. Extract the ability body from `AbilitiesSection.vue` into `AbilityForm`.
2. Add `BaseAbilityEditorPage.vue`.
3. Wire create, update, rename, and delete flows.
4. Add the affected-skills warning and delete confirm.
5. Add E2E coverage for list, edit, and delete.

Deliverable: a full standalone-ability editor.

### Recommended order relative to ISSUE-274

ISSUE-274 is the prerequisite. It teaches the skill editor to reference base
abilities safely. This page is the natural home for editing the shared
definitions. Build this after ISSUE-274. The reference validation in ISSUE-274
gives this page its safety net.

---

## 8. Alternative Considered

Alternative: reuse `AbilitiesSection.vue` unchanged inside a wrapper page.

The wrapper would render an `AbilitiesSection` bound to a one-ability array.
This avoids extracting `AbilityForm`. The downside is a poor fit. The section
has add, duplicate, and reorder controls meant for a multi-ability skill. The
standalone editor needs create, delete, and rename semantics. The skill editor
would inherit new controls it does not need.

Recommendation: extract `AbilityForm` and give each editor its own controls.
The extraction is the higher-cost option, but it keeps both editors clean.

---

## 9. Open Questions

1. Route naming. Use `/base-abilities` or a different name?
2. Should the existing `/abilities` search page also show standalone abilities
   that no skill references? The current page only shows abilities inside
   skills.
3. Should the editor allow editing the trigger? A change affects every
   referencing skill. Or should the trigger be read-only when the ability is
   referenced?
4. Should the delete flow require the admin to confirm by typing the id, or is
   a checkbox confirm enough?
5. Should the backend `GET /api/abilities/{id}` return the raw registered map,
   or the normalized DTO shape used by the skill editor? The DTO shape matches
   the editor model and is recommended.

---

## 10. Recommended Architecture Summary

- Backend: `AbilityHandler` + extracted `AbilitySerializer` + `StagingManager`
  `abilities/` support.
- Validation: registry-aware parse under the read lock, fail-fast at stage.
- Frontend: `BaseAbilitiesPage` list + `BaseAbilityEditorPage` editor, both
  reusing a shared `AbilityForm`.
- Interaction: affected-skills counts, delete warning, trigger-change warning.
- Sequencing: after ISSUE-274, in two shippable phases.
