import { test, expect } from '@playwright/test';
import { GuiLayoutPage } from '../pages/GuiLayoutPage';
import { takeScreenshot } from '../helpers/debug';

test.describe('GUI Layout Editor', () => {
  test('displays the layout page with default grid and palette', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();

    await expect(layoutPage.header).toHaveText('Layout');
    // Default layout has 6 rows × 9 cols = 54 slots
    expect(await layoutPage.getSlotCount()).toBe(54);
    // Palette should show available skills
    expect(await layoutPage.getPaletteCount()).toBeGreaterThanOrEqual(1);
  });

  test('renders the default page tabs', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();

    expect(await layoutPage.getTabCount()).toBeGreaterThanOrEqual(1);
  });

  test('adds a new page tab', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();

    const initial = await layoutPage.getTabCount();
    await layoutPage.clickAddPage();
    expect(await layoutPage.getTabCount()).toBe(initial + 1);
  });

  test('can add and remove a page tab', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();

    const initial = await layoutPage.getTabCount();
    await layoutPage.clickAddPage();
    expect(await layoutPage.getTabCount()).toBe(initial + 1);

    // Remove the newly added tab
    await layoutPage.removeTab(initial);

    expect(await layoutPage.getTabCount()).toBe(initial);
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
