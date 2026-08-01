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

  test('edits cooldown and reloads the saved value', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    // The mining fixture's vein_miner ability has a numeric cooldown; read the
    // current value so the test is idempotent across runs.
    const before = Number(await editor.getCooldown('vein_miner'));
    const target = before + 1;
    await editor.setCooldown('vein_miner', target);
    await editor.save();
    await dashboard.assertBannerVisible();

    // Reload the editor and confirm the cooldown round-trips as a number
    await editor.waitForLoad();
    await editor.assertNoError();
    expect(await editor.getCooldown('vein_miner')).toBe(String(target));
  });
});
