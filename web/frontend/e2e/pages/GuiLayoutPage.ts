import { Page, Locator, expect } from '@playwright/test';
import { ensureLoggedIn } from './shared-login';

export class GuiLayoutPage {
  readonly page: Page;
  readonly header: Locator;
  readonly pageTabs: Locator;
  readonly addPageBtn: Locator;
  readonly applyBtn: Locator;
  readonly resetBtn: Locator;
  readonly chestSlots: Locator;
  readonly paletteItems: Locator;
  readonly paletteSearch: Locator;

  constructor(page: Page) {
    this.page = page;
    this.header = page.locator('.page-title');
    this.pageTabs = page.locator('.page-tab');
    this.addPageBtn = page.locator('.tab-add-btn');
    this.applyBtn = page.getByRole('button', { name: 'Save Changes' });
    this.resetBtn = page.locator('.btn-ghost');
    this.chestSlots = page.locator('.chest-slot');
    this.paletteItems = page.locator('.palette-item');
    this.paletteSearch = page.locator('.search-input');
  }

  async goto() {
    await ensureLoggedIn(this.page);
    await this.page.goto('/#/layout');
    await this.page.waitForLoadState('load');
  }

  async getTabCount(): Promise<number> {
    await this.pageTabs.first().waitFor({ state: 'visible', timeout: 10000 });
    return this.pageTabs.count();
  }

  async getSlotCount(): Promise<number> {
    return this.chestSlots.count();
  }

  async getPaletteCount(): Promise<number> {
    await this.paletteItems.first().waitFor({ state: 'visible', timeout: 10000 });
    return this.paletteItems.count();
  }

  async searchPalette(query: string) {
    await this.paletteSearch.fill(query);
    await this.page.waitForTimeout(200);
  }

  async clickTab(index: number) {
    await this.pageTabs.nth(index).click();
  }

  async clickAddPage(label = 'Test Page') {
    await this.addPageBtn.click();
    await this.page.locator('.modal-dialog .modal-input').first().fill(label);
    await this.page.getByRole('button', { name: 'Add', exact: true }).click();
    await this.pageTabs.first().waitFor({ state: 'visible', timeout: 5000 });
  }

  async removeTab(index: number) {
    await this.pageTabs.nth(index).locator('.tab-action-danger').click();
    await this.page.locator('.modal-dialog').getByRole('button', { name: 'Remove' }).click();
  }

  async clickApply() {
    const applied = this.page.waitForResponse(res => res.url().includes('/api/gui-layout'));
    await this.applyBtn.click();
    await applied;
  }

  async dragPaletteToSlot(skillIndex: number, slotIndex: number) {
    const paletteItem = this.paletteItems.nth(skillIndex);
    const targetSlot = this.chestSlots.nth(slotIndex);

    await paletteItem.dragTo(targetSlot);
    await this.page.waitForTimeout(200);
  }

  async rightClickSlot(slotIndex: number) {
    await this.chestSlots.nth(slotIndex).click({ button: 'right' });
    await this.page.waitForTimeout(100);
  }
}
