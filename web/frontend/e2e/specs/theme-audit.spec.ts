import { test } from '@playwright/test';
import { ensureLoggedIn } from '../pages/shared-login';

const SELECTORS = [
  // Inputs & selects that commonly have theme issues
  { name: 'nav-links', selector: '.nav-link' },
  { name: 'skill-card', selector: '.skill-card' },
  { name: 'skill-name', selector: '.skill-name' },
  { name: 'meta-text', selector: '.skill-meta' },
  { name: 'badge', selector: '.badge' },
  // Add more as we discover them
];

async function inspectTheme(page: any, selector: string) {
  return page.evaluate((sel: string) => {
    const el = document.querySelector(sel);
    if (!el) return { selector: sel, error: 'not found' };
    const s = getComputedStyle(el);
    return {
      selector: sel,
      background: s.background,
      color: s.color,
      border: s.border,
      boxShadow: s.boxShadow,
    };
  }, selector);
}

test.describe('Theme Audit', () => {
  test('inspect dashboard elements in light mode', async ({ page }) => {
    await ensureLoggedIn(page);
    await page.goto('/#/');
    await page.waitForLoadState('networkidle');
    // Ensure light mode
    await page.evaluate(() => document.documentElement.classList.remove('app-dark'));
    await page.waitForTimeout(300);
    for (const s of SELECTORS) {
      const result = await inspectTheme(page, s.selector);
      console.log(JSON.stringify(result));
    }
    await page.screenshot({ path: '/tmp/theme-light.png', fullPage: true });
  });

  test('inspect dashboard elements in dark mode', async ({ page }) => {
    await ensureLoggedIn(page);
    await page.goto('/#/');
    await page.waitForLoadState('networkidle');
    // Ensure dark mode
    await page.evaluate(() => document.documentElement.classList.add('app-dark'));
    await page.waitForTimeout(300);
    for (const s of SELECTORS) {
      const result = await inspectTheme(page, s.selector);
      console.log(JSON.stringify(result));
    }
    await page.screenshot({ path: '/tmp/theme-dark.png', fullPage: true });
  });
});
