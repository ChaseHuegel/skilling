import { Page, Locator, expect } from '@playwright/test';
import { ensureLoggedIn } from './shared-login';

export class TagsPage {
  readonly page: Page;
  readonly header: Locator;
  readonly materialSection: Locator;
  readonly entitySection: Locator;
  readonly materialTagHeaders: Locator;
  readonly entityTagHeaders: Locator;
  readonly saveBtn: Locator;
  readonly addTagBtn: Locator;

  constructor(page: Page) {
    this.page = page;
    this.header = page.locator('.page-header h1');
    this.materialSection = page.locator('section.tags-section').nth(0);
    this.entitySection = page.locator('section.tags-section').nth(1);
    this.materialTagHeaders = this.materialSection.locator('.tag-header');
    this.entityTagHeaders = this.entitySection.locator('.tag-header');
    this.saveBtn = page.getByRole('button', { name: 'Save Changes' });
    this.addTagBtn = page.locator('button:has-text("Add Tag")');
  }

  async goto() {
    await this.page.goto('/#/tags');
    await this.page.waitForLoadState('load');
    await ensureLoggedIn(this.page);
  }

  async getMaterialTagNames(): Promise<string[]> {
    await this.materialTagHeaders.first().waitFor({ state: 'visible', timeout: 10000 });
    return this.materialTagHeaders.evaluateAll((els) =>
      els.map((el) => el.textContent?.trim() || '')
    );
  }

  async getEntityTagNames(): Promise<string[]> {
    await this.entityTagHeaders.first().waitFor({ state: 'visible', timeout: 10000 });
    return this.entityTagHeaders.evaluateAll((els) =>
      els.map((el) => el.textContent?.trim() || '')
    );
  }

  async save() {
    const saved = this.page.waitForResponse(res => res.url().includes('/api/tags'));
    await this.saveBtn.click();
    await saved;
  }
}
