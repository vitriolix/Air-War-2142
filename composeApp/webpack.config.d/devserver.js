// Disable webpack-dev-server's auto-open: it launches the system DEFAULT browser on
// startup (even under `playWebHeadless`), which collides with Playwright's own
// "Chrome for Testing" during captures (two browser windows). play.sh handles opening
// a browser itself for the non-headless path, so the dev server shouldn't also do it.
config.devServer = config.devServer || {};
config.devServer.open = false;
