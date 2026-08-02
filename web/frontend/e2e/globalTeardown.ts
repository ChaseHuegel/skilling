import { FullConfig } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const PID_FILE = path.resolve(__dirname, '../.server.pid');

// Marker in the Paper server JVM command line started by ./gradlew runServer
// (xyz.jpenilla.run-paper forked it with -Dxyz.jpenilla.run-task=true).
const RUN_TASK_MARKER = 'run-task=true';

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/** Finds PIDs of the Paper server JVM started by the runServer task. */
function findServerPids(): number[] {
  try {
    const procs = fs.readdirSync('/proc');
    const pids: number[] = [];
    for (const entry of procs) {
      if (!/^\d+$/.test(entry)) continue;
      try {
        const cmdline = fs.readFileSync(`/proc/${entry}/cmdline`, 'utf8');
        if (cmdline.includes(RUN_TASK_MARKER)) pids.push(Number(entry));
      } catch { /* process vanished */ }
    }
    return pids;
  } catch {
    return [];
  }
}

async function killProcesses(pids: number[]): Promise<void> {
  for (const pid of pids) {
    try { process.kill(pid, 'SIGTERM'); } catch { /* already gone */ }
  }
  await sleep(3000);
  for (const pid of pids) {
    try { process.kill(pid, 'SIGKILL'); } catch { /* already gone */ }
  }
}

async function globalTeardown(_config: FullConfig) {
  if (process.env.SKILLING_SERVER_URL) {
    return;
  }

  // The Gradle wrapper (recorded PID) plus any run-server JVM. The server JVM
  // is forked into its own process group, so group-killing the wrapper is not
  // enough; kill the server process directly as well.
  const pids: number[] = [];
  if (fs.existsSync(PID_FILE)) {
    const pid = parseInt(fs.readFileSync(PID_FILE, 'utf-8').trim(), 10);
    if (Number.isFinite(pid)) {
      pids.push(pid);
      try { process.kill(-pid, 'SIGTERM'); } catch { /* group already gone */ }
    }
    try { fs.unlinkSync(PID_FILE); } catch {}
  }
  pids.push(...findServerPids());
  await killProcesses([...new Set(pids)]);
  if (pids.length > 0) {
    console.log(`Stopped server processes: ${pids.join(', ')}`);
  }
}

export default globalTeardown;
