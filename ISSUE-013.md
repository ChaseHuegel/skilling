# ISSUE-013: Confirmation Dialogs

**Scope:** Add confirmation dialogs to destructive actions to prevent accidental data loss.

---

## 1. Tags Reset Button

**Current:** Tags page has a "Reset" button that immediately discards changes with no confirmation.

**Fix:** Add a confirmation dialog before executing the reset. Use the existing modal-overlay pattern from `SkillEditorPage.vue`.

### Dialog Text

```
┌─────────────────────────────┐
│  Discard tag changes?       │
│                             │
│  Any unsaved changes to     │
│  your custom tags will be   │
│  lost.                      │
│                             │
│     [Keep Editing] [Discard]│
└─────────────────────────────┘
```

### File

- `src/views/TagsPage.vue`

---

## 2. Skills Reset Button

**Current:** There is no reset button on the dashboard. The only way to discard changes is via the PendingChangesBanner's "Discard" button.

**Fix:** Add a "Reset" button (secondary style) next to the "New Skill" button in the dashboard header. When clicked, show a confirmation dialog.

### Dialog Text

```
┌────────────────────────────────┐
│  Discard all pending changes?  │
│                                │
│  This will remove all staged   │
│  edits to skills, tags, and    │
│  configuration. The pending    │
│  changes banner will disappear.│
│                                │
│     [Keep Editing] [Discard]   │
└────────────────────────────────┘
```

### File

- `src/views/DashboardPage.vue`
- `src/components/layout/PendingChangesBanner.vue`

### Behavior

- Reset button only visible when there are pending changes
- Calls `staging.discard()` after confirmation
- Refreshes the staging status display
