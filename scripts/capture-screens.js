// Capture real game screens for the design baseline (design/screens/*.png).
// Drives the KorGE/JS build at http://localhost:8080 and jumps straight to each screen
// via the in-game debug "jump to screen" picker (no gameplay needed) — so end states
// (Game Over / Victory) that normally require dying / killing a boss capture the same way
// as the menus. The picker lives behind the debug overlay: `~` toggles the overlay, `J`
// opens the picker, then a per-screen key jumps. See main.kt.
//
// Prereqs: a served web build (`./gradlew playWebHeadless`) + Playwright from scripts/web-console.
//   node scripts/capture-screens.js          # uses http://localhost:8080
//   GAME_URL=http://localhost:9000 node scripts/capture-screens.js
//
// HEADED vs HEADLESS — when to use which:
//   This script defaults to HEADED + 1:1 native scale because headless is NOT
//   trustworthy for pixels: it renders through SwiftShader (software GL), whose
//   fwidth/derivatives differ from hardware GL, so KorGE's SDF text AA clips thin glyph
//   stems into slivers. (Low Power Mode does NOT affect captured pixels — it only throttles
//   rAF, which matters for perf, not screenshots.) See TASKS.md + the
//   headless_playwright_visual_unreliable / check_low_power_mode memories. So:
//     - UI/visual validation, design baselines  -> HEADED (default here).
//     - routine functional smoke checks -> headless is fine and faster (scripts/web-console).
//   Reproduce the old headless path with: HEADLESS=1 DSF=2 W=600 H=900 node scripts/capture-screens.js
//   Headed control note: Playwright drives Chrome over CDP, so the window does NOT
//   need OS focus and can sit in the background; just don't MINIMIZE it (stops
//   compositing -> stale/blank shots) or click into it mid-run.
const path = require('path');
// Playwright lives in the web-console tooling install, not a root node_modules.
const { chromium } = require(path.join(__dirname, 'web-console', 'node_modules', 'playwright-core'));

const URL = process.env.GAME_URL || 'http://localhost:8080/';
const OUT = path.resolve(__dirname, '..', 'design', 'screens');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const HEADLESS = process.env.HEADLESS === '1';
const DSF = Number(process.env.DSF || 1);
const W = Number(process.env.W || 1000);   // native logical canvas width
const H = Number(process.env.H || 1500);   // native logical canvas height

// name, jump-key, settle-ms (overlays are instant; HUD wants a beat so lives=3 shows).
const SCREENS = [
  ['1-menu',            'm', 400],
  ['2-hud',             'h', 700],
  ['3-paused',          'p', 400],
  ['4-settings',        's', 500],
  ['5-controller-prefs','c', 500],
  ['6-gameover',        'g', 500],
  ['7-victory',         'v', 500],
];

(async () => {
  console.log(`capture mode: headless=${HEADLESS} viewport=${W}x${H} dsf=${DSF}`);
  const browser = await chromium.launch({ headless: HEADLESS });
  const page = await browser.newPage({ viewport: { width: W, height: H }, deviceScaleFactor: DSF });

  console.log('loading', URL);
  await page.goto(URL, { waitUntil: 'networkidle' });
  await page.waitForSelector('canvas', { timeout: 30000 });
  await page.locator('canvas').click({ position: { x: W / 2, y: H / 2 } }); // focus for key events
  await sleep(4000); // KorGE boot + atlas + first render

  await page.keyboard.press('Backquote'); // debug overlay ON (enables the jump picker)
  await sleep(150);

  for (const [name, key, settle] of SCREENS) {
    await page.keyboard.press('j');         // open picker
    await sleep(120);
    await page.keyboard.press(key);         // jump to the screen (closes picker)
    await sleep(settle);
    await page.keyboard.press('Backquote'); // debug OFF → clean frame (no overlay/picker)
    await sleep(180);
    await page.screenshot({ path: path.join(OUT, `${name}.png`) });
    console.log('  captured', name);
    await page.keyboard.press('Backquote'); // debug ON again for the next jump
    await sleep(120);
  }

  await browser.close();
  console.log('done →', OUT);
})().catch((e) => { console.error(e); process.exit(1); });
