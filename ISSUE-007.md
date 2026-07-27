# ISSUE-007: Web GUI — Administrative Interface

**Epic / Milestone** — Estimated: 6+ weeks of development

---

## 1. Overview & Design Philosophy

The Web GUI is a browser-based administrative interface for managing the
Skilling plugin's configuration, skill definitions, and custom tags. It
is a **separate subsystem** that lives in its own package tree with zero
coupling to the engine core. The web server is **disabled by default**
and only starts when explicitly configured in `config.yml`.

### Design Tenets

1. **No coupling to engine internals.** The web package communicates with
   the plugin exclusively through the public `SkillingAPI` service and
   direct filesystem reads/writes to the config directory. No engine class
   is ever imported by web code.

2. **Staged changes.** Edits are never written directly to live config
   files. They are saved to a staging directory (`plugins/Skilling/.web_staging/`).
   Only an explicit "Apply & Reload" action copies staged files to their
   live locations and triggers the reload lockdown sequence. This prevents
   partial or invalid edits from breaking a running server.

3. **Fail-safe defaults.** The web server is disabled by default. If any
   configuration error occurs (bad port, invalid credentials, port in use),
   the server fails to start with a clear warning in the console but the
   plugin continues running normally.

4. **Backend-agnostic frontend.** The Vue SPA communicates exclusively
   through the REST API. The frontend has no knowledge of filesystem paths,
   YAML structures, or plugin internals. It receives and sends DTOs (Data
   Transfer Objects) — plain JSON structures that mirror the YAML schema
   but are optimized for form-based editing.

5. **Authenticated by default.** Every API request requires HTTP Basic
   Authentication. The frontend presents a proper login page (not the
   browser's native auth dialog) on first load and whenever the session
   expires.

---

## 2. Tech Stack

| Layer | Technology | Version | Rationale |
|-------|-----------|---------|-----------|
| **HTTP Server** | Javalin 7 | 7.x | First-class REST routing, static file serving, CORS, active maintenance |
| **JSON** | Gson | (shaded) | Already in the plugin; handles DTO serialization |
| **Logging** | SLF4J + java.util.logging bridge | — | Javalin logs via SLF4J; bridge to Bukkit logger |
| **Frontend** | Vue 3 + Vite | 3.x / 6.x | Composition API, fast HMR, single-file components |
| **UI Library** | PrimeVue 4 | 4.x | Pre-built drag-and-drop, tree, form, dialog, and select components |
| **Styling** | PrimeFlex + SCSS | 4.x / latest | Utility-first CSS, responsive grid, consistent with PrimeVue themes |
| **HTTP Client (FE)** | fetch (native) | — | No Axios needed; thin wrapper with auth header injection |
| **State (FE)** | Pinia | 3.x | Lightweight Vue store for pending-changes tracking, auth state |
| **Router (FE)** | Vue Router | 4.x | Hash-based routing (no server-side URL rewrites needed) |
| **Build (FE)** | Vite + @vitejs/plugin-vue | 6.x | Fast builds, CSS/JS bundling, tree-shaking |
| **Build (Plugin)** | Gradle (Shadow) + Node.js plugin | — | Builds frontend, bundles into JAR resources |

### Dependency impact on plugin JAR

| Dependency | Shaded size |
|-----------|-------------|
| Javalin 7 | ~4 MB (includes Jetty, shaded & relocated) |
| Gson | (already present) |
| SLF4J + jul bridge | ~200 KB |
| **Frontend static files** | ~100–200 KB (compiled Vue + PrimeVue) |
| **Total** | **~4.5 MB added** |

The JAR will grow from its current size by approximately 4.5 MB when the
web module is included. The web server is disabled by default, so the
only cost on a production server that doesn't use the web GUI is disk
space for the JAR — no CPU, memory, or port overhead.

---

## 3. Package Structure

ALL web code lives under `io.github.chasehuegel.skilling.web` — a
top-level package with no imports from `engine.*` (except the API):

```
io.github.chasehuegel.skilling.web/
  WebServer.java                  # Javalin lifecycle: start/stop, route registration, auth filter
  
  config/
    WebConfig.java                # Record parsed from config.yml web section
  
  auth/
    BasicAuthenticator.java       # Validates Basic Auth credentials against WebConfig
  
  dto/
    SkillSummaryDTO.java          # id, displayName, icon, color, maxLevel, abilityCount
    SkillDetailDTO.java           # Full skill tree as structured JSON (not raw YAML)
    TagListDTO.java               # Map<String, List<String>> — tag name → materials
    ConfigDTO.java                # Typed fields mirroring config.yml keys
    StagingStatusDTO.java         # hasPendingChanges, fileCount, lastModified
    ReloadResultDTO.java          # success, message, errors[ ]
  
  handler/
    SkillHandler.java             # CRUD for skill definitions
    TagHandler.java               # Read/write tags.yml
    ConfigHandler.java            # Read/write config.yml
    ReloadHandler.java            # Apply staged changes → trigger reload
    StagingHandler.java           # Query pending changes status
    AuthHandler.java              # Session validation endpoint
  
  staging/
    StagingManager.java           # Manages .web_staging directory, copy to live
  
  frontend/                       # Built Vue SPA (bundled into JAR resources)
    index.html
    assets/
      index-[hash].js
      index-[hash].css
      primevue-[theme].css
```

### Staging directory structure

```
plugins/Skilling/
  config.yml                      # Live config (read by plugin at runtime)
  tags.yml                        # Live tags
  skills/                         # Live skill definitions
  .web_staging/                   # ← Created by web GUI
    config.yml                    # Staged config (if modified)
    tags.yml                      # Staged tags (if modified)
    skills/                       # Staged skill definitions (if modified)
      mining.yml
      woodcutting.yml
    status.json                   # Timestamp + list of staged files
```

---

## 4. Backend Architecture

### 4.1 WebServer lifecycle

```java
public final class WebServer {
    private final Skilling plugin;
    private final WebConfig config;
    private Javalin app;

    public WebServer(Skilling plugin, WebConfig config) { ... }

    public void start() {
        if (config.enabled()) {
            app = Javalin.create(javalinConfig -> {
                javalinConfig.staticFiles.add("/web/frontend");
                javalinConfig.showJavalinBanner = false;
                javalinConfig.plugins.enableCors(cors -> cors.add(c -> {
                    c.anyHost();
                }));
            });
            registerRoutes();
            app.start(config.port());
            plugin.getLogger().info("Web GUI started on port " + config.port());
        }
    }

    public void stop() {
        if (app != null) app.stop();
    }
}
```

### 4.2 Authentication flow

The frontend is a SPA that requires authentication. To avoid the browser's
native Basic Auth dialog (which would appear before the page even loads),
the authentication is handled at the **API layer only**, not via Javalin's
global `AccessManager`.

**How it works:**

1. Static files (the SPA) are served freely — no auth required to load
   `index.html`, JS, and CSS.
2. All `/api/*` routes are protected by a `before` filter that checks the
   `Authorization` header.
3. The SPA's login page calls `GET /api/auth/check` with credentials.
   - 200: Valid credentials → proceed to dashboard.
   - 401: Invalid → show error on login page.
4. The SPA stores credentials in `sessionStorage` and attaches
   `Authorization: Basic <base64>` to every subsequent API call.
5. If any API call returns 401 at any point, the SPA redirects to the
   login page (session expired, password changed, etc.).

```java
// Javalin route setup
app.before("/api/*", ctx -> {
    if (ctx.path().equals("/api/auth/check")) return; // allow unauthenticated check
    String auth = ctx.header("Authorization");
    if (auth == null || !basicAuthenticator.valid(auth)) {
        ctx.status(401).header("WWW-Authenticate", "Basic realm=\"Skilling Web GUI\"");
    }
});

app.get("/api/auth/check", ctx -> {
    // If we reach here, credentials are valid (before filter passed)
    ctx.json(Map.of("status", "ok", "user", config.username()));
});
```

### 4.3 Config.yml integration

New section in the default `config.yml`:

```yaml
# Web-based administration interface
# Disabled by default. Enable only if you need the browser GUI.
web:
  # Enable the web server
  enabled: false
  # Port to listen on (use a port > 1024 on Linux)
  port: 8082
  # Basic authentication credentials
  username: "admin"
  password: "skilling"
```

The `WebConfig` record:

```java
public record WebConfig(
    boolean enabled,
    int port,
    String username,
    String password
) {
    public static WebConfig load(YamlConfiguration config) {
        return new WebConfig(
            config.getBoolean("web.enabled", false),
            config.getInt("web.port", 8082),
            config.getString("web.username", "admin"),
            config.getString("web.password", "skilling")
        );
    }
}
```

---

## 5. REST API Specification

### 5.1 Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/auth/check` | **No** | Validate credentials; used by login page |

**Request:**
```
Authorization: Basic YWRtaW46c2tpbGxpbmc=
```

**Response 200:**
```json
{ "status": "ok", "user": "admin" }
```

**Response 401:**
```json
{ "status": "error", "message": "Invalid credentials" }
```

---

### 5.2 Skills

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/skills` | List all skill summaries |
| `GET` | `/api/skills/{id}` | Get full skill detail |
| `POST` | `/api/skills` | Create a new skill (staged) |
| `PUT` | `/api/skills/{id}` | Update an existing skill (staged) |
| `DELETE` | `/api/skills/{id}` | Delete a skill file (immediate + staged removal) |

**`GET /api/skills` — Response:**
```json
[
  {
    "id": "mining",
    "displayName": "Mining",
    "icon": "minecraft:iron_pickaxe",
    "color": "GREEN",
    "maxLevel": 100,
    "abilityCount": 2
  }
]
```

**`GET /api/skills/{id}` — Response** (abbreviated structure):
```json
{
  "id": "mining",
  "displayName": "Mining",
  "maxLevel": 100,
  "icon": "minecraft:iron_pickaxe",
  "customModelData": 1001,
  "color": "GREEN",
  "style": "SEGMENTED_10",
  "progression": {
    "curve": "polynomial",
    "baseXp": 50,
    "exponent": 2.5
  },
  "xpSources": [
    {
      "trigger": "block_break",
      "filters": [
        { "target": "#c:ores", "state": "player_placed:false" }
      ],
      "reward": { "type": "constant", "value": 15.0 }
    }
  ],
  "abilities": [
    {
      "id": "geologist",
      "displayName": "Geologist",
      "unlockLevel": 1,
      "display": {
        "lore": [
          "&7Increases raw ore yield by &a{yield_chance}%&7."
        ]
      },
      "requirements": { "cooldown": 0, "state": [], "items": [] },
      "mechanics": [
        {
          "type": "core:yield_multiplier",
          "filters": [
            { "target": "#c:ores" },
            { "tool": "#minecraft:pickaxes" }
          ],
          "parameters": {
            "yield_chance": {
              "type": "linear",
              "base": 0.5,
              "step": 0.5,
              "max": 50.0
            }
          }
        }
      ],
      "feedback": {
        "actionBar": false,
        "chat": false,
        "message": "",
        "particles": [],
        "sounds": []
      }
    }
  ]
}
```

**Note on evaluator serialization:** The evaluator's type is inlined as
a `type` discriminator field rather than using the YAML key convention
(e.g., `linear: { base: 0.5 }`). This is because the frontend needs a
consistent way to switch between evaluator types in a dropdown. The
handler converts between the REST JSON format and the YAML format:

```java
// YAML → DTO
parameters: {
  "yield_chance": { "linear": { "base": 0.5, "step": 0.5, "max": 50.0 } }
}

// DTO → JSON
parameters: {
  "yield_chance": { "type": "linear", "base": 0.5, "step": 0.5, "max": 50.0 }
}
```

---

### 5.3 Tags

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/tags` | Get all custom tags |
| `PUT` | `/api/tags` | Update tags.yml (staged) |

**`GET /api/tags` — Response:**
```json
{
  "tags": {
    "c:ores": ["minecraft:coal_ore", "minecraft:iron_ore", "#minecraft:copper_ores"],
    "c:stone": ["minecraft:stone", "minecraft:andesite", "minecraft:granite"]
  }
}
```

---

### 5.4 Config

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/config` | Get config.yml |
| `PUT` | `/api/config` | Update config.yml (staged) |

**`GET /api/config` — Response:**
```json
{
  "database": { "poolSize": 10, "walMode": true },
  "bossbar": { "maxActive": 2, "fadeTicks": 40 },
  "debouncer": { "intervalMs": 500 },
  "debugLogging": false,
  "titles": { "stayDuration": 5000 },
  "globalXpModifier": 1.0,
  "web": { "enabled": false, "port": 8082, "username": "admin", "password": "skilling" }
}
```

---

### 5.5 Staging & Reload

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/staging/status` | Check if any staged changes exist |
| `DELETE` | `/api/staging` | Discard all staged changes |
| `POST` | `/api/reload` | Apply staged changes → reload plugin |

**`GET /api/staging/status` — Response:**
```json
{
  "hasPendingChanges": true,
  "fileCount": 3,
  "files": ["skills/mining.yml", "tags.yml", "config.yml"],
  "lastModified": "2026-07-27T15:30:00Z"
}
```

**`POST /api/reload` — Request (optional):**
```json
{ "confirm": true }
```

**`POST /api/reload` — Response 200:**
```json
{
  "success": true,
  "message": "Changes applied. Plugin reloaded successfully.",
  "errors": []
}
```

**`POST /api/reload` — Response 500:**
```json
{
  "success": false,
  "message": "Reload failed. See console for details.",
  "errors": ["YAML error in skills/mining.yml: unmatched bracket at line 42"]
}
```

---

## 6. Frontend Architecture

### 6.1 Auth flow (detailed)

```
User navigates to http://localhost:8082
        ↓
SPA loads (index.html, JS, CSS — no auth required)
        ↓
Vue Router guard checks SessionStorage for credentials
        ↓
  [no credentials]                 [has credentials]
        ↓                                  ↓
  Login page displayed         GET /api/auth/check
        ↓                           [200]        [401]
  User enters credentials               ↓           ↓
        ↓                          Dashboard    Clear credentials
  POST credentials to           (proceed)     Show login page
  GET /api/auth/check                              ↓
  [200]        [401]                          (error message)
     ↓           ↓
  Store in     Show error
  SessionStorage
     ↓
  Redirect to /
     ↓
  Dashboard
```

The login page is a proper Vue component with username/password fields
styled with PrimeVue's `InputText` and `Password` components. It must NOT
rely on the browser's native HTTP Basic Auth dialog.

Implementation approach in the SPA:

```typescript
// api/client.ts
const credentials = sessionStorage.getItem('skilling_credentials');

async function apiFetch(path: string, options?: RequestInit): Promise<Response> {
    const headers: Record<string, string> = {
        'Authorization': 'Basic ' + credentials,
        'Content-Type': 'application/json',
        ...(options?.headers as Record<string, string> || {}),
    };
    const res = await fetch(path, { ...options, headers });
    if (res.status === 401) {
        sessionStorage.removeItem('skilling_credentials');
        window.location.hash = '#/login';
    }
    return res;
}
```

### 6.2 Route map

| Path | Component | Auth | Description |
|------|-----------|------|-------------|
| `/login` | `LoginPage.vue` | No | Username/password form |
| `/` | `DashboardPage.vue` | Yes | Skill grid overview, pending changes status |
| `/skills/new` | `SkillEditorPage.vue` | Yes | Create new skill |
| `/skills/:id` | `SkillEditorPage.vue` | Yes | Edit existing skill |
| `/tags` | `TagsPage.vue` | Yes | Edit custom tags |
| `/config` | `ConfigPage.vue` | Yes | Edit config.yml |
| `/settings` | `SettingsPage.vue` | Yes | Web server settings (change password, restart) |

### 6.3 Component tree

```
App.vue
├── AppTopbar.vue (PrimeVue Menubar — nav links, theme toggle, logout)
├── RouterView
│   ├── LoginPage.vue
│   │   └── Card > InputText (username) + Password + Button
│   │
│   ├── DashboardPage.vue
│   │   ├── PendingChangesBanner.vue (PrimeVue Message — "Apply & Reload" button)
│   │   └── SkillGrid.vue
│   │       └── SkillCard.vue (PrimeVue Card — icon, name, level, ability count)
│   │
│   ├── SkillEditorPage.vue
│   │   ├── SkillIdentitySection.vue (id, displayName, maxLevel)
│   │   ├── DisplaySection.vue
│   │   │   ├── IconPicker.vue (material autocomplete + custom model data)
│   │   │   ├── ColorPicker.vue (dropdown: BarColor values)
│   │   │   └── StylePicker.vue (dropdown: BarStyle values)
│   │   ├── ProgressionSection.vue
│   │   │   └── CurveTypeSelector.vue (dropdown → dynamic params)
│   │   │       ├── PolynomialParams.vue (baseXp, exponent)
│   │   │       ├── LinearParams.vue (base, step, min, max)
│   │   │       ├── MilestoneParams.vue (key-value map editor)
│   │   │       └── ConstantParams.vue (value)
│   │   ├── XpSourcesSection.vue
│   │   │   ├── SectionToolbar.vue (Add, Duplicate, Delete buttons)
│   │   │   └── XpSourceList.vue (PrimeVue OrderList — drag-and-drop)
│   │   │       └── XpSourceEditor.vue
│   │   │           ├── TriggerSelector.vue (dropdown: block_break, entity_kill, etc.)
│   │   │           ├── FilterBuilder.vue
│   │   │           │   ├── TargetSelector.vue (autocomplete: #c:*, #minecraft:*, materials)
│   │   │           │   ├── StateSelector.vue (multi-checkbox: is_sneaking, player_placed:false)
│   │   │           │   └── ToolSelector.vue (autocomplete: #minecraft:pickaxes, etc.)
│   │   │           └── RewardEditor.vue (EvaluatorParameter — type dropdown + dynamic fields)
│   │   └── AbilitiesSection.vue
│   │       ├── SectionToolbar.vue (Add, Duplicate)
│   │       └── AbilityList.vue (PrimeVue OrderList — drag-and-drop)
│   │           └── AbilityEditor.vue (Tabs: Display / Requirements / Mechanics / Feedback)
│   │               ├── AbilityDisplayTab.vue (lore lines editor)
│   │               ├── RequirementsTab.vue
│   │               │   ├── CooldownInput.vue (number)
│   │               │   ├── StateSelector.vue (multi-checkbox)
│   │               │   └── ItemsEditor.vue (possession/cost items with tag/amount)
│   │               ├── MechanicsTab.vue
│   │               │   ├── MechanicToolbar.vue (Add, Duplicate, Delete)
│   │               │   └── MechanicList.vue (PrimeVue OrderList — drag-and-drop)
│   │               │       └── MechanicEditor.vue
│   │               │           ├── TypeSelector.vue (dropdown: core:yield_multiplier, etc.)
│   │               │           ├── FilterBuilder.vue (same as above)
│   │               │           └── ParametersEditor.vue (dynamic per mechanic type)
│   │               └── FeedbackTab.vue
│   │                   ├── NotifySection.vue (actionBar toggle, chat toggle, message input)
│   │                   ├── ParticlesEditor.vue (add/remove/reorder particle effects)
│   │                   └── SoundsEditor.vue (add/remove/reorder sound effects)
│   │
│   ├── TagsPage.vue
│   │   └── TagListEditor.vue
│   │       └── TagEditor.vue
│   │           ├── TagNameInput.vue
│   │           └── MaterialMultiSelect.vue (autocomplete + tag references)
│   │
│   ├── ConfigPage.vue
│   │   ├── DatabaseSection.vue (poolSize number, walMode toggle)
│   │   ├── BossBarSection.vue (maxActive, fadeTicks)
│   │   ├── DebouncerSection.vue (intervalMs)
│   │   ├── DebugSection.vue (debugLogging toggle)
│   │   ├── TitleSection.vue (stayDuration)
│   │   ├── XpSection.vue (globalXpModifier number)
│   │   └── WebSection.vue (enabled toggle, port, username, password)
│   │
│   └── SettingsPage.vue
│       ├── ChangePasswordForm.vue
│       └── RestartServerButton.vue
```

### 6.4 Key Interaction Patterns

#### Drag-and-Drop Reordering

Abilities, XP sources, and mechanics within an ability are all
reorderable via PrimeVue's `OrderList` component. The list emits a
reorder event that updates the Pinia store's array order. No backend
call is made until "Apply & Reload" is clicked.

```
[Geologist]     ← drag handle
[Vein Miner]    ← drag handle
    ↑ user drags Vein Miner above Geologist
[Vein Miner]    ← reordered
[Geologist]
```

#### Duplicating Sections

Each section (ability, XP source, mechanic) has a "Duplicate" button
in its toolbar. Clicking creates a deep copy of the section with a
modified ID/name (e.g., "Geologist (copy)") and appends it to the
list. The copy inherits all the same form values.

#### Evaluator Type Switching

When the user changes an evaluator's type dropdown (e.g., from
"linear" to "milestone"), the form fields dynamically swap:
- **Linear:** shows `base`, `step`, `min`, `max` number inputs
- **Milestone:** shows a key-value map editor (level → value pairs)
- **Constant:** shows a single `value` input
- **Polynomial:** shows `baseXp`, `exponent` inputs

Previous values are preserved when switching back. If the evaluator
type doesn't use a particular field, the field is hidden but its
value is retained in the store.

#### Filter Builder

The `FilterBuilder` component has three sections:
- **Target:** An autocomplete input that suggests:
  - Custom tags (`#c:ores`, `#c:stone`, etc.)
  - Vanilla tags (`#minecraft:logs`, `#minecraft:pickaxes`, etc.)
  - Block/item materials (`minecraft:diamond_ore`, etc.)
- **State:** A multi-checkbox selector with known states
  (`is_sneaking`, `is_sprinting`, `is_in_water`, `is_on_ground`)
  plus a custom text input for arbitrary states like
  `player_placed:false`
- **Tool:** An autocomplete input (same as target but for tools)

---

## 7. Staged Changes Workflow (Detailed)

### 7.1 Data flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Browser (Vue SPA)                         │
│                                                             │
│  User edits skill → Pinia store updates (in-memory only)    │
│  User clicks "Save Changes"                                 │
│                                                             │
│  PUT /api/skills/mining  {...}                               │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                    Javalin API Handler                       │
│                                                             │
│  Validates JSON structure (required fields present)          │
│  Serializes to YAML format                                  │
│  Writes to .web_staging/skills/mining.yml                   │
│  Updates .web_staging/status.json                           │
│  Returns 200 OK                                             │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                    Staging Area (filesystem)                 │
│                                                             │
│  plugins/Skilling/.web_staging/                              │
│    status.json  ← { lastModified, files: [...] }            │
│    skills/                                                   │
│      mining.yml                                              │
│    tags.yml       (if modified)                              │
│    config.yml     (if modified)                              │
└─────────────────────────────────────────────────────────────┘
                                  ▲
                                  │
  User clicks "Apply & Reload"    │
  ────────────────────────────────┘
  POST /api/reload  {"confirm": true}
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                    ReloadHandler                             │
│                                                             │
│  1. Reads .web_staging/status.json                          │
│  2. For each file in staging:                                │
│     - Copies from .web_staging/ → live location              │
│     - Creates backup at .web_staging/backup/{timestamp}/     │
│  3. Calls lockDownManager.reload()                           │
│  4. On success: clears .web_staging/                         │
│  5. Returns ReloadResultDTO                                  │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 Backup before apply

Before overwriting any live file, the ReloadHandler creates a timestamped
backup:

```
.web_staging/backup/2026-07-27_15-30-00/
  config.yml
  tags.yml
  skills/
    mining.yml
```

This allows manual rollback by the server admin if the reload introduces
problems.

### 7.3 Conflict detection

If a file was modified on disk (e.g., by another admin via FTP) after the
user opened the web GUI but before they clicked "Apply & Reload," the
staging status includes a `lastModified` timestamp. On apply, the handler
compares the live file's modification time against the staging snapshot.
If the live file is newer, the reload is rejected with a conflict error
listing the affected files.

---

## 8. Frontend Build Pipeline

### 8.1 Directory structure

The Vue frontend lives outside the Java source tree to keep concerns
separated:

```
web/
  frontend/                     # Vue 3 + Vite project root
    package.json
    vite.config.ts
    tsconfig.json
    index.html                   # SPA entry point
    src/
      main.ts                    # Vue app bootstrap
      router.ts                  # Vue Router config
      stores/
        auth.ts                  # Pinia store for credentials
        staging.ts               # Pinia store for pending changes
        skills.ts                # Pinia store for skill data (cached)
        tags.ts                  # Pinia store for tag data
        config.ts                # Pinia store for config data
      api/
        client.ts                # fetch wrapper with auth header injection
        skills.ts                # API functions for skill endpoints
        tags.ts                  # API functions for tag endpoints
        config.ts                # API functions for config endpoints
        staging.ts               # API functions for staging/reload endpoints
      components/
        layout/
          AppTopbar.vue
          PendingChangesBanner.vue
        common/
          EvaluatorParameter.vue
          FilterBuilder.vue
          SectionToolbar.vue
        skills/
          SkillCard.vue
          SkillIdentitySection.vue
          DisplaySection.vue
          IconPicker.vue
          ColorPicker.vue
          ProgressionSection.vue
          CurveTypeSelector.vue
          XpSourcesSection.vue
          XpSourceEditor.vue
          RewardEditor.vue
          AbilitiesSection.vue
          AbilityEditor.vue
          AbilityDisplayTab.vue
          RequirementsTab.vue
          MechanicsTab.vue
          MechanicEditor.vue
          ParametersEditor.vue
          FeedbackTab.vue
        tags/
          TagListEditor.vue
          MaterialMultiSelect.vue
        config/
          ConfigSection.vue
      views/
        LoginPage.vue
        DashboardPage.vue
        SkillEditorPage.vue
        TagsPage.vue
        ConfigPage.vue
        SettingsPage.vue
      styles/
        main.scss
```

### 8.2 Vite configuration

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
    plugins: [vue()],
    base: '/',
    build: {
        outDir: 'dist',
        assetsInlineLimit: 0,  // always emit separate files
        rollupOptions: {
            output: {
                manualChunks: {
                    primevue: ['primevue'],
                },
            },
        },
    },
});
```

### 8.3 Gradle integration

```kotlin
// In build.gradle.kts
val buildFrontend by tasks.registering(Exec::class) {
    description = "Build the Vue frontend for production"
    workingDir = file("web/frontend")
    commandLine("npm", "run", "build")
    outputs.dir("web/frontend/dist")
}

tasks.named<Copy>("processResources") {
    dependsOn(buildFrontend)
    from("web/frontend/dist") {
        into("web/frontend")
    }
}

tasks.named("shadowJar") {
    dependsOn(buildFrontend)
}

// Only run the frontend build if the directory exists
// (developers working only on Java won't have Node.js installed)
buildFrontend.onlyIf { file("web/frontend/package.json").exists() }
```

---

## 9. Implementation Phases

### Phase 1: Backend Skeleton (Week 1)

**Objective:** Server starts, accepts connections, auth works, health
check endpoint returns JSON.

**Tasks:**

1. Add Javalin 7 dependency to `build.gradle.kts` with Shadow relocation
   (`io.javalin` → `io.github.chasehuegel.skilling.libs.javalin`)
2. Add SLF4J + jul-to-slf4j bridge for Javalin logging
3. Create `WebConfig` record
4. Add `web.*` config section to default `config.yml`
5. Create `WebServer.java` with start/stop lifecycle
6. Create `BasicAuthenticator.java`
7. Integrate into `Skilling.onEnable()` / `onDisable()`
8. Create `GET /api/auth/check` endpoint
9. Create `GET /api/health` endpoint (no auth, always available)

**Deliverable:** Server starts on configured port (default 8082), returns
`{"status":"ok"}` after auth. Curl:
```bash
curl -u admin:skilling http://localhost:8082/api/auth/check
```

---

### Phase 2: REST API — Skills CRUD (Week 2)

**Objective:** Full CRUD for skill definitions with staging.

**Tasks:**

1. Create DTOs: `SkillSummaryDTO`, `SkillDetailDTO`
2. Create `StagingManager` with directory management
3. Create `SkillHandler`:
   - `GET /api/skills` — read skill directory, return summaries
   - `GET /api/skills/{id}` — read single skill, convert to DTO
   - `POST /api/skills` — validate + write to staging
   - `PUT /api/skills/{id}` — validate + write to staging
   - `DELETE /api/skills/{id}` — delete from live + staging
4. Implement YAML ↔ DTO conversion:
   - Parse YAML → `SkillDetailDTO` (using SnakeYAML directly since
     `YamlConfiguration` is designed for flat configs, not nested trees)
   - Convert `SkillDetailDTO` → YAML string
5. Implement validation:
   - Required fields present (id, max_level, progression)
   - Ability IDs unique within skill
   - Mechanic types exist in registry
   - Evaluator parameters valid for their type

**Deliverable:** Full skill CRUD testable via curl. Skills can be created,
read, updated, and deleted through the API.

---

### Phase 3: REST API — Tags, Config, Reload (Week 3)

**Objective:** Complete all REST endpoints.

**Tasks:**

1. Create `TagHandler`:
   - `GET /api/tags` — parse and return `tags.yml`
   - `PUT /api/tags` — validate + write to staging
2. Create `ConfigHandler`:
   - `GET /api/config` — return typed config values
   - `PUT /api/config` — validate + write to staging
3. Create `StagingHandler`:
   - `GET /api/staging/status` — check `.web_staging/status.json`
   - `DELETE /api/staging` — clear staging directory
4. Create `ReloadHandler`:
   - `POST /api/reload` — backup, copy, reload, respond
5. Implement backup system (timestamped copies before overwrite)
6. Implement conflict detection (file modification time comparison)

**Deliverable:** Complete backend API. All endpoints functional.

---

### Phase 4: Frontend Scaffold + Auth (Week 4)

**Objective:** Vue SPA loads, login page works, dashboard displays
skill list.

**Tasks:**

1. Initialize Vue 3 + Vite project
2. Install PrimeVue 4 + PrimeFlex + Pinia + Vue Router
3. Configure Vite for production build
4. Configure Gradle to build frontend and bundle into resources
5. Create `api/client.ts` — fetch wrapper with auth header injection
6. Create `stores/auth.ts` — Pinia store with session management
7. Build `LoginPage.vue`:
   - PrimeVue `Card` layout
   - `InputText` for username, `Password` for password
   - `Button` to submit
   - Error message display
   - Loading state during auth check
8. Build `DashboardPage.vue`:
   - Fetch skill list from API
   - Display as PrimeVue `Card` grid (icon, name, color indicator, level cap)
   - "Create New Skill" button
   - Pending changes banner (if staging has files)
9. Set up Vue Router with auth guard:
   - No credentials → redirect to `/login`
   - Valid session → allow navigation
10. Theme configuration:
    - PrimeVue Aura theme (modern, clean)
    - Light/dark mode toggle via PrimeVue's built-in theme switching

**Deliverable:** Full login flow works. Dashboard shows skill grid.
Navigation guards protect all routes.

---

### Phase 5: Skill Editor (Weeks 5-6)

**Objective:** Complete skill editing with all form controls,
drag-and-drop, duplicate/delete, evaluator type switching, and
filter builder.

**Tasks:**

**Week 5 — Skill Identity, Display, Progression, XP Sources:**

1. Build `SkillIdentitySection.vue` — id (auto-generated), displayName, maxLevel
2. Build `DisplaySection.vue`:
   - `IconPicker.vue` — autocomplete with Bukkit material list,
     custom model data number input
   - `ColorPicker.vue` — dropdown with color swatches
   - `StylePicker.vue` — dropdown with bar style options
3. Build `ProgressionSection.vue`:
   - `CurveTypeSelector.vue` — dropdown (polynomial, linear, milestone, constant)
   - Dynamic parameter forms per type (validate min/max ranges)
4. Build `XpSourcesSection.vue`:
   - PrimeVue `OrderList` with drag handles
   - "Add XP Source" / "Duplicate" / "Delete" toolbar
   - `XpSourceEditor.vue` with trigger dropdown, filter builder, reward editor
5. Build `FilterBuilder.vue`:
   - `TargetSelector.vue` — autocomplete pulling from:
     - `GET /api/tags` for custom tag suggestions
     - Hardcoded list of common `#minecraft:` tags
     - Bukkit material list
   - `StateSelector.vue` — checkbox group + custom text input
   - `ToolSelector.vue` — autocomplete (same as target but tool-focused)
6. Build `RewardEditor.vue` — evaluator type dropdown + dynamic fields

**Week 6 — Abilities, Mechanics, Feedback:**

7. Build `AbilitiesSection.vue`:
   - PrimeVue `OrderList` with drag handles
   - "Add Ability" / "Duplicate" toolbar
8. Build `AbilityEditor.vue` with PrimeVue `TabView`:
   - **Display tab:** `AbilityDisplayTab.vue` — lore lines as
     editable list (add/remove/reorder lore strings)
   - **Requirements tab:** `RequirementsTab.vue`:
     - Cooldown (number input, seconds)
     - States (multi-checkbox: sneaking, sprinting, in water, on ground)
     - Items (add/remove items with action type (possession/cost),
       tag autocomplete, amount, slot selector, item cooldown)
   - **Mechanics tab:** `MechanicsTab.vue`:
     - PrimeVue `OrderList` — reorderable mechanics
     - "Add Mechanic" button → shows type selector dialog
     - `MechanicEditor.vue`:
       - Type dropdown (populated from mechanic registry)
       - Filter builder (reuses `FilterBuilder.vue`)
       - Dynamic parameter editor per mechanic type
         (e.g., `chain_limit` shows milestones or linear editor;
          `exhaustion` shows linear editor)
   - **Feedback tab:** `FeedbackTab.vue`:
     - Toggle switches for action_bar and chat
     - Message text input (with legacy `&` color code preview)
     - Particles list (add/remove, type dropdown, count, offset, speed, target)
     - Sounds list (add/remove, type dropdown, volume, pitch, target)
9. Save/Cancel buttons at page level — "Save" writes to staging via API,
   "Cancel" discards in-memory changes (with confirmation dialog)

**Deliverable:** Complete skill editor. Users can create, edit, reorder,
duplicate, and delete all sections of a skill definition through an
intuitive form interface.

---

### Phase 6: Tag + Config Editors (Week 7)

**Objective:** Complete tag and config editing pages.

**Tasks:**

1. Build `TagsPage.vue`:
   - List of custom tags with add/delete
   - `TagEditor.vue`:
     - Tag name input (readonly, with `c:` prefix)
     - `MaterialMultiSelect.vue`:
       - Autocomplete search across all Bukkit materials
       - Ability to add `#minecraft:` and `#c:` tag references
       - PrimeVue `Chips` or `MultiSelect` for displaying selections
       - Drag-and-drop reorder of entries within a tag
2. Build `ConfigPage.vue`:
   - Grouped sections matching config.yml structure
   - `ConfigSection.vue` — reusable section wrapper with title/description
   - Form controls per config key:
     - `database.poolSize` → `InputNumber` (min: 1, max: 100)
     - `database.walMode` → `InputSwitch` (readonly documentation note)
     - `bossbar.maxActive` → `InputNumber` (min: 1, max: 10)
     - `bossbar.fadeTicks` → `InputNumber` (min: 0, max: 200)
     - `debouncer.intervalMs` → `InputNumber` (min: 100, max: 5000)
     - `debugLogging` → `InputSwitch` (with warning about log output)
     - `titles.stayDuration` → `InputNumber` (min: 1000, max: 30000, suffix "ms")
     - `globalXpModifier` → `InputNumber` (min: 0.1, max: 100, step: 0.1)
     - `web.*` → `InputSwitch` for enabled, `InputText` for port/username/password
       (with warning that changing web settings requires server restart)
   - "Save Changes" / "Discard Changes" buttons

**Deliverable:** Complete configuration and tag editing.

---

### Phase 7: Polish & Integration Testing (Week 8)

**Objective:** End-to-end workflow works smoothly. Edge cases handled.

**Tasks:**

1. Staged changes workflow end-to-end:
   - Edit skills → "Save" → status shows pending → "Apply & Reload"
   - Verify reload triggers properly
   - Verify backup is created
   - Verify conflict detection works
2. Error handling:
   - Network errors → show PrimeVue `Toast` with error message
   - Validation errors → inline form validation with `Message` component
   - 401 during session → redirect to login (with toast explaining why)
   - Server errors (500) → show error details in dialog
3. Loading states:
   - Skeleton loaders for skill list, skill editor, tag list
   - Disable buttons during API calls (`loading` prop on PrimeVue buttons)
4. Confirmation dialogs:
   - "Delete skill?" → PrimeVue `ConfirmDialog`
   - "Discard unsaved changes?" → ConfirmDialog
   - "Apply & Reload?" → ConfirmDialog explaining the process
5. Responsive design:
   - Grid adjusts from 4 columns (desktop) → 2 (tablet) → 1 (mobile)
   - Forms stack vertically on narrow screens
   - PrimeFlex utility classes for responsive layout
6. Light/dark theme:
   - Theme toggle in topbar
   - PrimeVue Aura theme for both modes
   - Persist preference in `localStorage`
7. Gradle build finalization:
   - Ensure frontend build runs before `processResources`
   - Handle case where Node.js isn't installed (skip frontend, warn)
   - Verify shaded JAR starts correctly with web module included

**Deliverable:** Production-ready web GUI. Fully integrated with the
plugin's reload system.

---

## 10. Security Considerations

| Concern | Mitigation |
|---------|-----------|
| **Brute-force auth** | Basic Auth over HTTP is inherently vulnerable. Recommend HTTPS via reverse proxy (nginx, Caddy) in production. Document this in config.yml comments. |
| **Credentials in config.yml** | Password stored in plaintext. Document that this is a local admin tool designed for trusted networks only. |
| **CSRF** | The SPA is a single origin; CORS is configured to only allow the same origin in production. |
| **XSS** | All form input is rendered through Vue's template compiler (safe by default). No `v-html` is used with user input. |
| **Staging file injection** | Skill IDs are validated against a strict regex (`[a-z_]+`) before being used as filenames. No path traversal is possible. |
| **Reload abuse** | POST /api/reload requires authentication. The reload sequence is the same as `/skills reload` — all existing guards apply. |

---

## 11. Out of Scope

The following are intentionally excluded from this issue to keep scope
manageable:

- **WebSocket-based real-time updates** (e.g., live console output during
  reload). The current design uses a request-response model.
- **Multiple admin users** or role-based permissions. Single admin account.
- **HTTPS support** — users are expected to reverse-proxy if needed.
- **Internationalization** — English only for v1.
- **Mobile app** — browser-only for v1.
- **Export/import** of skills as YAML files from the browser. Users edit
  files directly via the GUI, which writes to the server's filesystem.
- **Audit log** of who made what changes and when.

---

## 12. Open Questions

The following should be resolved before Phase 1 begins:

1. **YAML library for serialization:** Should we use SnakeYAML directly
   (more control over formatting) or continue with Bukkit's
   `YamlConfiguration` (inconsistent with nested skill definitions)?
   **Recommendation:** SnakeYAML for DTO ↔ YAML conversion, since the
   skill YAML format is already parsed by `SkillManager` via SnakeYAML.

2. **DTO ↔ YAML conversion location:** Should the conversion logic live
   in the `SkillHandler` (convenient but couples API to YAML format) or
   in a separate `SkillSerializer` utility class?
   **Recommendation:** Separate `SkillSerializer` class in
   `io.github.chasehuegel.skilling.web.dto` package.

3. **Frontend build reliability:** The Gradle build will fail if Node.js
   isn't installed. Should we provide a pre-built frontend in the repo
   (committed to git) as a fallback?
   **Recommendation:** Yes, commit the built `dist/` output to git so
   that developers only working on Java can still build the plugin.
