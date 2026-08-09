import { test, expect } from '../fixtures';
import { TagsPage } from '../pages/TagsPage';

test.describe('Tags Editor', () => {
  test('displays loaded custom tags', async ({ page }) => {
    const tagsPage = new TagsPage(page);
    await tagsPage.goto();

    const tagNames = await tagsPage.getMaterialTagNames();
    expect(tagNames.length).toBeGreaterThanOrEqual(1);
    // Tags are stored as "#c:ores", "#c:stone" — the editor shows them
    expect(tagNames.some((n) => n.includes('ores'))).toBeTruthy();
  });

  test('displays entity tags in their own section', async ({ page }) => {
    const tagsPage = new TagsPage(page);
    await tagsPage.goto();

    const entityTagNames = await tagsPage.getEntityTagNames();
    expect(entityTagNames.length).toBeGreaterThanOrEqual(1);
    // Entity tags are stored under entity_tags: with the same "#c:" prefix
    expect(entityTagNames.some((n) => n.includes('undead'))).toBeTruthy();
  });

  test('editing a tag under a search filter preserves non-matching tags', async ({ page }) => {
    const tagsPage = new TagsPage(page);
    await tagsPage.goto();
    const materialTags = tagsPage.materialSection.locator('.tag-name');
    await materialTags.first().waitFor({ state: 'visible', timeout: 10000 });

    const cleanNames = () => materialTags.allTextContents().then(ls => ls.map(s => s.trim()));
    const before = await cleanNames();
    expect(before.length).toBeGreaterThanOrEqual(3);

    // Filter down to a single material tag ("coal" only matches the ores tag)
    await page.locator('.search-input').fill('coal');
    await expect.poll(async () => cleanNames()).toHaveLength(1);

    // Add a new tag while the filter is active, then save
    await tagsPage.materialSection.getByRole('button', { name: '+ Add Tag' }).click();
    await tagsPage.materialSection.locator('.tag-name-input').fill('newtest');
    await tagsPage.materialSection.getByRole('button', { name: 'OK' }).click();
    await page.waitForSelector('.sticky-banner');

    const putPromise = page.waitForRequest(r => r.url().includes('/api/tags') && r.method() === 'PUT');
    await page.getByRole('button', { name: 'Save Changes' }).click();
    const put = await putPromise;
    const body = put.postDataJSON();

    // Every material tag that did not match the search must still be in the saved payload
    const savedKeys = Object.keys(body.tags);
    for (const name of before) {
      expect(savedKeys, `tag ${name} must survive an edit under a search filter`).toContain(name);
    }
    expect(savedKeys).toContain('#c:newtest');
  });

  test('entity tags round-trip through the save payload', async ({ page }) => {
    const tagsPage = new TagsPage(page);
    await tagsPage.goto();

    // Edit an entity tag: add a value under the existing "#c:undead" entry
    const undeadRow = tagsPage.entitySection.locator('.tag-entry', { hasText: '#c:undead' });
    await undeadRow.locator('.multi-input').fill('minecraft:ghast');
    await undeadRow.locator('button:has-text("Add")').click();
    await page.waitForSelector('.sticky-banner');

    const putPromise = page.waitForRequest(r => r.url().includes('/api/tags') && r.method() === 'PUT');
    await page.getByRole('button', { name: 'Save Changes' }).click();
    const put = await putPromise;
    const body = put.postDataJSON();

    // Both sections must be present in the single PUT payload
    expect(body.tags).toBeDefined();
    expect(body.entityTags).toBeDefined();
    const undeadValues = body.entityTags['#c:undead'];
    expect(undeadValues).toContain('minecraft:ghast');
  });
});
