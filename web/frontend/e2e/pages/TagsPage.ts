import { Page, Locator, expect } from '@playwright/test';

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
    this.saveBtn = page.locator('.btn-primary');
    this.addTagBtn = page.locator('button:has-text("Add Tag")');
  }

  async goto() {
    await this.page.goto('/tags');
    await this.page.waitForLoadState('networkidle');
  }

  async getTagNames(): Promise<string[]> {
    return this.tagHeaders.evaluateAll((els) =>
      els.map((el) => el.textContent?.trim() || '')
    );
  }

  async save() {
    await this.saveBtn.click();
    await this.page.waitForLoadState('networkidle');
  }
}
