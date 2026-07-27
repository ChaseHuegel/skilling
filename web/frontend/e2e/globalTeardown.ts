import { FullConfig } from '@playwright/test';
import fs from 'fs';
import path from 'path';

const PID_FILE = path.resolve(__dirname, '../.server.pid');

async function globalTeardown(_config: FullConfig) {
  if (process.env.SKILLING_SERVER_URL) {
    return; // external server, don't manage lifecycle
  }

  // Kill the Paper dev server if we started it
  if (fs.existsSync(PID_FILE)) {
    const pid = parseInt(fs.readFileSync(PID_FILE, 'utf-8').trim(), 10);
    try {
      process.kill(pid, 'SIGTERM');
      console.log(`Server process ${pid} stopped.`);
    } catch (e: any) {
      if (e.code !== 'ESRCH') {
        console.error('Failed to stop server:', e.message);
      }
    }
    fs.unlinkSync(PID_FILE);
  }
}

export default globalTeardown;
