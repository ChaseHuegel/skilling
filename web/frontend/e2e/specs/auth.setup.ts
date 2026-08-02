import { test as setup, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { WEB_USERNAME, WEB_PASSWORD } from '../helpers/credentials';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const AUTH_FILE = path.resolve(__dirname, '../.auth/admin.json');
const SERVER_URL = process.env.SKILLING_SERVER_URL || 'http://localhost:8082';
const CREDENTIALS = Buffer.from(`${WEB_USERNAME}:${WEB_PASSWORD}`).toString('base64');

setup('authenticate via API and save storage state', async ({ request }) => {
  const res = await request.get(`${SERVER_URL}/api/auth/check`, {
    headers: {
      Authorization: `Basic ${CREDENTIALS}`,
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
        origin: SERVER_URL,
        localStorage: [],
        sessionStorage: [
          { name: 'skilling_credentials', value: CREDENTIALS },
        ],
      },
    ],
  }));
});
