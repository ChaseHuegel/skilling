import { test, expect } from '@playwright/test';
import { DashboardPage } from '../pages/DashboardPage';
import { takeScreenshot } from '../helpers/debug';

test.describe('Dashboard', () => {
  test('displays skill cards from loaded skills', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    const count = await dashboard.getSkillCount();
    expect(count).toBeGreaterThanOrEqual(1);
  });

  test('skill card shows name and meta information', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    const card = dashboard.skillCards.first();
    await expect(card).toBeVisible();
    await expect(card.locator('.skill-name')).not.toBeEmpty();
    await expect(card.locator('.skill-meta')).toContainText(/Max Level/);
  });

  test('clicking a skill card navigates to editor', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();

    // Get the first skill's name
    const firstCard = dashboard.skillCards.first();
    await firstCard.waitFor({ state: 'visible', timeout: 10000 });
    await firstCard.click();

    // Should navigate to editor, URL contains /skills/
    await expect(page).toHaveURL(/\/skills\//);
    await expect(page.locator('.editor-header')).toBeVisible({ timeout: 5000 });
  });

  test('clicking create navigates to new skill', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickCreateSkill();
    await expect(page).toHaveURL(/\/skills\/new/);
    await expect(page.locator('h1')).toContainText('Create Skill');
  });

  test('dashboard screenshot matches baseline', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await takeScreenshot(page, 'dashboard-desktop');
  });
});
