# CI/CD Pipeline (GitHub Actions)

## Issue

The project has **zero CI/CD infrastructure**. There is no `.github/` directory, no automated build, no test runner, and no release automation. Every build and test run is performed manually via `./gradlew build` and `./gradlew test`. This creates several risks:

- Pull requests won't automatically verify compilation or test pass
- The frontend (Vue 3 + TypeScript) has no automated type-checking or linting in CI
- Playwright E2E tests exist but have no CI runner configuration
- Releases must be performed entirely by hand (build JAR, create GitHub Release, upload)
- No artifact attestation or reproducibility guarantees

## Scope

Three GitHub Actions workflows:

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `ci.yml` | `push` + `pull_request` on `main` | Build plugin, run tests, lint frontend, type-check frontend |
| `e2e.yml` | `pull_request` on `main` (optional) | Run Playwright E2E tests against Paper dev server |
| `release.yml` | `push` tag `v*` | Build, create GitHub Release, upload shadow JAR |

## Affected Files

| File | Action |
|------|--------|
| `.github/workflows/ci.yml` | Create |
| `.github/workflows/e2e.yml` | Create |
| `.github/workflows/release.yml` | Create |

## Development Plan

### Step 1: Create `ci.yml` — Build & Test

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle.kts') }}

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 22

      - name: Cache npm
        uses: actions/cache@v4
        with:
          path: web/frontend/node_modules
          key: ${{ runner.os }}-npm-${{ hashFiles('web/frontend/package-lock.json') }}

      - name: Install frontend dependencies
        working-directory: web/frontend
        run: npm ci

      - name: Type-check frontend
        working-directory: web/frontend
        run: npx vue-tsc --noEmit

      - name: Lint frontend
        working-directory: web/frontend
        run: npx eslint src/

      - name: Build frontend
        working-directory: web/frontend
        run: npm run build

      - name: Build plugin with Gradle
        run: ./gradlew build --no-daemon

      - name: Run tests
        run: ./gradlew test --no-daemon

      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: build/reports/tests/

      - name: Upload shadow JAR
        uses: actions/upload-artifact@v4
        with:
          name: skilling-jar
          path: build/libs/skilling-*.jar
```

Key decisions:
- `vue-tsc --noEmit` catches TypeScript errors without emitting compiled JS
- `eslint` enforces code style (requires an ESLint config; if none exists, add a baseline)
- Gradle `--no-daemon` prevents background daemon leak in CI
- Test report and JAR uploaded as artifacts for inspection/download

### Step 2: Create `e2e.yml` — Playwright End-to-End Tests

```yaml
name: E2E

on:
  pull_request:
    branches: [main]

jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 22

      - name: Cache npm
        uses: actions/cache@v4
        with:
          path: web/frontend/node_modules
          key: ${{ runner.os }}-npm-${{ hashFiles('web/frontend/package-lock.json') }}

      - name: Install frontend dependencies
        working-directory: web/frontend
        run: npm ci

      - name: Build frontend
        working-directory: web/frontend
        run: npm run build

      - name: Build plugin
        run: ./gradlew shadowJar --no-daemon

      - name: Install Playwright browsers
        working-directory: web/frontend
        run: npx playwright install chromium

      - name: Run E2E tests
        working-directory: web/frontend
        run: npm run e2e
        env:
          CI: true
```

**Considerations:**
- The E2E tests start an actual Paper dev server via `globalSetup.ts` — this needs a vanilla server JAR download (Mojang's manifest), which requires network access
- CI doesn't have a display, so Playwright runs in headless mode by default
- May need `xvfb-run` for headed mode debugging artifacts
- Timeout should be generous (120s per test) since Paper server startup is slow
- Downside: E2E tests add ~5-8 minutes to CI per run. Consider running only on label-trigger or manual dispatch if this is too heavy.

### Step 3: Create `release.yml` — Automated Release

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 22

      - name: Install frontend dependencies
        working-directory: web/frontend
        run: npm ci

      - name: Build frontend
        working-directory: web/frontend
        run: npm run build

      - name: Build plugin
        run: ./gradlew build --no-daemon

      - name: Run tests
        run: ./gradlew test --no-daemon

      - name: Extract version from tag
        id: get_version
        run: echo "VERSION=${GITHUB_REF#refs/tags/v}" >> $GITHUB_OUTPUT

      - name: Generate release notes
        id: release_notes
        run: |
          git log --oneline --no-decorate $(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || git rev-list --max-parents=0 HEAD)..HEAD > /tmp/release-notes.txt
          echo "notes=$(cat /tmp/release-notes.txt)" >> $GITHUB_OUTPUT

      - name: Create Release
        uses: softprops/action-gh-release@v2
        with:
          name: Skilling v${{ steps.get_version.outputs.VERSION }}
          body_path: /tmp/release-notes.txt
          files: |
            build/libs/skilling-*.jar
          draft: false
          prerelease: false
```

**Considerations:**
- Tags must match `v*` pattern (e.g., `v1.0.0`, `v1.1.0`)
- Version in `gradle.properties` and `paper-plugin.yml` should be managed alongside tags
- Consider adding a `CHANGELOG.md` for manual changelog entries augmenting the auto-generated notes
- Release JAR is the shadow JAR from `build/libs/`
- GitHub Release body uses commit log between last tag and current tag

### Step 4: ESLint / Linter Baseline Setup

Before `ci.yml` can run ESLint, the frontend needs a linter configuration. If none exists:

```bash
cd web/frontend && npx eslint --init
```

Recommended config: flat config (`eslint.config.js`) with TypeScript support using `typescript-eslint`. If the project already uses ESLint, verify the config is compatible with the Action setup.

### Step 5: Verify Workflow

1. Create the workflow files and push to a feature branch
2. Open a draft PR to trigger `ci.yml` and verify all steps pass
3. Test tag push on a fork to validate `release.yml` (dry run)
4. Fix any issues: missing ESLint config, test failures, frontend type errors

## Testing

- **CI workflow:** Verified by pushing to a PR branch — GitHub Actions runs build, test, lint, type-check
- **Release workflow:** Verified by pushing a `v*` tag to a fork — GitHub Release created with JAR attached
- **E2E workflow:** Verified by pushing a PR — Playwright tests pass against Paper dev server

## Self-Review

- CI setup is standard for Java + Vue projects — no exotic dependencies
- The Gradle daemon is disabled (`--no-daemon`) to prevent zombie processes in CI
- Node.js version (22) matches current LTS
- Actions use pinned major versions (`v4`) for stability
- The `e2e.yml` workflow is separate from `ci.yml` because it's slow and may fail due to Paper server startup flakiness
- Release workflow should only fire on version tags — use `softprops/action-gh-release` for GitHub Release creation
- No secrets or credentials are needed for build/test workflows; release workflow may need `GITHUB_TOKEN` (auto-provided)

## Future Considerations

- **Hangar/Modrinth publishing:** The release workflow could be extended to publish to PaperMC Hangar and Modrinth via their respective GitHub Actions (`hangar-publish-action`, `modrinth-publish-action`)
- **Multi-version matrix:** If the plugin targets multiple Paper API versions, a matrix strategy could build against each
- **Code coverage:** Integrate JaCoCo or similar for coverage reporting (currently there's no coverage tool in the build)
- **Dependency auditing:** Add `npm audit` and `gradle dependencyCheck` for vulnerability scanning
