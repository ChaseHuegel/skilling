import { test } from '@playwright/test';

test('debug borders dark mode', async ({ page }) => {
  await page.goto('/#/');
  await page.waitForLoadState('networkidle');
  await page.fill('#username', 'admin');
  await page.fill('#password', 'skilling');
  await page.click('.login-btn');
  await page.waitForURL('**/'); await page.waitForLoadState('networkidle');
  await page.waitForSelector('.skill-card', { timeout: 5000 });

  await page.evaluate(() => document.documentElement.classList.add('app-dark'));
  await page.waitForTimeout(500);

  const info = await page.evaluate(() => {
    const s = document.querySelector('.skill-card');
    if (!s) return 'no card';
    const st = getComputedStyle(s);
    return {
      background: st.background,
      borderTop: st.borderTop,
      borderRight: st.borderRight,
      borderBottom: st.borderBottom,
      borderLeft: st.borderLeft,
    };
  });
  console.log('SKILL CARD:', JSON.stringify(info, null, 2));

  // Check the CSS variable value
  const rootVars = await page.evaluate(() => ({
    surfaceBorder: getComputedStyle(document.documentElement).getPropertyValue('--p-surface-border').trim(),
    surfaceSection: getComputedStyle(document.documentElement).getPropertyValue('--p-surface-section').trim(),
    surfaceGround: getComputedStyle(document.documentElement).getPropertyValue('--p-surface-ground').trim(),
  }));
  console.log('ROOT VARS:', JSON.stringify(rootVars, null, 2));

  // Check actual HTML element class
  const htmlClass = await page.evaluate(() => document.documentElement.className);
  console.log('HTML CLASS:', htmlClass);
});
