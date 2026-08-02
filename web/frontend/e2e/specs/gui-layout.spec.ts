import { test, expect } from '../fixtures';
import { GuiLayoutPage } from '../pages/GuiLayoutPage';
import { ensureLoggedIn } from '../pages/shared-login';

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
    // The filter applies reactively; poll instead of sleeping.
    await expect.poll(() => layoutPage.getPaletteCount()).toBeLessThanOrEqual(initialCount);
  });

  test('palette skills sort by color then name, matching the dashboard', async ({ page }) => {
    const layoutPage = new GuiLayoutPage(page);
    await layoutPage.goto();

    // Reference order: the dashboard sorts by color then displayName/id
    await page.goto('/#/');
    const dashboardCards = page.locator('.skill-card');
    await dashboardCards.first().waitFor({ state: 'visible', timeout: 10000 });
    const dashboardNames = await dashboardCards.locator('.skill-name').allTextContents();
    expect(dashboardNames.length).toBeGreaterThanOrEqual(2);

    // The navigation flyout must present the same order
    await page.goto('/#/layout');
    const paletteItems = page.locator('.palette-item');
    await paletteItems.first().waitFor({ state: 'visible', timeout: 10000 });
    const paletteNames = await page.locator('.palette-item-name').allTextContents();

    expect(paletteNames).toEqual(dashboardNames);
  });

  test('topbar skills dropdown sorts by color then name, matching the dashboard', async ({ page }) => {
    // Establish auth through the dashboard (handles the login page if the auth
    // guard races ahead of session restore)
    await page.goto('/#/');
    await page.waitForLoadState('load');
    await ensureLoggedIn(page);
    const dashboardCards = page.locator('.skill-card');
    await dashboardCards.first().waitFor({ state: 'visible', timeout: 10000 });
    const dashboardNames = await dashboardCards.locator('.skill-name').allTextContents();
    expect(dashboardNames.length).toBeGreaterThanOrEqual(2);

    // Hover the "Skills" nav link to reveal the topbar flyout
    await page.locator('.nav-dropdown').hover();
    const dropdownItems = page.locator('.dropdown-item');
    await dropdownItems.first().waitFor({ state: 'visible', timeout: 10000 });
    const dropdownTexts = await dropdownItems.allTextContents();

    // Each dropdown item embeds the skill name (plus an icon fallback letter);
    // walking in order, every dashboard skill must appear in the flyout.
    expect(dropdownTexts.length).toBe(dashboardNames.length);
    for (let i = 0; i < dashboardNames.length; i++) {
      expect(dropdownTexts[i]).toContain(dashboardNames[i]);
    }
  });
});
