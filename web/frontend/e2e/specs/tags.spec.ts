import { test, expect } from '../fixtures';
import { TagsPage } from '../pages/TagsPage';

test.describe('Tags Editor', () => {
  test('displays loaded custom tags', async ({ page }) => {
    const tagsPage = new TagsPage(page);
    await tagsPage.goto();

    const tagNames = await tagsPage.getTagNames();
    expect(tagNames.length).toBeGreaterThanOrEqual(1);
    // Tags are stored as "#c:ores", "#c:stone" — the editor shows them
    expect(tagNames.some((n) => n.includes('ores'))).toBeTruthy();
  });

  test('editing a tag under a search filter preserves non-matching tags', async ({ page }) => {
    const tagsPage = new TagsPage(page);
    await tagsPage.goto();
    await page.locator('.tag-name').first().waitFor({ state: 'visible', timeout: 10000 });

    const cleanNames = () => page.locator('.tag-name').allTextContents().then(ls => ls.map(s => s.trim()));
    const before = await cleanNames();
    expect(before.length).toBeGreaterThanOrEqual(3);

    // Filter down to a single tag ("coal" only matches the ores tag)
    await page.locator('.search-input').fill('coal');
    await expect.poll(async () => cleanNames()).toHaveLength(1);

    // Add a new tag while the filter is active, then save
    await page.getByRole('button', { name: '+ Add Tag' }).click();
    await page.fill('.tag-name-input', 'newtest');
    await page.getByRole('button', { name: 'OK' }).click();
    await page.waitForSelector('.sticky-banner');

    const putPromise = page.waitForRequest(r => r.url().includes('/api/tags') && r.method() === 'PUT');
    await page.getByRole('button', { name: 'Save Changes' }).click();
    const put = await putPromise;
    const body = put.postDataJSON();

    // Every tag that did not match the search must still be in the saved payload
    const savedKeys = Object.keys(body.tags);
    for (const name of before) {
      expect(savedKeys, `tag ${name} must survive an edit under a search filter`).toContain(name);
    }
    expect(savedKeys).toContain('#c:newtest');
  });
});
