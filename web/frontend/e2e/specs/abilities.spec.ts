import { test, expect } from '../fixtures';
import { DashboardPage } from '../pages/DashboardPage';

test.describe('Abilities Page', () => {
  test('shows a Cooldown row only for abilities that have one, formatted as n', async ({ page }) => {
    // Establish auth through the dashboard (handles the login page if the auth
    // guard races ahead of session restore), then visit the Abilities page
    const dashboard = new DashboardPage(page);
    await dashboard.goto();
    await page.goto('/#/abilities');
    await page.locator('.ability-card').first().waitFor({ state: 'visible', timeout: 10000 });

    // vein_miner in the mining fixture has a constant cooldown of 5
    const veinCard = page.locator('.ability-card', { hasText: 'Vein Miner' });
    await veinCard.first().waitFor({ state: 'visible', timeout: 10000 });
    const veinCooldown = veinCard.first().locator('.ability-detail', { hasText: 'Cooldown' });
    await expect(veinCooldown).toHaveCount(1);
    await expect(veinCooldown).toContainText('5s');

    // geologist in the mining fixture has no cooldown — no row
    const geoCard = page.locator('.ability-card', { hasText: 'Geologist' });
    await geoCard.first().waitFor({ state: 'visible', timeout: 10000 });
    await expect(geoCard.first().locator('.ability-detail', { hasText: 'Cooldown' })).toHaveCount(0);

    // No card renders raw evaluator JSON
    await expect(page.locator('.ability-card', { hasText: '"type"' })).toHaveCount(0);
    await expect(page.locator('.ability-card', { hasText: '"params"' })).toHaveCount(0);
  });
});
