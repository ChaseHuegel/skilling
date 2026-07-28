import { test } from '@playwright/test';

test('verify card bg and borders', async ({ page }) => {
  await page.goto('/#/'); await page.waitForLoadState('networkidle');
  await page.fill('#username', 'admin'); await page.fill('#password', 'skilling');
  await page.click('.login-btn'); await page.waitForURL('**/'); await page.waitForLoadState('networkidle');
  await page.waitForSelector('.skill-card', { timeout: 5000 });

  const light = await page.evaluate(() => {
    const s = document.querySelector('.skill-card');
    if (!s) return {};
    const st = getComputedStyle(s);
    return {
      backgroundColor: st.backgroundColor,
      borderRightColor: st.borderRightColor,
      borderBottomColor: st.borderBottomColor,
      borderLeftColor: st.borderLeftColor,
      borderRightWidth: st.borderRightWidth,
      borderRadius: st.borderRadius,
      backgroundClip: st.backgroundClip,
    };
  });
  console.log('LIGHT:', JSON.stringify(light, null, 2));

  await page.evaluate(() => document.documentElement.classList.add('app-dark'));
  await page.waitForTimeout(500);

  const dark = await page.evaluate(() => {
    const s = document.querySelector('.skill-card');
    if (!s) return {};
    const st = getComputedStyle(s);
    return {
      backgroundColor: st.backgroundColor,
      borderRightColor: st.borderRightColor,
      borderBottomColor: st.borderBottomColor,
      borderLeftColor: st.borderLeftColor,
      borderRightWidth: st.borderRightWidth,
      borderRadius: st.borderRadius,
    };
  });
  console.log('DARK:', JSON.stringify(dark, null, 2));
});
