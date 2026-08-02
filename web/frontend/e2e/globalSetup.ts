import { chromium, FullConfig } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { spawn } from 'child_process';
import { WEB_USERNAME, WEB_PASSWORD } from './helpers/credentials';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const REPO_ROOT = path.resolve(__dirname, '../../..');
const FIXTURES_DIR = path.resolve(__dirname, 'test-data');
const PLUGIN_DIR = path.resolve(REPO_ROOT, 'run/plugins/Skilling');
const PID_FILE = path.resolve(__dirname, '../.server.pid');
const LOG_FILE = path.resolve(__dirname, '../.server.log');
const AUTH_FILE = path.resolve(__dirname, '.auth/admin.json');
const SERVER_URL = process.env.SKILLING_SERVER_URL || 'http://localhost:8082';

function seedFixtures(): void {
  console.log('Seeding fixture data...');
  fs.mkdirSync(PLUGIN_DIR, { recursive: true });

  // Drop runtime state so every run starts from a known blank slate.
  fs.rmSync(path.join(PLUGIN_DIR, 'data.db'), { force: true });
  fs.rmSync(path.join(PLUGIN_DIR, '.web_staging'), { recursive: true, force: true });
  fs.rmSync(path.join(PLUGIN_DIR, 'gui.yml'), { force: true });
  fs.rmSync(path.join(PLUGIN_DIR, 'skills'), { recursive: true, force: true });

  fs.cpSync(path.join(FIXTURES_DIR, 'skills'), path.join(PLUGIN_DIR, 'skills'), { recursive: true });
  fs.copyFileSync(path.join(FIXTURES_DIR, 'config.yml'), path.join(PLUGIN_DIR, 'config.yml'));
  fs.copyFileSync(path.join(FIXTURES_DIR, 'tags.yml'), path.join(PLUGIN_DIR, 'tags.yml'));
  console.log('  Fixtures seeded into', PLUGIN_DIR);
}

async function waitForServer(timeoutMs: number, isDead: () => boolean): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  let attempts = 0;
  while (Date.now() < deadline) {
    attempts++;
    if (isDead()) {
      throw new Error('Server process exited before the Web GUI became ready; see .server.log');
    }
    try {
      const res = await fetch(`${SERVER_URL}/api/health`);
      if (res.ok) {
        console.log(`  Web GUI ready after ${attempts} attempt(s)`);
        return;
      }
    } catch { /* not ready yet */ }
    await new Promise(r => setTimeout(r, 2000));
  }
  throw new Error(`Server not ready at ${SERVER_URL} after ${Math.round(timeoutMs / 1000)}s`);
}

async function startServer(): Promise<void> {
  console.log('Starting Paper dev server (./gradlew runServer)...');
  fs.mkdirSync(path.dirname(LOG_FILE), { recursive: true });
  const logStream = fs.createWriteStream(LOG_FILE, { flags: 'w' });

  const child = spawn('./gradlew', ['runServer', '--console=plain', '--no-daemon'], {
    cwd: REPO_ROOT,
    detached: true,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  child.stdout.pipe(logStream);
  child.stderr.pipe(logStream);

  let exited = false;
  let serverReady = false;
  child.on('exit', (code, signal) => {
    exited = true;
    // Only the unexpected pre-readiness exit is worth surfacing; a teardown
    // kill after the server is up is the normal end of the run.
    if (!process.env.SKILLING_SERVER_URL && !serverReady) {
      console.error(`  Server process exited early (code=${code}, signal=${signal}); see .server.log`);
    }
  });
  child.on('error', err => {
    console.error('  Failed to start server:', err.message);
  });

  fs.writeFileSync(PID_FILE, String(child.pid));
  console.log(`  Server started with PID ${child.pid} (log: ${LOG_FILE})`);

  // First run compiles the plugin, builds the frontend, and downloads Paper, so
  // give the automatic mode a generous budget.
  await waitForServer(420_000, () => exited);
  serverReady = true;
}

async function globalSetup(_config: FullConfig) {
  if (process.env.SKILLING_SERVER_URL) {
    console.log(`External-server mode: using ${SERVER_URL}`);
    await waitForServer(30_000, () => false);
  } else {
    seedFixtures();
    await startServer();
  }

  // Create authenticated storage state via the login page
  const browser = await chromium.launch();
  const context = await browser.newContext({ baseURL: SERVER_URL });
  const page = await context.newPage();

  await page.goto('/#/login');
  await page.waitForSelector('#username', { timeout: 10000 });
  await page.fill('#username', WEB_USERNAME);
  await page.fill('#password', WEB_PASSWORD);
  await page.click('.btn-primary');
  await page.waitForURL('**/');
  await context.storageState({ path: AUTH_FILE });
  await browser.close();
  console.log('  Auth storage state saved');
}

export default globalSetup;
