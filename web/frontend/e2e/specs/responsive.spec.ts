import { test, expect } from '../fixtures';
import { ensureLoggedIn } from '../pages/shared-login';

test.describe('Responsive Layout', () => {
  test('desktop dashboard renders skill cards (1280x720)', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto('/#/');
    await page.waitForLoadState('load');
    await ensureLoggedIn(page);

    const cards = page.locator('.skill-card');
    await expect(cards.first()).toBeVisible({ timeout: 15000 });
    expect(await cards.count()).toBeGreaterThanOrEqual(1);
  });

  test('mobile dashboard renders skill cards without horizontal overflow (375x667)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/#/');
    await page.waitForLoadState('load');
    await ensureLoggedIn(page);

    const cards = page.locator('.skill-card');
    await expect(cards.first()).toBeVisible({ timeout: 15000 });
    expect(await cards.count()).toBeGreaterThanOrEqual(1);

    // The narrow layout must not force horizontal page scrolling.
    const horizontalOverflow = await page.evaluate(() =>
      document.documentElement.scrollWidth - document.documentElement.clientWidth
    );
    expect(horizontalOverflow).toBeLessThanOrEqual(4);
  });

  test('editor page renders identity fields on tablet (768x1024)', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/#/skills/new');
    await page.waitForLoadState('load');
    await ensureLoggedIn(page);

    await expect(page.locator('.field-input').first()).toBeVisible({ timeout: 15000 });
    await expect(page.locator('.editor-banner')).toBeVisible({ timeout: 15000 });
  });
});
