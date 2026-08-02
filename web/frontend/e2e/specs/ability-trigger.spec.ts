import { test, expect } from '../fixtures';
import { BASIC_AUTH } from '../helpers/credentials';
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
          requirements: { cooldown: { type: 'constant', params: { value: 0 } }, state: [], items: [] },
          mechanics: [],
          onFailure: { reasons: {} },
          feedback: { actionBar: false, chat: false, message: '', particles: [], sounds: [] },
        },
      ],
      levelUpCommands: [],
    };

    const authHeaders = {
      Authorization: BASIC_AUTH,
    };

    const created = await request.post('/api/skills', { data: payload, headers: authHeaders });
    expect(created.ok()).toBeTruthy();

    const fetched = await request.get(`/api/skills/${skillId}`, { headers: authHeaders });
    expect(fetched.ok()).toBeTruthy();
    const body = await fetched.json();
    expect(body.abilities.length).toBeGreaterThan(0);
    expect(body.abilities[0].trigger).toBe('block_break');

    // Clean up the staged skill so the round trip doesn't leave a pending
    // change that would surface the "Apply & Reload" banner for later tests.
    const deleted = await request.delete(`/api/skills/${skillId}`, { headers: authHeaders });
    expect(deleted.ok()).toBeTruthy();
  });
});