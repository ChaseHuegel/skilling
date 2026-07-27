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
  readonly iconInput: Locator;
  readonly colorSelect: Locator;
  readonly styleSelect: Locator;

  // Progression fields
  readonly curveSelect: Locator;
  readonly baseXpInput: Locator;
  readonly exponentInput: Locator;

  constructor(page: Page) {
    this.page = page;
    this.header = page.locator('.editor-header h1');
    this.saveBtn = page.locator('.btn-primary');
    this.cancelBtn = page.locator('.btn-secondary');
    this.errorBanner = page.locator('.error-banner');

    // Identity
    this.idInput = page.locator('input[placeholder*="e.g."]');
    this.displayNameInput = page.locator('input[placeholder="Display Name"]');
    this.maxLevelInput = page.locator('input[type="number"]').first();

    // Display
    this.iconInput = page.locator('input[placeholder*="minecraft:"]');
    this.colorSelect = page.locator('select').first();
    this.styleSelect = page.locator('select').nth(1);

    // Progression
    this.curveSelect = page.locator('select').nth(2);
    this.baseXpInput = page.locator('input[type="number"]').nth(1);
    this.exponentInput = page.locator('input[type="number"]').nth(2);
  }

  async isNewSkill(): Promise<boolean> {
    const text = await this.header.textContent();
    return text?.includes('Create Skill') ?? false;
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
    await this.iconInput.fill(icon);
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
    await this.saveBtn.click();
    await this.page.waitForLoadState('networkidle');
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
    await this.page.waitForLoadState('networkidle');
  }

  async assertNoError() {
    await expect(this.errorBanner).not.toBeVisible({ timeout: 3000 });
  }
}
