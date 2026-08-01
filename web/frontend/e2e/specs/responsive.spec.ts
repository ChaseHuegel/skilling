import { test } from '@playwright/test';
import { ensureLoggedIn } from '../pages/shared-login';
import { takeScreenshot } from '../helpers/debug';

test.describe('Responsive Layout', () => {
  test('desktop dashboard layout (1280x720)', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    await ensureLoggedIn(page);
    await page.goto('/#/');
    await page.waitForLoadState('load');
    await page.waitForSelector('.skill-card', { timeout: 15000 });
    await takeScreenshot(page, 'responsive-desktop');
  });

  test('mobile dashboard layout (375x667)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await ensureLoggedIn(page);
    await page.goto('/#/');
    await page.waitForLoadState('load');
    await page.waitForSelector('.skill-card', { timeout: 15000 });
    await takeScreenshot(page, 'responsive-mobile');
  });

  test('editor page on tablet (768x1024)', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await ensureLoggedIn(page);
    await page.goto('/#/skills/new');
    await page.waitForLoadState('load');
    await page.waitForSelector('.field-input', { timeout: 15000 });
    await takeScreenshot(page, 'responsive-tablet-editor');
  });
});
