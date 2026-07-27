import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';

test.describe('Authentication', () => {
  test('login with valid credentials redirects to dashboard', async ({ page }) => {
    const login = new LoginPage(page);
    await login.goto();
    await login.login('admin', 'skilling');
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
});
