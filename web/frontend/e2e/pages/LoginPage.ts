import { Page, Locator } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly errorMsg: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.locator('#username');
    this.passwordInput = page.locator('#password');
    this.submitButton = page.locator('.login-btn');
    this.errorMsg = page.locator('.error-msg');
  }

  async goto() {
    await this.page.goto('/login');
  }

  async login(username: string, password: string) {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.submitButton.click();
  }

  async assertError(expected?: string) {
    await this.errorMsg.waitFor({ state: 'visible', timeout: 5000 });
    if (expected) {
      await this.errorMsg.textContent().then(t => {
        if (!t?.includes(expected)) {
          throw new Error(`Expected error containing "${expected}", got "${t}"`);
        }
      });
    }
  }
}
