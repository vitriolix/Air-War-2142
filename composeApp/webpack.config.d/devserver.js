// webpack-dev-server auto-opens the system DEFAULT browser on startup. That's wanted for
// the interactive `playWeb` (you want to see the game), but NOT for the headless capture
// path: there it collides with Playwright's own "Chrome for Testing" (two windows).
//
// So only suppress auto-open when play.sh's `--headless` path asks for it via this env var.
// Interactive `playWeb` leaves webpack's default open behaviour intact.
config.devServer = config.devServer || {};
if (process.env.AIR_WAR_2142_NO_OPEN === "1") {
  config.devServer.open = false;
}
