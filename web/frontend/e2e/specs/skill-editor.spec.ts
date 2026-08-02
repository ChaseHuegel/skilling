import { test, expect } from '../fixtures';
import { DashboardPage } from '../pages/DashboardPage';
import { SkillEditorPage } from '../pages/SkillEditorPage';

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

  test('expanded state follows an ability through a drag reorder', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    const geoCard = page.locator('#ability-geologist');
    const prospectorCard = page.locator('#ability-prospector');
    await geoCard.waitFor({ state: 'visible', timeout: 10000 });

    const orderBefore = await page.locator('.ability-card').evaluateAll(cards =>
      cards.map(c => c.id).filter(Boolean)
    );

    // Expand geologist and verify its body is open.
    await geoCard.locator('.ability-header').click();
    await expect(geoCard.locator('.ability-body')).toBeVisible();

    // Reorder geologist over the next ability using the native drag handlers
    // (the cards implement @dragstart/@dragover/@dragend directly).
    await geoCard.dispatchEvent('dragstart');
    await prospectorCard.dispatchEvent('dragover');
    await geoCard.dispatchEvent('dragend');

    // The reorder must have actually moved geologist.
    const orderAfter = await page.locator('.ability-card').evaluateAll(cards =>
      cards.map(c => c.id).filter(Boolean)
    );
    expect(orderAfter).not.toEqual(orderBefore);

    // Expanded state is keyed by the ability's stable identity, so geologist
    // stays expanded at its new position while the row that took its old slot
    // does not inherit the expanded state.
    await expect(geoCard.locator('.ability-body')).toBeVisible();
    const firstCard = page.locator('.ability-card').first();
    if ((await firstCard.getAttribute('id')) !== 'ability-geologist') {
      await expect(firstCard.locator('.ability-body')).not.toBeVisible();
    }
  });

  test('ability lore full preview escapes HTML payloads and renders color codes', async ({ page }) => {
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await dashboard.clickSkill('Mining');

    const editor = new SkillEditorPage(page);
    await editor.waitForLoad();
    await editor.assertNoError();

    // Expand the geologist ability card, then its Lore Lines section.
    const geoCard = page.locator('#ability-geologist');
    await geoCard.locator('.ability-header').click();
    await geoCard.locator('.section-block--lore .section-header').click();
    await geoCard.getByRole('button', { name: '+ Add Lore Line' }).click();

    const loreInput = geoCard.locator('.lore-line-block').last().locator('input');
    await loreInput.fill('<img src=x onerror="window.__xss=1"> &c&lCOLORED');

    // The full preview routes through FormattedText (the same component the
    // tooltip uses): the payload stays literal text, never an element.
    const preview = geoCard.locator('.full-preview-line').last();
    await expect(preview).toContainText('<img src=x onerror="window.__xss=1">');
    await expect(preview.locator('img')).toHaveCount(0);
    await expect(geoCard.locator('img[src="x"]')).toHaveCount(0);

    const xssFired = await page.evaluate(() =>
      (window as unknown as { __xss?: number }).__xss
    );
    expect(xssFired).toBeUndefined();

    const colored = preview.locator('span').filter({ hasText: 'COLORED' });
    await expect(colored).toHaveCSS('color', 'rgb(255, 85, 85)');
    await expect(colored).toHaveCSS('font-weight', '700');
  });

  test('a failed leave-save keeps the user on the page with edits intact', async ({ page }) => {
    // Force every skill PUT to fail so the save cannot succeed
    await page.route('**/api/skills/**', route => {
      if (route.request().method() === 'PUT') {
        route.abort('failed');
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

    await editor.setDisplayName('Mining Save Fail Test');
    await expect(editor.displayNameInput).toHaveValue('Mining Save Fail Test');

    // Attempt to navigate away; the unsaved-changes dialog appears
    await page.locator('nav a', { hasText: 'Skills' }).click();
    await page.locator('.modal h3', { hasText: 'Unsaved changes' }).waitFor({ state: 'visible', timeout: 5000 });

    // Saving while leaving fails (the PUT is aborted)
    await page.getByRole('button', { name: 'Save & Leave' }).click();
    await page.waitForSelector('.error-banner', { timeout: 5000 });

    // The user stays on the editor with the unsaved edit intact
    await expect(editor.displayNameInput).toBeVisible();
    await expect(editor.displayNameInput).toHaveValue('Mining Save Fail Test');
  });
});
