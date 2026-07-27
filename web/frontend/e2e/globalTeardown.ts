import { FullConfig } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const PID_FILE = path.resolve(__dirname, '../.server.pid');

async function globalTeardown(_config: FullConfig) {
  if (process.env.SKILLING_SERVER_URL) {
    return;
  }
  if (fs.existsSync(PID_FILE)) {
    const pid = parseInt(fs.readFileSync(PID_FILE, 'utf-8').trim(), 10);
    try {
      process.kill(pid, 'SIGTERM');
      console.log(`Server process ${pid} stopped.`);
    } catch {
      // process already gone
    }
    try { fs.unlinkSync(PID_FILE); } catch {}
  }
}

export default globalTeardown;
