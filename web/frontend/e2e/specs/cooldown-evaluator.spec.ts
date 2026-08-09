import { test, expect } from '../fixtures';
import { DashboardPage } from '../pages/DashboardPage';
import { SkillEditorPage } from '../pages/SkillEditorPage';
import { BASIC_AUTH } from '../helpers/credentials';

test.describe('Cooldown evaluator', () => {
  test('linear cooldown requirement renders and round-trips through the editor', async ({ page }) => {
    // Serve a modified mining skill whose vein_miner ability has a linear
    // cooldown (intercepted locally) so the shared fixture is never mutated.
    const base = await page.request.get('/api/skills/mining', { headers: { Authorization: BASIC_AUTH } });
    const skill = await base.json();
    const vein = skill.abilities.find((a: any) => a.id === 'vein_miner');
    vein.requirements.cooldown = { type: 'linear', params: { base: 5, step: -0.02, min: 0, max: 1 } };

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

    // Expand vein_miner: the dynamic cooldown must render as an evaluator
    // (type selector), not the plain scalar number input, so it cannot be
    // collapsed to 0.
    const card = page.locator('#ability-vein_miner');
    await card.locator('.ability-header').click();
    await expect(card.locator('.cooldown-evaluator')).toBeVisible();
    await expect(card.locator('.cooldown-evaluator .evaluator-type-select')).toHaveValue('linear');

    // Dirty the page with a skill-level edit so the save banner appears; the
    // linear cooldown block itself is left untouched.
    await editor.setDisplayName('Mining (edited)');

    await editor.save();

    // The staged payload keeps the linear cooldown block intact.
    expect(captured).not.toBeNull();
    const cooldown = captured.abilities.find((a: any) => a.id === 'vein_miner').requirements.cooldown;
    expect(cooldown.type).toBe('linear');
    expect(cooldown.params.base).toBe(5);
    expect(cooldown.params.step).toBe(-0.02);
    expect(cooldown.params.max).toBe(1);
  });
});
