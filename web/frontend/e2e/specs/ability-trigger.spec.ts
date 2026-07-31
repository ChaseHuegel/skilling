import { test, expect } from '@playwright/test';
import { DashboardPage } from '../pages/DashboardPage';
import { SkillEditorPage } from '../pages/SkillEditorPage';

test.describe('Ability Trigger Field', () => {
  test('trigger combobox is present and populated for existing ability', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    // Expand the first ability card to reveal the trigger field
    const firstCard = page.locator('.ability-card').first();
    await firstCard.locator('.ability-header').click();

    const triggerInput = page.locator('input#ci-trigger-0');
    await expect(triggerInput).toBeVisible();
    // Geologist ability binds to the block_break trigger
    await expect(triggerInput).toHaveValue('block_break');
  });

  test('round-trip create preserves trigger field', async ({ page, request }) => {
    const skillId = 'trigger_roundtrip_test';
    const payload = {
      id: skillId,
      displayName: 'Trigger Roundtrip',
      maxLevel: 10,
      icon: 'minecraft:stick',
      customModelData: 0,
      color: 'WHITE',
      style: 'SOLID',
      progression: { curve: 'constant', baseXp: 100, exponent: 2.5 },
      xpSources: [],
      abilities: [
        {
          id: 'sample',
          displayName: 'Sample',
          unlockLevel: 1,
          trigger: 'block_break',
          display: { lore: [] },
          requirements: { cooldown: 0, state: [], items: [] },
          mechanics: [],
          onFailure: { reasons: {} },
          feedback: { actionBar: false, chat: false, message: '', particles: [], sounds: [] },
        },
      ],
      levelUpCommands: [],
    };

    const created = await request.post('/api/skills', { data: payload });
    expect(created.ok()).toBeTruthy();

    const fetched = await request.get(`/api/skills/${skillId}`);
    expect(fetched.ok()).toBeTruthy();
    const body = await fetched.json();
    expect(body.abilities.length).toBeGreaterThan(0);
    expect(body.abilities[0].trigger).toBe('block_break');
  });
});