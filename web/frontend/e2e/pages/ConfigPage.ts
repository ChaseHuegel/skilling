import { Page, Locator, expect } from '@playwright/test';

export class ConfigPage {
  readonly page: Page;
  readonly header: Locator;
  readonly sections: Locator;
  readonly saveBtn: Locator;
  readonly resetBtn: Locator;

  constructor(page: Page) {
    this.page = page;
    this.header = page.locator('.page-header h1');
    this.sections = page.locator('.config-section');
    this.saveBtn = page.locator('.btn-primary');
    this.resetBtn = page.locator('.btn-secondary');
  }

  async goto() {
    await this.page.goto('/config');
    await this.page.waitForLoadState('networkidle');
  }

  async getSectionCount(): Promise<number> {
    return this.sections.count();
  }

  async getCheckbox(label: string): Promise<boolean> {
    const row = this.page.locator('.field-row', { hasText: label });
    const cb = row.locator('input[type="checkbox"]');
    return cb.isChecked();
  }

  async toggleCheckbox(label: string) {
    const row = this.page.locator('.field-row', { hasText: label });
    const cb = row.locator('input[type="checkbox"]');
    await cb.click();
  }

  async getNumberField(label: string): Promise<number> {
    const row = this.page.locator('.field-row', { hasText: label });
    const input = row.locator('input[type="number"]');
    return parseInt(await input.inputValue(), 10);
  }

  async setNumberField(label: string, value: number) {
    const row = this.page.locator('.field-row', { hasText: label });
    const input = row.locator('input[type="number"]');
    await input.fill(String(value));
  }

  async save() {
    await this.saveBtn.click();
    await this.page.waitForLoadState('networkidle');
  }
}
