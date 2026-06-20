// Capture real game screens for the design baseline (design/screens/*.png).
// Drives the KorGE/JS build at http://localhost:8080 through the scenes we can reach
// without gameplay (Menu, HUD, Paused, Settings). Game Over / Victory need real play.
//
// Prereqs: a served web build (`./gradlew playWebHeadless`) + Playwright from scripts/web-console.
//   node scripts/capture-screens.js          # uses http://localhost:8080
//   GAME_URL=http://localhost:9000 node scripts/capture-screens.js
//
// HEADED vs HEADLESS — when to use which:
//   This script defaults to HEADED + 1:1 native scale because headless is NOT
//   trustworthy for pixels or perf: it renders through SwiftShader (software GL),
//   whose fwidth/derivatives differ from hardware GL, so KorGE's SDF text AA clips
//   thin glyph stems into slivers. A prior session chased that as a phantom "font
//   artifact"; it's software-GL only (isolated at integer 1:1 — the screen scale is
//   irrelevant). See TASKS.md + the headless_playwright_visual_unreliable memory. So:
//     - UI/visual validation, design baselines, perf  -> HEADED (default here).
//     - routine functional smoke checks (did it boot? console errors? scene
//       transitions? element exists?) -> headless is fine and faster; tightens the
//       build-test loop. Prefer the scripts/web-console tooling for that.
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

// Capture path matters for SDF text: headless uses software GL (SwiftShader) and a
// non-integer resample, both of which manufacture thin-stem slivers that the real
// GPU does not. Default to HEADED + 1:1 integer scale (matches the on-screen game).
// Override with HEADLESS=1 / DSF=2 / W=600 H=900 to reproduce the old headless path.
const HEADLESS = process.env.HEADLESS === '1';
const DSF = Number(process.env.DSF || 1);
const W = Number(process.env.W || 1000);   // native logical canvas width
const H = Number(process.env.H || 1500);   // native logical canvas height

(async () => {
  console.log(`capture mode: headless=${HEADLESS} viewport=${W}x${H} dsf=${DSF}`);
  const browser = await chromium.launch({ headless: HEADLESS });
  const page = await browser.newPage({
    viewport: { width: W, height: H },
    deviceScaleFactor: DSF,
  });
  const shot = async (name) => {
    const file = path.join(OUT, `${name}.png`);
    await page.screenshot({ path: file });
    console.log('  captured', name);
  };

  console.log('loading', URL);
  await page.goto(URL, { waitUntil: 'networkidle' });
  await page.waitForSelector('canvas', { timeout: 30000 });
  await page.locator('canvas').click({ position: { x: W / 2, y: H / 2 } }); // focus the canvas for key events
  await sleep(4000); // let KorGE boot + load the atlas + first render

  await shot('1-menu');

  // Menu focus starts on START CAMPAIGN; Enter activates it → GameScene.
  // Capture HUD early (800ms) so lives = 3 before enemies can reach the player.
  await page.keyboard.press('Enter');
  await sleep(800);
  await shot('2-hud');

  // P toggles pause → paused overlay.
  await page.keyboard.press('p');
  await sleep(1200);
  await shot('3-paused');
  await page.keyboard.press('p'); // unpause
  await sleep(600);

  // ESC in GameScene opens Settings (EscapeHandler).
  await page.keyboard.press('Escape');
  await sleep(1200);
  await shot('4-settings');

  // ArrowDown ×7 reaches focus index 7 (CONTROLLER row); Enter navigates to ControllerPrefsScene.
  for (let i = 0; i < 7; i++) await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Enter');
  await sleep(1200);
  await shot('5-controller-prefs');

  await browser.close();
  console.log('done →', OUT);
})().catch((e) => { console.error(e); process.exit(1); });
