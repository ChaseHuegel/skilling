import { test, expect } from '@playwright/test';
import { DashboardPage } from '../pages/DashboardPage';
import { SkillEditorPage } from '../pages/SkillEditorPage';
import { takeScreenshot } from '../helpers/debug';

test.describe('Staged Changes Workflow', () => {
  test.beforeEach(async ({ page }) => {
    // Ensure a staged change exists by editing a skill
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.setDisplayName('Mining E2E Test');
    await editor.save();
    await dashboard.assertBannerVisible();
  });

  test('discard removes pending changes', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.assertBannerVisible();

    await dashboard.discardChanges();
    await dashboard.assertBannerHidden();
  });
});
