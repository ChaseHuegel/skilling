import { chromium, FullConfig } from '@playwright/test';
import { spawn } from 'child_process';
import path from 'path';
import fs from 'fs';

const PROJECT_ROOT = path.resolve(__dirname, '../..');
const RUN_DIR = path.resolve(PROJECT_ROOT, 'run/plugins/Skilling');
const FIXTURES = path.resolve(__dirname, 'test-data');
const AUTH_FILE = path.resolve(__dirname, '../.auth/admin.json');
const PID_FILE = path.resolve(__dirname, '../.server.pid');
const SERVER_URL = process.env.SKILLING_SERVER_URL || 'http://localhost:8082';

async function globalSetup(_config: FullConfig) {
  // 1. Prepare fixture data in the Paper dev server plugin directory
  const skillsDest = path.resolve(RUN_DIR, 'skills');
  fs.mkdirSync(skillsDest, { recursive: true });

  const srcSkills = path.resolve(FIXTURES, 'skills');
  if (fs.existsSync(srcSkills)) {
    for (const f of fs.readdirSync(srcSkills)) {
      fs.copyFileSync(path.resolve(srcSkills, f), path.resolve(skillsDest, f));
    }
  }

  for (const file of ['config.yml', 'tags.yml']) {
    const src = path.resolve(FIXTURES, file);
    if (fs.existsSync(src)) {
      fs.copyFileSync(src, path.resolve(RUN_DIR, file));
    }
  }

  // 2. Start the Paper dev server if no external URL is provided
  if (!process.env.SKILLING_SERVER_URL) {
    console.log('Starting Paper dev server...');
    const gradleCmd = path.resolve(PROJECT_ROOT, 'gradlew');
    const proc = spawn(gradleCmd, ['runServer', '--no-daemon'], {
      cwd: PROJECT_ROOT,
      stdio: 'pipe',
      detached: true,
      shell: true,
    });

    // Save PID so globalTeardown can kill it
    if (proc.pid !== undefined) {
      fs.writeFileSync(PID_FILE, String(proc.pid));
    }

    // Capture server output for debugging
    const logFile = path.resolve(__dirname, '../server-output.log');
    const logStream = fs.createWriteStream(logFile, { flags: 'a' });

    if (proc.stdout) {
      proc.stdout.on('data', (data: Buffer) => {
        logStream.write(data);
        const line = data.toString();
        if (line.includes('[Skilling] Web GUI started')) {
          console.log('✓ Web GUI started');
        }
        if (line.includes('[Skilling] Skilling')) {
          console.log('✓ Plugin enabled');
        }
      });
    }
    if (proc.stderr) {
      proc.stderr.on('data', (data: Buffer) => {
        logStream.write(data);
      });
    }

    proc.on('exit', (code) => {
      console.log(`Server process exited with code ${code}`);
      logStream.end();
    });

    // 3. Wait for Web GUI to be ready
    console.log('Waiting for Web GUI to be ready...');
    const maxRetries = 120; // ~4 minutes
    for (let i = 0; i < maxRetries; i++) {
      try {
        const res = await fetch(`${SERVER_URL}/api/health`);
        if (res.ok) {
          console.log(`✓ Web GUI ready at ${SERVER_URL} (${i + 1}s)`);
          break;
        }
      } catch { /* not ready yet */ }
      await new Promise(r => setTimeout(r, 2000));
      if (i === maxRetries - 1) {
        throw new Error('Server failed to start within timeout (check server-output.log)');
      }
    }
  }

  // 4. Create authenticated storage state via the login page
  const browser = await chromium.launch();
  const context = await browser.newContext({ baseURL: SERVER_URL });
  const page = await context.newPage();

  await page.goto('/login');
  // Wait for login form to render
  await page.waitForSelector('#username', { timeout: 10000 });
  await page.fill('#username', 'admin');
  await page.fill('#password', 'skilling');
  await page.click('.login-btn');
  // Wait for redirect to dashboard
  await page.waitForURL('**/');
  await context.storageState({ path: AUTH_FILE });
  await browser.close();
  console.log('✓ Auth storage state saved');
}

export default globalSetup;
