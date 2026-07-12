import fs from "node:fs/promises";
import ph from "node:path";

import log from "fancy-log";
import * as h from "./_helpers.js";
import ppt from "pretty-time";

const isDebug = process.env.NODE_ENV !== "production";

const worker = h.startWorker();
let sass = null;

async function compileSassAll() {
  const start = process.hrtime();
  let error = false;

  log.info("init: compile styles");

  try {
    sass = await h.compileSassAll(worker);
    let output = await h.concatSass(sass);
    await fs.writeFile("./resources/public/css/main.css", output);

    if (isDebug) {
      let debugCSS = await h.compileSassDebug(worker);
      await fs.writeFile("./resources/public/css/debug.css", debugCSS);
    }
  } catch (cause) {
    error = cause;
  }

  const end = process.hrtime(start);

  if (error) {
    log.error("error: compile styles", `(${ppt(end)})`);
    console.error(error);
  } else {
    log.info("done: compile styles", `(${ppt(end)})`);
  }
}

async function compileSass(path) {
  const start = process.hrtime();
  log.info("changed:", path);

  try {
    const result = await h.compileSass(worker, path, { modules: true });
    sass.index[result.outputPath] = result.css;

    const output = h.concatSass(sass);

    await fs.writeFile("./resources/public/css/main.css", output);

    const end = process.hrtime(start);
    log.info("done:", `(${ppt(end)})`);
  } catch (cause) {
    console.error(cause);
    const end = process.hrtime(start);
    log.error("error:", `(${ppt(end)})`);
  }
}

await h.ensureDirectories();
await compileSassAll();
await h.copyAssets();
await h.copyWasmPlayground();
await h.compileTranslations();
await h.compileSvgSprites();
await h.compileTemplates();
await h.compilePolyfills();

log.info("watch: scss src (~)");

h.watch("src", h.isSassFile, async function (path) {
  // Skip penpot files that are shadowed by a Contrast override — the Contrast
  // watcher below handles those. Compiling the penpot version here would
  // reintroduce the shadowed file into the CSS, diverging from prod behaviour.
  const rel = ph.relative("src", path);
  const contrastPath = ph.join("../../frontend/src", rel);
  try {
    await fs.access(contrastPath);
    log.info("skipped (shadowed by Contrast override):", path);
    return;
  } catch (_) {}

  if (path.includes("common")) {
    await compileSassAll();
  } else {
    await compileSass(path);
  }
});

log.info("watch: scss: resources (~)");
h.watch("resources/styles", h.isSassFile, async function (path) {
  log.info("changed:", path);
  await compileSassAll();
});

log.info("watch: scss: contrast (~)");
h.watch("../../frontend/src", h.isSassFile, async function (path) {
  log.info("changed:", path);
  await compileSassAll();
});

log.info("watch: scss: contrast resources (~)");
h.watch("../../frontend/resources/styles", h.isSassFile, async function (path) {
  log.info("changed:", path);
  await compileSassAll();
});

log.info("watch: templates (~)");
h.watch("resources/templates", null, async function (path) {
  log.info("changed:", path);
  await h.compileTemplates();
});

log.info("watch: contrast templates (~)");
h.watch("../../frontend/resources/templates", null, async function (path) {
  log.info("changed:", path);
  await h.compileTemplates();
});

log.info("watch: translations (~)");
h.watch("translations", null, async function (path) {
  log.info("changed:", path);
  await h.compileTranslations();
});

log.info("watch: assets (~)");
h.watch(["resources/images", "resources/fonts"], null, async function (path) {
  log.info("changed:", path);
  await h.copyAssets();
  await h.compileSvgSprites();
  await h.compileTemplates();
});

log.info("watch: contrast assets (~)");      
h.watch(["../../frontend/resources/images"], 
null, async function (path) {                
  log.info("changed:", path);                
  await h.copyAssets();
  await h.compileSvgSprites();
  await h.compileTemplates();
});

log.info("watch: wasm playground (~)");
h.watch(["resources/wasm-playground"], null, async function (path) {
  log.info("changed:", path);
  await h.copyWasmPlayground();
});

worker.terminate();
