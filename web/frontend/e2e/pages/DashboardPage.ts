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
    this.createBtn = page.locator('.create-btn');
    this.pendingBanner = page.locator('.banner');
    this.applyBtn = page.locator('.apply-btn');
    this.discardBtn = page.locator('.discard-btn');
  }

  async goto() {
    await ensureLoggedIn(this.page);
    await this.page.goto('/#/');
    await this.page.waitForLoadState('networkidle');
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
    await this.applyBtn.click();
    await this.page.waitForLoadState('networkidle');
  }

  async discardChanges() {
    await this.discardBtn.click();
    await this.page.waitForLoadState('networkidle');
  }
}
