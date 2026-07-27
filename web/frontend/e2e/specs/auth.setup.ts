import { test as setup, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';

const AUTH_FILE = path.resolve(__dirname, '../../.auth/admin.json');

setup('authenticate via API and save storage state', async ({ request }) => {
  const res = await request.get('/api/auth/check', {
    headers: {
      Authorization: 'Basic ' + Buffer.from('admin:skilling').toString('base64'),
    },
  });
  expect(res.ok()).toBeTruthy();

  const authDir = path.dirname(AUTH_FILE);
  if (!fs.existsSync(authDir)) fs.mkdirSync(authDir, { recursive: true });

  // Write a minimal storage state so dependent projects can use it.
  // The actual session is maintained via credentials stored in sessionStorage,
  // so we just need the file to exist for Playwright's storageState to load.
  fs.writeFileSync(AUTH_FILE, JSON.stringify({
    cookies: [],
    origins: [
      {
        origin: 'http://localhost:8082',
        localStorage: [],
        sessionStorage: [
          { name: 'skilling_credentials', value: Buffer.from('admin:skilling').toString('base64') },
        ],
      },
    ],
  }));
});
