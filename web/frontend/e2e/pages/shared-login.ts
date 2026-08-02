import { Page, expect } from '@playwright/test';
import { WEB_USERNAME, WEB_PASSWORD } from '../helpers/credentials';

/** Topbar navigation links are present on every authenticated view and absent on the login page. */
const AUTHD_MARKER = 'nav a';
/** Resolve the authenticated marker to a single element (strict-mode safe for toBeVisible). */
const authedLocator = (page: Page) => page.locator(AUTHD_MARKER).first();

/**
 * Ensures the page is logged in without navigating or sleeping. If the auth
 * guard redirected to the login page, fills in admin credentials and submits;
 * otherwise returns immediately. Auto-retries until the app settles into either
 * the login page or an authenticated view (replaces the former fixed 500ms sleep).
 *
 * @param page the Playwright page
 */
export async function ensureLoggedIn(page: Page): Promise<void> {
  const loginTitle = page.locator('.login-title');
  const authed = authedLocator(page);
  await expect(loginTitle.or(authed)).toBeVisible({ timeout: 15000 });

  if (await loginTitle.isVisible().catch(() => false)) {
    await page.fill('#username', WEB_USERNAME);
    await page.fill('#password', WEB_PASSWORD);
    await page.locator('.btn-primary').click();
    await expect(authed).toBeVisible({ timeout: 10000 });
  }
}
