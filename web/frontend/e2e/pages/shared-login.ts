import { Page } from '@playwright/test';
import { WEB_USERNAME, WEB_PASSWORD } from '../helpers/credentials';

/**
 * Ensures the page is logged in. If the login page is displayed,
 * fills in admin credentials and submits.
 */
export async function ensureLoggedIn(page: Page): Promise<void> {
  // Navigate to root to trigger auth guard
  await page.goto('/#/');
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(500);

  // Check if we're on the login page by looking for the login-title element
  // (unique to the login page — avoids conflicting with .btn-primary on the dashboard)
  const loginTitle = page.locator('.login-title');
  if (await loginTitle.isVisible({ timeout: 1000 }).catch(() => false)) {
    await page.fill('#username', WEB_USERNAME);
    await page.fill('#password', WEB_PASSWORD);
    await page.locator('.btn-primary').click();
    await page.waitForURL(/#\/$/);
  }
}
