import { test as base, expect } from '@playwright/test';
import type { APIRequestContext } from '@playwright/test';
import { BASIC_AUTH } from '../helpers/credentials';

/**
 * Clears any pending staged changes on the shared dev server so each test starts
 * from the seeded baseline. Tests stage skill/config/tags edits and a mid-suite
 * failure can otherwise leave a pending banner or staged file that cascades into
 * later specs. Because every test resets staging up front, the suite is
 * order-independent and does not rely on serial file ordering or alphabetical
 * worker scheduling.
 */
async function resetStaging(request: APIRequestContext): Promise<void> {
  const res = await request.delete('/api/staging', { headers: { Authorization: BASIC_AUTH } });
  if (!res.ok() && res.status() !== 404) {
    throw new Error(`Failed to reset staging before test (HTTP ${res.status()})`);
  }
}

/**
 * The suite-wide test with an automatic per-test staging reset. Specs import
 * {@link test} and {@link expect} from here instead of '@playwright/test'.
 */
export const test = base.extend({
  resetStaging: [
    async ({ request }, use) => {
      await resetStaging(request);
      await use();
    },
    { auto: true },
  ],
});

export { expect };
