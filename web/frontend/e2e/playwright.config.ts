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
    storageState: '.auth/admin.json',
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
        storageState: '.auth/admin.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'mobile',
      use: {
        viewport: { width: 375, height: 667 },
        storageState: '.auth/admin.json',
      },
      dependencies: ['setup'],
    },
  ],
  globalSetup: './globalSetup.ts',
  globalTeardown: './globalTeardown.ts',
});
