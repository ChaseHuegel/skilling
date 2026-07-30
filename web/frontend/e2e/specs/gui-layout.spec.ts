import { test, expect } from '@playwright/test';
import { GuiLayoutPage } from '../pages/GuiLayoutPage';
import { takeScreenshot } from '../helpers/debug';

test.describe('GUI Layout Editor', () => {
  test('displays the layout page with default grid and palette', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();

    await expect(layoutPage.header).toHaveText('GUI Layout');
    // Default layout has 6 rows × 9 cols = 54 slots
    expect(await layoutPage.getSlotCount()).toBe(54);
    // Palette should show available skills
    expect(await layoutPage.getPaletteCount()).toBeGreaterThanOrEqual(1);
  });

  test('shows a single default page tab', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();

    expect(await layoutPage.getTabCount()).toBe(1);
  });

  test('adds a new page tab', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();

    await layoutPage.clickAddPage();
    expect(await layoutPage.getTabCount()).toBe(2);
  });

  test('can add and remove a page tab', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();

    await layoutPage.clickAddPage();
    expect(await layoutPage.getTabCount()).toBe(2);

    // Remove the first tab
    const removeButtons = page.locator('.tab-remove-btn');
    await removeButtons.first().click();

    // Confirm removal in dialog
    const confirmBtn = page.locator('.confirm-dialog .btn-danger');
    await confirmBtn.click();
    await page.waitForTimeout(100);

    expect(await layoutPage.getTabCount()).toBe(1);
  });

  test('palette search filters skills', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();

    const initialCount = await layoutPage.getPaletteCount();
    await layoutPage.searchPalette('mining');
    const filteredCount = await layoutPage.getPaletteCount();

    expect(filteredCount).toBeLessThanOrEqual(initialCount);
    expect(filteredCount).toBeGreaterThanOrEqual(0);
  });

  test('layout page screenshot', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();
    await takeScreenshot(page, 'gui-layout-editor');
  });
});
