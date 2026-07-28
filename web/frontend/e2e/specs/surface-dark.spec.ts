import { test } from '@playwright/test';
test('surface vars dark', async ({ page }) => {
  await page.goto('/#/'); await page.waitForLoadState('networkidle');
  await page.fill('#username', 'admin'); await page.fill('#password', 'skilling');
  await page.click('.login-btn'); await page.waitForURL('**/'); await page.waitForLoadState('networkidle');
  await page.evaluate(() => document.documentElement.classList.add('app-dark'));
  await page.waitForTimeout(500);
  const vars = await page.evaluate(() => {
    const s = getComputedStyle(document.documentElement);
    const keys = ['--p-content-background','--p-content-border-color','--p-content-color','--p-content-hover-background','--p-form-field-background','--p-form-field-border-color','--p-form-field-color','--p-form-field-hover-border-color','--p-form-field-placeholder-color','--p-text-color','--p-primary-color'];
    return keys.map(k => k + '=' + s.getPropertyValue(k).trim()).join('\n');
  });
  console.log(vars);
});
