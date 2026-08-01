import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './specs',
  fullyParallel: false,
  retries: 1,
  workers: 1,
  timeout: 30000,
  expect: {
    timeout: 10000,
    toHaveScreenshot: {
      maxDiffPixels: 100,
    },
  },
  use: {
    baseURL: 'http://localhost:8082',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    storageState: 'e2e/.auth/admin.json',
    launchOptions: {
      // The skill-card icons fetch textures from raw.githubusercontent.com. In
      // sandboxed/offline environments these keep-alive connections prevent
      // waitForLoadState('networkidle') from ever firing. Resolve the CDN to
      // localhost so the fetches fail fast (the icon component falls back to a
      // letter badge) and the network can reach idle.
      args: ['--host-resolver-rules=MAP raw.githubusercontent.com 127.0.0.1'],
    },
  },
  projects: [
    {
      name: 'setup',
      testMatch: /auth\.setup\.ts/,
    },
    {
      name: 'desktop',
      use: {
        viewport: { width: 1280, height: 720 },
        storageState: 'e2e/.auth/admin.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'mobile',
      use: {
        viewport: { width: 375, height: 667 },
        storageState: 'e2e/.auth/admin.json',
      },
      dependencies: ['setup'],
    },
  ],
  globalSetup: './globalSetup.ts',
  globalTeardown: './globalTeardown.ts',
});
