import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { WEB_USERNAME, WEB_PASSWORD } from '../helpers/credentials';

test.describe('Authentication', () => {
  test('login with valid credentials redirects to dashboard', async ({ page }) => {
    const login = new LoginPage(page);
    await login.goto();
    await login.login(WEB_USERNAME, WEB_PASSWORD);
    await expect(page).toHaveURL(/#\/$/);
    await expect(page.locator('.skill-grid')).toBeVisible({ timeout: 5000 });
  });

  test('login with invalid credentials shows error', async ({ page }) => {
    const login = new LoginPage(page);
    await login.goto();
    await login.login('admin', 'wrongpassword');
    await login.assertError('Invalid');
  });

  test('redirects to login when session expires', async ({ page }) => {
    // Clear session storage while on the dashboard
    await page.goto('/#/');
    await page.evaluate(() => sessionStorage.clear());
    await page.reload();
    await expect(page).toHaveURL(/\/login/);
  });

  test('a 401 response logs the user out and redirects to login', async ({ page }) => {
    // Simulate session expiry: every skills-list request returns 401
    await page.route('**/api/skills', route => {
      route.fulfill({ status: 401, contentType: 'application/json', body: '{"status":"error","message":"Unauthorized"}' });
    });

    await page.goto('/#/');

    // The store logged out, the router redirected, and credentials are cleared
    await expect(page).toHaveURL(/#\/login/);
    await expect(page.locator('#username')).toBeVisible();
    // Topbar and pending-changes banner are hidden on the login page
    await expect(page.locator('nav a')).toHaveCount(0);
    const creds = await page.evaluate(() => sessionStorage.getItem('skilling_credentials'));
    expect(creds).toBeNull();
  });
});
