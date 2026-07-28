import { Page, Locator, expect } from '@playwright/test';
import { ensureLoggedIn } from './shared-login';

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
    await ensureLoggedIn(this.page);
    await this.page.goto('/#/config');
    await this.page.waitForLoadState('networkidle');
  }

  async getSectionCount(): Promise<number> {
    await this.sections.first().waitFor({ state: 'visible', timeout: 10000 });
    return this.sections.count();
  }

  async getCheckbox(label: string): Promise<boolean> {
    const wrapper = this.page.locator('.app-input', { hasText: label });
    const toggle = wrapper.locator('.checkbox-toggle');
    return toggle.getAttribute('aria-checked').then(v => v === 'true');
  }

  async toggleCheckbox(label: string) {
    const wrapper = this.page.locator('.app-input', { hasText: label });
    const toggle = wrapper.locator('.checkbox-toggle');
    await toggle.click();
  }

  async getNumberField(label: string): Promise<number> {
    const wrapper = this.page.locator('.app-input', { hasText: label });
    const input = wrapper.locator('.app-input-field');
    return parseInt(await input.inputValue(), 10);
  }

  async setNumberField(label: string, value: number) {
    const wrapper = this.page.locator('.app-input', { hasText: label });
    const input = wrapper.locator('.app-input-field');
    await input.fill(String(value));
  }

  async save() {
    await this.saveBtn.click();
    await this.page.waitForLoadState('networkidle');
  }
}
