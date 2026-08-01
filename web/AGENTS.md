# AI Agent Instructions for Skilling Web GUI (web)

This is the Web GUI subsystem of the Skilling PaperMC plugin. It is the closest DOX contract for all work under `web/` and for the Java backend package `io.github.chasehuegel.skilling.web` (physically under `src/main/java/.../web/**`). Read the root `AGENTS.md` for project-wide rules, then use this file for local web rules.

## Purpose

Provide an administrative browser UI for the Skilling plugin: view and edit skill definitions, tags, config, and GUI layout, with a staging workflow before applying changes to the live plugin. It is a **separate subsystem** with zero coupling to the engine core. The web package communicates exclusively through the public `SkillingAPI` service and direct filesystem reads/writes.

## Ownership

- `web/frontend/**` — Vue 3 + Vite frontend.
- `web/frontend/e2e/**` — Playwright end-to-end tests.
- `src/main/java/io/github/chasehuegel/skilling/web/**` — Javalin 7 backend (auth, config, dto, handler, staging).
- Does NOT own the engine core; the Java web backend is the only `web`-owned code inside `src/`.

## Local Contracts

### Tech Stack

| Layer | Technology |
|-------|-----------|
| **HTTP Server** | Javalin 7 (embedded Jetty, shaded into plugin JAR) |
| **JSON** | Javalin built-in (via Gson/reflection) |
| **Frontend** | Vue 3 + Vite + TypeScript |
| **UI Library** | PrimeVue 4 (Aura theme) |
| **State** | Pinia |
| **Router** | Vue Router 4 (hash-based) |
| **HTTP Client** | `fetch` (native, thin wrapper) |
| **Testing** | Playwright 1.x (Chromium) |
| **Build** | Gradle (Shadow) for Java; Vite for frontend |

### Commit Conventions

All commits in this repository MUST follow `../docs/dev/CONVENTIONS-COMMITS.md`. Refer to it before writing any commit message.

### Component Conventions

- All components use `<script setup lang="ts">` (Composition API)
- Props are typed with `defineProps<{ ... }>()` (generic syntax, not runtime)
- Emits use `defineEmits<{ eventName: [args] }>()`
- `v-model` is used for data flow (components emit `update:modelValue`)
- No PrimeVue components are used directly; all inputs are plain HTML with scoped CSS styling. This keeps the bundle small and avoids framework lock-in.
- Styles are scoped (`<style scoped>`) with CSS custom properties from PrimeVue's theme (`var(--p-*)`). Fallback values are provided for when the theme isn't loaded (e.g., `var(--p-primary-color, #3b82f6)`).

### Backend REST API

All API routes are registered in `WebServer.java` using Javalin 7's `routes` API (`app.unsafe.routes`). Handlers live in the `handler/` package.

#### Route Table

| Method | Path | Handler | Description |
|--------|------|---------|-------------|
| `GET` | `/api/health` | inline | Health check (no auth) |
| `GET` | `/api/auth/check` | inline | Validate credentials (no auth) |
| `GET` | `/api/skills` | `SkillHandler.list` | List all skill summaries |
| `GET` | `/api/skills/{id}` | `SkillHandler.get` | Get full skill detail |
| `POST` | `/api/skills` | `SkillHandler.create` | Create skill (staged) |
| `PUT` | `/api/skills/{id}` | `SkillHandler.update` | Update skill (staged) |
| `DELETE` | `/api/skills/{id}` | `SkillHandler.delete` | Delete skill file |
| `GET` | `/api/tags` | `TagHandler.get` | Get all custom tags |
| `PUT` | `/api/tags` | `TagHandler.update` | Update tags.yml (staged) |
| `GET` | `/api/config` | `ConfigHandler.get` | Get config.yml values |
| `PUT` | `/api/config` | `ConfigHandler.update` | Update config.yml (staged) |
| `GET` | `/api/staging/status` | inline | Check pending changes |
| `DELETE` | `/api/staging` | inline | Discard all staged changes |
| `POST` | `/api/reload` | `ReloadHandler.reload` | Apply staged + reload |

#### Staging Workflow

1. **Edit** → `PUT/POST` writes to `run/plugins/Skilling/.web_staging/`
2. **Pending** → `GET /api/staging/status` shows changed files
3. **Apply & Reload** → `POST /api/reload` copies staged → live, triggers `LockdownManager.reload()`, creates backup in `.web_staging/backup/{timestamp}/`
4. **Discard** → `DELETE /api/staging` clears staging directory

#### Conflict Detection

Before applying, `StagingManager.applyAndBackup()` snapshots live file timestamps at staging time. If a live file was modified externally (e.g., FTP) since staging, the reload is rejected with HTTP 409 and a list of conflicting files.

### Minecraft Asset Textures

The skill card icons render actual Minecraft item textures via a public CDN. This gives admins a visual preview of how their icon choice looks.

#### Configuration

The Minecraft release version used for asset URLs is set in `web/frontend/.env`:

```
VITE_MINECRAFT_ASSETS_VERSION=1.21.4
```

This defaults to `1.21.4` (matching the current Paper API target). To change it, edit the `.env` file and rebuild the frontend: `cd web/frontend && npm run build`.

The version must correspond to a branch in the [InventivetalentDev/minecraft-assets](https://github.com/InventivetalentDev/minecraft-assets) repository (e.g., `1.21.4`, `1.21.3`, etc.).

#### Texture CDN Source

Textures are fetched from GitHub's raw content CDN:

```
https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/{VERSION}/assets/minecraft/textures/item/{ITEM_NAME}.png
```

- Strips the `minecraft:` namespace from material strings.
- Falls back to a styled letter-in-circle if the texture fails to load (network error, missing texture, or invalid version).
- The `MinecraftIcon` component handles loading states (shimmer), error states (letter fallback), and loaded states (fade-in).

### Security Considerations

- **Auth:** Basic Auth over HTTP. All API routes (except `/api/health` and `/api/auth/check`) require a valid `Authorization: Basic ...` header.
- **CORS:** All origins/methods/headers allowed (admin tool, trusted network).
- **XSS:** Vue's template compiler sanitizes all user input. No `v-html`.
- **Path traversal:** Skill IDs are validated against `[a-z_][a-z0-9_]*`.
- **Staging:** Edits go to a separate staging directory first; only explicit "Apply & Reload" touches live files.
- **Backups:** Before applying, a timestamped backup is created in `.web_staging/backup/`.

## Work Guidance

### Directory Structure

```
web/
  frontend/            # Vue 3 + Vite project root
    src/
      api/             # fetch wrapper + per-resource API functions
      stores/          # Pinia stores (auth, staging, skills, tags, config)
      components/      # Vue components organized by domain
        common/        # Reusable: EvaluatorParameter, FilterBuilder, SectionToolbar, ToastNotification
        layout/        # AppTopbar, PendingChangesBanner
        skills/        # SkillCard, SkillIdentitySection, DisplaySection, etc.
        tags/          # TagListEditor, MaterialMultiSelect
        config/        # ConfigSection
      views/           # LoginPage, DashboardPage, SkillEditorPage, TagsPage, ConfigPage
      router.ts        # Hash-based Vue Router config with auth guard
      main.ts          # Vue app bootstrap (PrimeVue, Pinia, Router)
    e2e/               # Playwright end-to-end tests
      pages/           # Page Object Models
      specs/           # Test specifications
      helpers/         # Debug utilities (inspectElement, tweakCSS, screenshots)
      test-data/       # Fixture YAML files for E2E testing
      playwright.config.ts
      globalSetup.ts   # Starts Paper dev server, seeds data, logs in
      globalTeardown.ts# Stops dev server
    dist/              # Built frontend (bundled into JAR resources)
```

### Running the Frontend

```bash
# Development (Vite dev server)
cd web/frontend && npm run dev

# Production build (outputs to dist/)
cd web/frontend && npm run build
```

### Visual Debugging Workflow

The `e2e/helpers/debug.ts` module provides utilities for visual refinement:

```typescript
import { inspectElement, tweakCSS, takeScreenshot, highlightElement } from '../e2e/helpers/debug';

// Inspect computed styles and box model
const info = await inspectElement(page, '.skill-card');

// Try a CSS tweak and capture the result
await tweakCSS(page, '.skill-grid', 'grid-template-columns', 'repeat(3, 1fr)', 'grid-3-col');

// Highlight an element for visual inspection
await highlightElement(page, '.skill-card:first-child', 'highlight-first-card');
```

To open an interactive headed browser with DevTools:
```bash
# From the frontend directory
npx playwright open http://localhost:8082
```

### E2E Testing with Playwright

All tests live in `web/frontend/e2e/`. Page Object Models (POMs) in `pages/` encapsulate selectors and actions. Spec files in `specs/` use the POMs to describe scenarios.

The E2E tests require the Skilling plugin running on a Paper server with `web.enabled: true`. Two modes are supported:

**A) Automatic (default):** `globalSetup.ts` copies fixture data into `run/plugins/Skilling/`, starts the Paper dev server via `./gradlew runServer`, waits for the Web GUI, logs in via the login page, and saves the auth storage state. `globalTeardown.ts` kills the server.

**B) External server:** Set `SKILLING_SERVER_URL` env var to point to an already-running server. Tests connect to that URL instead.

```bash
# Full test suite (starts server, runs tests, stops server)
cd web/frontend && npm run e2e

# Run with visible browser
npm run e2e:headed

# Interactive Playwright UI
npm run e2e:open

# Update screenshot baselines
npm run e2e:update

# Debug a specific test interactively
npx playwright test --debug --grep "dashboard"
```

#### Test Fixtures

Fixture data lives in `e2e/test-data/`:
- `config.yml` — plugin config with `web.enabled: true`
- `tags.yml` — sample custom tags (`c:ores`, `c:stone`)
- `skills/mining.yml` — sample mining skill with XP sources and abilities

The `globalSetup.ts` copies these into `run/plugins/Skilling/` before the server starts, so they are loaded as the initial plugin state.

#### Writing Tests

1. Create or reuse a Page Object Model in `pages/`
2. Add a spec file in `specs/`
3. Use the POM methods for assertions and actions
4. Use `takeScreenshot()` from helpers for visual snapshots
5. Run with `npm run e2e:headed` to watch the browser

Example:
```typescript
import { test, expect } from '@playwright/test';
import { DashboardPage } from '../pages/DashboardPage';

test('displays skill cards', async ({ page }) => {
  const dashboard = new DashboardPage(page);
  await dashboard.goto();
  expect(await dashboard.getSkillCount()).toBeGreaterThanOrEqual(1);
});
```

## Verification

* `cd web/frontend && npm run build` — type-checks (`vue-tsc -b`) and builds the frontend.
* `cd web/frontend && npm run e2e` — full Playwright suite (starts server, runs tests, stops server).
* CI runs type-check, build, and `./gradlew test` (see `.github/workflows/ci.yml`).

## Child DOX Index

| Path | Scope |
|---|---|
| `src/AGENTS.md` | The Java web backend package physically lives under `src/main/java/io/github/chasehuegel/skilling/web/**`, which is owned here. `src/AGENTS.md` documents that routing; no conflict. |
