import { Page } from '@playwright/test';

/**
 * Ensures the page is logged in. If the login page is displayed,
 * fills in admin credentials and submits.
 */
export async function ensureLoggedIn(page: Page): Promise<void> {
  // Navigate to root to trigger auth guard
  await page.goto('/#/');
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(500);

  // Check if we're on the login page (auth guard redirected)
  const loginBtn = page.locator('.login-btn');
  if (await loginBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
    await page.fill('#username', 'admin');
    await page.fill('#password', 'skilling');
    await loginBtn.click();
    await page.waitForURL(/#\/$/);
  }
}
