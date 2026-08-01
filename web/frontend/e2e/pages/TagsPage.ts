import { Page, Locator, expect } from '@playwright/test';
import { ensureLoggedIn } from './shared-login';

export class TagsPage {
  readonly page: Page;
  readonly header: Locator;
  readonly tagHeaders: Locator;
  readonly saveBtn: Locator;
  readonly addTagBtn: Locator;

  constructor(page: Page) {
    this.page = page;
    this.header = page.locator('.page-header h1');
    this.tagHeaders = page.locator('.tag-header');
    this.saveBtn = page.getByRole('button', { name: 'Save Changes' });
    this.addTagBtn = page.locator('button:has-text("Add Tag")');
  }

  async goto() {
    await ensureLoggedIn(this.page);
    await this.page.goto('/#/tags');
    await this.page.waitForLoadState('load');
  }

  async getTagNames(): Promise<string[]> {
    await this.tagHeaders.first().waitFor({ state: 'visible', timeout: 10000 });
    return this.tagHeaders.evaluateAll((els) =>
      els.map((el) => el.textContent?.trim() || '')
    );
  }

  async save() {
    const saved = this.page.waitForResponse(res => res.url().includes('/api/tags'));
    await this.saveBtn.click();
    await saved;
  }
}
