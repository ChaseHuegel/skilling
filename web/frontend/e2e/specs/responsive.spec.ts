import { test } from '@playwright/test';
import { takeScreenshot } from '../helpers/debug';

test.describe('Responsive Layout', () => {
  test('desktop dashboard layout (1280x720)', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('.skill-card', { timeout: 10000 });
    await takeScreenshot(page, 'responsive-desktop');
  });

  test('mobile dashboard layout (375x667)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('.skill-card', { timeout: 10000 });

    // On mobile, the grid should be single column. Take screenshot for visual review.
    await takeScreenshot(page, 'responsive-mobile');
  });

  test('editor page on tablet (768x1024)', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/skills/new');
    await page.waitForLoadState('networkidle');
    await takeScreenshot(page, 'responsive-tablet-editor');
  });
});
