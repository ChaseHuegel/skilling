import { test, expect } from '../fixtures';
import { DashboardPage } from '../pages/DashboardPage';
import { SkillEditorPage } from '../pages/SkillEditorPage';
import { BASIC_AUTH } from '../helpers/credentials';

test.describe('Referenced base/shared abilities', () => {
  test('a bare id reference opens with a Base/Shared badge and round-trips without spurious overrides', async ({ page }) => {
    // Serve a modified mining skill whose vein_miner entry is a bare reference
    // (id with no trigger, no overrides) so the shared fixture is never mutated.
    const base = await page.request.get('/api/skills/mining', { headers: { Authorization: BASIC_AUTH } });
    const skill = await base.json();
    skill.abilities = [
      { id: 'geologist', display_name: 'Geologist', unlock_level: 1, trigger: 'block_break', display: { lore: [] }, requirements: {}, mechanics: [], feedback: { notify: { action_bar: false } } },
      { id: 'vein_miner' },
    ];

    let captured: any = null;
    await page.route('**/api/skills/mining', route => {
      if (route.request().method() === 'GET') {
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(skill) });
      } else if (route.request().method() === 'PUT') {
        captured = JSON.parse(route.request().postData() || '{}');
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'ok' }) });
      } else {
        route.continue();
      }
    });

    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    // The reference renders with a Base/Shared badge and does not require a trigger.
    const card = page.locator('#ability-vein_miner');
    await card.locator('.ability-header').click();
    await expect(card.locator('.badge-reference')).toBeVisible();
    await expect(card.locator('.reference-note')).toBeVisible();
    // The inherited trigger is shown read-only instead of an editable combobox.
    await expect(card.locator('.reference-inherited-value')).toContainText('block_break');

    // Dirty the page with a skill-level edit so the save banner appears; the
    // reference itself is left untouched.
    await editor.setDisplayName('Mining (edited)');

    // No-op save of the reference: the staged payload must keep the bare reference.
    await editor.save();
    expect(captured).not.toBeNull();
    const vein = captured.abilities.find((a: any) => a.id === 'vein_miner');
    expect(vein.id).toBe('vein_miner');
    expect(vein.trigger).toBeUndefined();
    expect(vein.displayName).toBeUndefined();
    expect(vein.unlockLevel).toBeUndefined();
    expect(vein.mechanics).toBeUndefined();
  });

  test('editing only unlock_level of a reference writes an override without expanding it', async ({ page }) => {
    const base = await page.request.get('/api/skills/mining', { headers: { Authorization: BASIC_AUTH } });
    const skill = await base.json();
    skill.abilities = [{ id: 'vein_miner' }];

    let captured: any = null;
    await page.route('**/api/skills/mining', route => {
      if (route.request().method() === 'GET') {
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(skill) });
      } else if (route.request().method() === 'PUT') {
        captured = JSON.parse(route.request().postData() || '{}');
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'ok' }) });
      } else {
        route.continue();
      }
    });

    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    const card = page.locator('#ability-vein_miner');
    await card.locator('.ability-header').click();
    const unlockRow = card.locator('.field-row', { hasText: 'Unlock Level' });
    await unlockRow.locator('input[type="number"]').fill('30');

    await editor.save();
    expect(captured).not.toBeNull();
    const vein = captured.abilities.find((a: any) => a.id === 'vein_miner');
    expect(vein.id).toBe('vein_miner');
    expect(vein.unlockLevel).toBe(30);
    expect(vein.trigger).toBeUndefined();
    expect(vein.mechanics).toBeUndefined();
  });

  test('a reference to an unregistered id fails validation', async ({ page }) => {
    const base = await page.request.get('/api/skills/mining', { headers: { Authorization: BASIC_AUTH } });
    const skill = await base.json();
    skill.abilities = [{ id: 'vein_miner' }];

    await page.route('**/api/skills/mining', route => {
      if (route.request().method() === 'GET') {
        route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(skill) });
      } else {
        route.continue();
      }
    });

    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    // Point the reference at an id that is not registered in abilities/.
    const card = page.locator('#ability-vein_miner');
    await card.locator('.ability-header').click();
    await card.locator('.field-row', { hasText: 'ID' }).locator('input').fill('nonexistent_ability');

    await editor.saveBtn.click();
    await expect(editor.errorBanner).toContainText("unknown base ability 'nonexistent_ability'");
  });
});
