import { test } from '@playwright/test';

test('find surface vars', async ({ page }) => {
  await page.goto('/#/');
  await page.waitForLoadState('networkidle');
  await page.fill('#username', 'admin');
  await page.fill('#password', 'skilling');
  await page.click('.login-btn');
  await page.waitForURL('**/');
  await page.waitForLoadState('networkidle');

  const surfaceVars = await page.evaluate(() => {
    const styles = getComputedStyle(document.documentElement);
    const result = [];
    for (let i = 0; i < styles.length; i++) {
      const name = styles[i];
      if (name.startsWith('--p-surface') || name.startsWith('--p-overlay') || name.startsWith('--p-form') || name.startsWith('--p-content')) {
        result.push({ name, value: styles.getPropertyValue(name).trim() });
      }
    }
    return result.sort((a, b) => a.name.localeCompare(b.name));
  });
  console.log('SURFACE VARS (LIGHT):');
  console.log(surfaceVars.map(v => v.name + '=' + v.value).join('\n'));
});
