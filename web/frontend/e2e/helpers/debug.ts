import { Page } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SCREENSHOT_DIR = path.resolve(__dirname, '../../screenshots');

/**
 * Opens a headed browser with DevTools, logs in automatically, and pauses
 * for interactive debugging.
 */
export async function launchDebugView(page: Page) {
  await page.goto('/login');
  await page.waitForSelector('#username', { timeout: 10000 });
  await page.fill('#username', 'admin');
  await page.fill('#password', 'skilling');
  await page.click('.login-btn');
  await page.waitForURL('**/');
  await page.pause();
}

/**
 * Returns computed CSS properties and layout info for an element.
 */
export async function inspectElement(page: Page, selector: string) {
  return page.evaluate((sel) => {
    const el = document.querySelector(sel);
    if (!el) return { error: `Element not found: ${sel}` };
    const rect = el.getBoundingClientRect();
    const styles = getComputedStyle(el);
    return {
      tag: el.tagName.toLowerCase(),
      classes: Array.from(el.classList),
      id: el.id,
      dimensions: { width: rect.width, height: rect.top, left: rect.left, top: rect.top },
      box: {
        margin: styles.margin,
        padding: styles.padding,
        border: styles.border,
      },
      computed: {
        display: styles.display,
        position: styles.position,
        flexDirection: styles.flexDirection,
        alignItems: styles.alignItems,
        justifyContent: styles.justifyContent,
        gap: styles.gap,
        gridTemplateColumns: styles.gridTemplateColumns,
        color: styles.color,
        backgroundColor: styles.backgroundColor,
        fontSize: styles.fontSize,
        fontWeight: styles.fontWeight,
        fontFamily: styles.fontFamily,
        opacity: styles.opacity,
        zIndex: styles.zIndex,
      },
      text: el.textContent?.slice(0, 200),
    };
  }, selector);
}

/**
 * Applies a CSS tweak to a matching element and takes a screenshot.
 * Useful for iterating on visual adjustments.
 */
export async function tweakCSS(
  page: Page,
  selector: string,
  property: string,
  value: string,
  screenshotName?: string
) {
  await page.evaluate(({ sel, prop, val }) => {
    document.querySelectorAll(sel).forEach((el) => {
      (el as HTMLElement).style.setProperty(prop, val, 'important');
    });
  }, { sel: selector, prop: property, val: value });

  if (screenshotName) {
    await takeScreenshot(page, screenshotName);
  }
}

/**
 * Takes a screenshot of the full page with a descriptive name.
 * Screenshots are saved to web/frontend/screenshots/.
 */
export async function takeScreenshot(page: Page, name: string) {
  if (!fs.existsSync(SCREENSHOT_DIR)) {
    fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
  }
  const filePath = path.resolve(SCREENSHOT_DIR, `${name}.png`);
  await page.screenshot({ path: filePath, fullPage: true });
  console.log(`  Screenshot saved: ${filePath}`);
  return filePath;
}

/**
 * Highlights an element by overlaying a colored border and takes a screenshot.
 */
export async function highlightElement(page: Page, selector: string, name?: string) {
  await page.evaluate((sel) => {
    document.querySelectorAll(sel).forEach((el) => {
      (el as HTMLElement).style.outline = '3px solid red';
      (el as HTMLElement).style.outlineOffset = '2px';
    });
  }, selector);

  const shotName = name || `highlight-${selector.replace(/[^a-zA-Z0-9]/g, '-')}`;
  await takeScreenshot(page, shotName);

  // Remove highlight
  await page.evaluate((sel) => {
    document.querySelectorAll(sel).forEach((el) => {
      (el as HTMLElement).style.outline = '';
      (el as HTMLElement).style.outlineOffset = '';
    });
  }, selector);
}
