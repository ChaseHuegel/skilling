import { Page, Locator, expect } from '@playwright/test';

export class SkillEditorPage {
  readonly page: Page;
  readonly header: Locator;
  readonly saveBtn: Locator;
  readonly cancelBtn: Locator;
  readonly errorBanner: Locator;

  // Identity fields
  readonly idInput: Locator;
  readonly displayNameInput: Locator;
  readonly maxLevelInput: Locator;

  // Display fields
  readonly colorSelect: Locator;
  readonly styleSelect: Locator;

  // Progression fields
  readonly curveSelect: Locator;
  readonly baseXpInput: Locator;
  readonly exponentInput: Locator;

  constructor(page: Page) {
    this.page = page;
    this.header = page.locator('.editor-banner .banner-name');
    this.saveBtn = page.getByRole('button', { name: 'Save Changes' });
    this.cancelBtn = page.getByRole('button', { name: 'Cancel' });
    this.errorBanner = page.locator('.error-banner');

    // Identity
    this.idInput = page.locator('input[placeholder="e.g. mining, woodcutting"]');
    this.displayNameInput = page.locator('input[placeholder="e.g. Mining, Woodcutting"]');
    this.maxLevelInput = page.locator('input[placeholder*="Max Level"], input[max="1000"]');

    // Display
    this.colorSelect = page.locator('select').first();
    this.styleSelect = page.locator('select').nth(1);

    // Progression
    this.curveSelect = page.locator('select').nth(2);
    this.baseXpInput = page.locator('.progression-section .decimal-input').nth(0);
    this.exponentInput = page.locator('.progression-section .decimal-input').nth(1);
  }

  async isNewSkill(): Promise<boolean> {
    const text = await this.header.textContent();
    return text?.includes('New Skill') ?? false;
  }

  async getHeaderText(): Promise<string> {
    return (await this.header.textContent()) || '';
  }

  async setId(id: string) {
    await this.idInput.fill(id);
  }

  async setDisplayName(name: string) {
    await this.displayNameInput.fill(name);
  }

  async setMaxLevel(level: number) {
    await this.maxLevelInput.fill(String(level));
  }

  async setIcon(icon: string) {
    const picker = this.page.locator('.display-section .material-picker');
    await picker.locator('.picker-trigger').click();
    await this.page.locator('.picker-search-input').fill(icon);
    await this.page.locator('.picker-option').filter({ hasText: icon }).first().click();
  }

  async selectColor(color: string) {
    await this.colorSelect.selectOption(color);
  }

  async selectStyle(style: string) {
    await this.styleSelect.selectOption(style);
  }

  async setBaseXp(xp: number) {
    await this.baseXpInput.fill(String(xp));
  }

  async setExponent(exp: number) {
    await this.exponentInput.fill(String(exp));
  }

  async save() {
    const saved = this.page.waitForResponse(
      res => res.url().includes('/api/skills') && (res.request().method() === 'PUT' || res.request().method() === 'POST')
    );
    await this.saveBtn.click();
    await saved;
  }

  // Get identity form values for verification
  async getIdentity() {
    return {
      id: await this.idInput.inputValue(),
      displayName: await this.displayNameInput.inputValue(),
      maxLevel: await this.maxLevelInput.inputValue(),
    };
  }

  // Wait for editor to load
  async waitForLoad() {
    await this.page.waitForLoadState('load');
  }

  async assertNoError() {
    await expect(this.errorBanner).not.toBeVisible({ timeout: 3000 });
  }

  async getCooldown(abilityId: string): Promise<string> {
    const card = this.page.locator(`#ability-${abilityId}`);
    await this.expandAbility(card);
    const input = card.locator('.field-row', { hasText: 'Cooldown (s)' }).locator('input[type="number"]');
    return input.inputValue();
  }

  async setCooldown(abilityId: string, cooldown: number) {
    const card = this.page.locator(`#ability-${abilityId}`);
    await this.expandAbility(card);
    const input = card.locator('.field-row', { hasText: 'Cooldown (s)' }).locator('input[type="number"]');
    await input.fill(String(cooldown));
  }

  private async expandAbility(card: Locator) {
    const body = card.locator('.ability-body');
    if (await body.isVisible()) return;
    await card.locator('.ability-header').click();
    await expect(body).toBeVisible();
  }
}
