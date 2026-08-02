import { test, expect } from '../fixtures';
import { DashboardPage } from '../pages/DashboardPage';
import { SkillEditorPage } from '../pages/SkillEditorPage';

test.describe('Trigger Field Schema', () => {
  test('every ability in the loaded mining skill has a non-blank trigger', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    const abilityCards = page.locator('.ability-card');
    const count = await abilityCards.count();
    expect(count).toBeGreaterThan(0);

    for (let i = 0; i < count; i++) {
      const card = abilityCards.nth(i);
      // Expand the ability card body to reveal the trigger field
      await card.locator('.ability-header').click();

      const triggerInput = page.locator(`input#ci-trigger-${i}`);
      await expect(triggerInput).toBeVisible();
      const value = await triggerInput.inputValue();
      expect(value.trim().length, `ability #${i} has a blank trigger`).toBeGreaterThan(0);
    }
  });
});
