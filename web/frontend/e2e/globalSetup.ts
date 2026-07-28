import { chromium, FullConfig } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const AUTH_FILE = path.resolve(__dirname, '.auth/admin.json');
const SERVER_URL = process.env.SKILLING_SERVER_URL || 'http://localhost:8082';

async function globalSetup(_config: FullConfig) {
  // Wait for Web GUI to be ready
  console.log(`Connecting to Web GUI at ${SERVER_URL}...`);
  const maxRetries = 60;
  for (let i = 0; i < maxRetries; i++) {
    try {
      const res = await fetch(`${SERVER_URL}/api/health`);
      if (res.ok) {
        console.log(`  Web GUI ready (${i + 1}s)`);
        break;
      }
    } catch { /* not ready yet */ }
    await new Promise(r => setTimeout(r, 2000));
    if (i === maxRetries - 1) {
      throw new Error(`Server not ready at ${SERVER_URL} after ${maxRetries * 2}s`);
    }
  }

  // Create authenticated storage state via the login page
  const browser = await chromium.launch();
  const context = await browser.newContext({ baseURL: SERVER_URL });
  const page = await context.newPage();

  await page.goto('/#/login');
  await page.waitForSelector('#username', { timeout: 10000 });
  await page.fill('#username', 'admin');
  await page.fill('#password', 'skilling');
  await page.click('.btn-primary');
  await page.waitForURL('**/');
  await context.storageState({ path: AUTH_FILE });
  await browser.close();
  console.log('  Auth storage state saved');
}

export default globalSetup;
