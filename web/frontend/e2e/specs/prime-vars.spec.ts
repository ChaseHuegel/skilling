import { test } from '@playwright/test';

test('list primevue CSS vars', async ({ page }) => {
  await page.goto('/#/');
  await page.waitForLoadState('networkidle');
  await page.fill('#username', 'admin');
  await page.fill('#password', 'skilling');
  await page.click('.login-btn');
  await page.waitForURL('**/');
  await page.waitForLoadState('networkidle');

  const vars = await page.evaluate(() => {
    const styles = getComputedStyle(document.documentElement);
    const result = [];
    // Collect all --p-* vars
    for (let i = 0; i < styles.length; i++) {
      const name = styles[i];
      if (name.startsWith('--p-')) {
        result.push({ name, value: styles.getPropertyValue(name).trim() });
      }
    }
    return result;
  });
  console.log('Light vars:', vars.slice(0, 20).map(v => v.name + '=' + v.value).join('\n'));

  await page.evaluate(() => document.documentElement.classList.add('app-dark'));
  await page.waitForTimeout(500);

  const darkVars = await page.evaluate(() => {
    const styles = getComputedStyle(document.documentElement);
    const result = [];
    for (let i = 0; i < styles.length; i++) {
      const name = styles[i];
      if (name.startsWith('--p-')) {
        result.push({ name, value: styles.getPropertyValue(name).trim() });
      }
    }
    return result;
  });
  console.log('Dark vars (first 20):', darkVars.slice(0, 20).map(v => v.name + '=' + v.value).join('\n'));
});
