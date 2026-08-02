import { test, expect } from '../fixtures';
import { ConfigPage } from '../pages/ConfigPage';

test.describe('Config Editor', () => {
  test('displays config sections', async ({ page }) => {
    const config = new ConfigPage(page);
    await config.goto();
    const count = await config.getSectionCount();
    expect(count).toBeGreaterThanOrEqual(1);
  });

  test('shows database pool size from config', async ({ page }) => {
    const config = new ConfigPage(page);
    await config.goto();
    const poolSize = await config.getNumberField('Pool Size');
    expect(poolSize).toBeGreaterThanOrEqual(1);
  });

  test('toggle debug logging saves to staging', async ({ page }) => {
    const config = new ConfigPage(page);
    await config.goto();
    await config.toggleCheckbox('Debug Logging');
    await config.save();
    // After save, should show success (no error banner)
    await expect(page.locator('.error-banner')).not.toBeVisible({ timeout: 3000 });
  });
});
