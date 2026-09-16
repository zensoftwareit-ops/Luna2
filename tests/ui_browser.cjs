// Run ui_review.php first. Uses Playwright and an installed Chromium browser.
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const base = path.resolve(__dirname, '..');
const out = path.join(base, 'storage/private/ui-review');
(async () => {
  const browser = await chromium.launch(process.env.LUNA_UI_BROWSER ? { executablePath: process.env.LUNA_UI_BROWSER, headless: true } : { headless: true });
  try {
    const page = await browser.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    // Intercept fixture requests; no live application or third-party service is contacted.
    await page.route('**/*', async route => {
      const url = new URL(route.request().url());
      if (url.origin !== 'http://luna-ui.test') return route.abort();
      const file = url.pathname.startsWith('/assets/')
        ? path.join(base, 'public/assets', path.basename(url.pathname))
        : path.join(out, path.basename(url.pathname));
      if (!fs.existsSync(file)) return route.fulfill({ status: 404, body: '' });
      const type = { '.html': 'text/html; charset=utf-8', '.css': 'text/css', '.js': 'text/javascript', '.png': 'image/png' }[path.extname(file)];
      await route.fulfill({ body: fs.readFileSync(file), contentType: type || 'application/octet-stream' });
    });
    let checks = 0;
    for (const width of [1440, 900, 390]) {
      await page.setViewportSize({ width, height: 1000 });
      for (const name of ['quotes', 'hr', 'logistics', 'communications', 'ecommerce', 'calendar', 'endpoints', 'imports', 'professional', 'datev']) {
        await page.goto(`http://luna-ui.test/${name}.html`);
        await page.waitForLoadState('networkidle');
        const sizes = await page.evaluate(() => ({ width: innerWidth, scroll: document.documentElement.scrollWidth }));
        assert(sizes.scroll <= sizes.width + 1, `${name} overflows at ${width}: ${sizes.scroll}`);
        assert.equal(errors.length, 0, `${name}: ${errors.join('; ')}`);
        const search = page.locator('.topbar-search input');
        if (await search.isVisible()) assert.equal(await search.evaluate(el => getComputedStyle(el).borderTopWidth), '0px');
        if (name === 'quotes') {
          const rows = page.locator('[data-lines] > tr');
          assert.equal(await rows.count(), 1);
          await page.locator('[data-add-line]').click();
          assert.equal(await rows.count(), 2);
          await rows.last().locator('[data-remove-line]').click();
          assert.equal(await rows.count(), 1);
          await page.locator('.table-wrap').evaluateAll(elements => elements.forEach(el => { el.scrollLeft = 0; }));
        }
        if (name === 'hr') {
          assert.equal(await page.locator('select[name="leave_type"]').first().inputValue(), 'HOLIDAY');
          assert.equal(await page.locator('select[name="calculation_mode"]').inputValue(), 'IMPORTED_PAYSLIPS');
        }
        if (['quotes', 'hr', 'imports', 'professional', 'communications', 'datev'].includes(name) && width !== 900) {
          await page.screenshot({ path: path.join(out, `${name}-${width}.png`), fullPage: true });
        }
        checks++;
      }
    }
    console.log(`${checks} verifiche browser completate; nessun errore JavaScript o overflow di pagina.`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
