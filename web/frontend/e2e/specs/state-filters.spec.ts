import { test, expect } from '@playwright/test';
import { DashboardPage } from '../pages/DashboardPage';
import { SkillEditorPage } from '../pages/SkillEditorPage';

test.describe('State Filter Suggestions', () => {
  test('state filter suggestions include equipped when API is reachable', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    // Expand the first ability card to reveal the requirement states
    const firstCard = page.locator('.ability-card').first();
    await firstCard.locator('.ability-header').click();

    // The Requirements section (and its States combobox) is expanded by default.
    // The dynamic state-filter list from GET /api/state-filters feeds the
    // combobox datalist, so the registered `equipped` key must be offered.
    // Multiple state comboboxes can share the datalist id, so assert presence
    // across all of them rather than a single instance.
    const equippedOptions = page.locator('datalist[id^="cl-state-"] option[value="equipped"]');
    await expect.poll(async () => await equippedOptions.count()).toBeGreaterThanOrEqual(1);
  });
});
