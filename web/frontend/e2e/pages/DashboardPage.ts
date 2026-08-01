import { Page, Locator, expect } from '@playwright/test';
import { ensureLoggedIn } from './shared-login';

export class DashboardPage {
  readonly page: Page;
  readonly skillCards: Locator;
  readonly createBtn: Locator;
  readonly pendingBanner: Locator;
  readonly applyBtn: Locator;
  readonly discardBtn: Locator;

  constructor(page: Page) {
    this.page = page;
    this.skillCards = page.locator('.skill-card');
    this.createBtn = page.locator('.dashboard-header .btn-primary');
    this.pendingBanner = page.locator('.banner');
    this.applyBtn = page.locator('.banner .btn-primary');
    this.discardBtn = page.locator('.banner .btn-ghost');
  }

  async goto() {
    await ensureLoggedIn(this.page);
    await this.page.goto('/#/');
    // The SPA issues post-load API fetches, so Chromium never re-emits the
    // networkIdle lifecycle event and networkidle would hang. load + the
    // element waits used by callers cover data readiness deterministically.
    await this.page.waitForLoadState('load');
  }

  async clickSkill(id: string) {
    await this.skillCards.first().waitFor({ state: 'visible', timeout: 10000 });
    const card = this.skillCards.filter({ hasText: id }).first();
    await card.click();
  }

  async clickCreateSkill() {
    await this.createBtn.click();
    await this.page.waitForURL(/#\/skills\/new/);
  }

  async getSkillCount(): Promise<number> {
    await this.skillCards.first().waitFor({ state: 'visible', timeout: 10000 });
    return this.skillCards.count();
  }

  async assertBannerVisible() {
    await expect(this.pendingBanner).toBeVisible({ timeout: 5000 });
  }

  async assertBannerHidden() {
    await expect(this.pendingBanner).not.toBeVisible({ timeout: 5000 });
  }

  async applyChanges() {
    const reloaded = this.page.waitForResponse(res => res.url().includes('/api/reload'));
    await this.applyBtn.click();
    await reloaded;
  }

  async discardChanges() {
    const discarded = this.page.waitForResponse(
      res => res.url().includes('/api/staging') && res.request().method() === 'DELETE'
    );
    await this.discardBtn.click();
    await discarded;
  }
}
