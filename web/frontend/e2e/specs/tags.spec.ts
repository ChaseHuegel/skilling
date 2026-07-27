import { test, expect } from '@playwright/test';
import { TagsPage } from '../pages/TagsPage';
import { takeScreenshot } from '../helpers/debug';

test.describe('Tags Editor', () => {
  test('displays loaded custom tags', async ({ page }) => {
    const tagsPage = new TagsPage(page);
    await tagsPage.goto();

    const tagNames = await tagsPage.getTagNames();
    expect(tagNames.length).toBeGreaterThanOrEqual(1);
    // Tags are stored as "#c:ores", "#c:stone" — the editor shows them
    expect(tagNames.some((n) => n.includes('ores'))).toBeTruthy();
  });

  test('tags page screenshot', async ({ page }) => {
    const tagsPage = new TagsPage(page);
    await tagsPage.goto();
    await takeScreenshot(page, 'tags-editor');
  });
});
