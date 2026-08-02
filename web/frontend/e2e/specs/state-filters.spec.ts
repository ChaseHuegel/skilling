import { test, expect } from '../fixtures';
import { DashboardPage } from '../pages/DashboardPage';
import { SkillEditorPage } from '../pages/SkillEditorPage';

test.describe('State Filter Suggestions', () => {
  test('state filter suggestions include equipped_all/equipped_any when API is reachable', async ({ page }) => {
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
    // combobox datalist, so the registered `equipped_all` / `equipped_any` keys
    // must be offered. Multiple state comboboxes can share the datalist id, so
    // assert presence across all of them rather than a single instance.
    const equippedAllOptions = page.locator('datalist[id^="cl-state-"] option[value="equipped_all"]');
    await expect.poll(async () => await equippedAllOptions.count()).toBeGreaterThanOrEqual(1);
    const equippedAnyOptions = page.locator('datalist[id^="cl-state-"] option[value="equipped_any"]');
    await expect.poll(async () => await equippedAnyOptions.count()).toBeGreaterThanOrEqual(1);
  });
});
