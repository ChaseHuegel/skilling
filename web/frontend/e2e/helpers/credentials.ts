/**
 * Shared E2E web credentials.
 *
 * The plugin rotates its default password ("skilling") on first enable, so the
 * automatic-mode fixture uses a fixed non-default password. External-server
 * mode can override it via SKILLING_WEB_PASSWORD.
 */
export const WEB_USERNAME = 'admin';
export const WEB_PASSWORD = process.env.SKILLING_WEB_PASSWORD || 'e2e_secret';
export const BASIC_AUTH =
  'Basic ' + Buffer.from(`${WEB_USERNAME}:${WEB_PASSWORD}`).toString('base64');
