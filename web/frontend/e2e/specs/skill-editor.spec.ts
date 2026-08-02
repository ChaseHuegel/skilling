import { test, expect } from '@playwright/test';
import { DashboardPage } from '../pages/DashboardPage';
import { SkillEditorPage } from '../pages/SkillEditorPage';
import { takeScreenshot } from '../helpers/debug';

test.describe('Skill Editor', () => {
  test('loads existing skill fields correctly', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();

    // Should be in edit mode, not create
    expect(await editor.isNewSkill()).toBe(false);
    await editor.assertNoError();

    // Verify identity fields are populated
    const identity = await editor.getIdentity();
    expect(identity.displayName).toBeTruthy();
    expect(identity.maxLevel).toBeTruthy();
  });

  test('modify display name and save creates staged change', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    // Modify display name
    await editor.setDisplayName('Mining Pro');
    await editor.save();

    // The editor reloads in place and the pending changes banner appears
    await dashboard.assertBannerVisible();
  });

  test('creates new skill with all sections', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickCreateSkill();

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    expect(await editor.isNewSkill()).toBe(true);

    // Fill identity
    const skillId = 'test_woodcutting';
    await editor.setId(skillId);
    await editor.setDisplayName('Test Woodcutting');
    await editor.setMaxLevel(50);

    // Fill display
    await editor.setIcon('minecraft:iron_axe');
    await editor.selectColor('GREEN');
    await editor.selectStyle('SEGMENTED_10');

    // Fill progression (defaults are polynomial, baseXp 50, exponent 2.5)
    await editor.setBaseXp(100);
    await editor.setExponent(2.0);

    await editor.save();

    // The editor reloads in place and the pending changes banner appears
    await dashboard.assertBannerVisible();
  });

  test('skill editor screenshot', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await takeScreenshot(page, 'skill-editor-loaded');
  });

  test('edits cooldown and saves it as a numeric value', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    // The mining fixture's vein_miner ability has a numeric cooldown; read the
    // current value so the test is idempotent across runs.
    const before = Number(await editor.getCooldown('vein_miner'));
    expect(Number.isFinite(before)).toBeTruthy();
    const target = before + 1;
    await editor.setCooldown('vein_miner', target);

    // The cooldown stays numeric in the editor, and saving stages the change
    // without error (the pending-changes banner appears).
    expect(await editor.getCooldown('vein_miner')).toBe(String(target));
    await editor.save();
    await dashboard.assertBannerVisible();
  });

  test('lore preview escapes HTML payloads and still renders color codes', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    // Add a lore line carrying an XSS payload plus color/format codes
    const addBtn = page.locator('.lore-actions').getByRole('button', { name: '+ Add Line' });
    await addBtn.click();
    const loreInput = page.locator('.lore-input').last();
    await loreInput.fill('<img src=x onerror="window.__xss=1"> &c&lCOLORED');

    const preview = page.locator('.lore-preview .preview-line').last();
    await expect(preview).toContainText('<img src=x onerror="window.__xss=1">');

    // The injected markup must not create elements
    await expect(preview.locator('img')).toHaveCount(0);
    await expect(page.locator('img[src="x"]')).toHaveCount(0);

    // The onerror handler never fired
    const xssFired = await page.evaluate(() =>
      (window as unknown as { __xss?: number }).__xss
    );
    expect(xssFired).toBeUndefined();

    // Color (&c = red) and format (&l = bold) codes still render. The style
    // attribute is serialized as CSS (e.g. "color: rgb(255, 85, 85)"), so
    // assert on computed styles rather than raw style-attribute substrings.
    const colored = preview.locator('span').filter({ hasText: 'COLORED' });
    await expect(colored).toHaveCSS('color', 'rgb(255, 85, 85)');
    await expect(colored).toHaveCSS('font-weight', '700');
  });
});
