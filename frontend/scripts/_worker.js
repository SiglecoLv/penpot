import proc from "node:child_process";
import crypto from "node:crypto";
import fs from "node:fs/promises";
import ph from "node:path";
import url from "node:url";
import * as sass from "sass-embedded";
import log from "fancy-log";

import wpool from "workerpool";
import postcss from "postcss";
import modulesProcessor from "postcss-modules";
import autoprefixerProcessor from "autoprefixer";

const compiler = await sass.initAsyncCompiler();

async function compileFile(path) {
  const dir = ph.dirname(path);
  const name = ph.basename(path, ".scss");
  const dest = `${dir}${ph.sep}${name}.css`;

  return new Promise(async (resolve, reject) => {
    try {
      const result = await compiler.compileAsync(path, {
        loadPaths: [
          "../../frontend/resources/styles/common/",
          "../../frontend/resources/styles/",
          "../../frontend/src/app/main/ui/",
          "node_modules/animate.css",
          "resources/styles/common/",
          "resources/styles",
          "src/app/main/ui/",
        ],
        sourceMap: false,
      });
      resolve({
        inputPath: path,
        outputPath: dest,
        css: result.css,
      });
    } catch (cause) {
      reject(cause);
    }
  });
}

function configureModulesProcessor(options) {
  const ROOT_NAME = "app";

  return modulesProcessor({
    getJSON: (cssFileName, json, outputFileName) => {
      // We do nothing because we don't want the generated JSON files
    },
    // Calculates the whole css-module selector name.
    // Should be the same as the one in the file `/src/app/main/style.clj`
    //
    // Contrast: when CONTRAST_OBFUSCATE_CSS is set (prod build only), the
    // readable name is replaced by a short md5 token to minify + hide the
    // penpot namespace fingerprint. MUST stay byte-identical to `scoped` in
    // style.clj: "c" + md5hex(full).slice(0,12) over the SAME `full` string.
    generateScopedName: (selector, filename, css) => {
      const dir = ph.dirname(filename);
      const name = ph.basename(filename, ".css");
      const parts = dir.split("/");
      const rootIdx = parts.findIndex((s) => s === ROOT_NAME);
      const full = parts.slice(rootIdx + 1).join("_") + "_" + name + "__" + selector;
      if (process.env.CONTRAST_OBFUSCATE_CSS) {
        return "c" + crypto.createHash("md5").update(full, "utf8").digest("hex").slice(0, 12);
      }
      return full;
    },
  });
}

function configureProcessor(options = {}) {
  const processors = [];

  if (options.modules) {
    processors.push(configureModulesProcessor(options));
  }
  processors.push(autoprefixerProcessor);

  return postcss(processors);
}

async function postProcessFile(data, options) {
  const proc = configureProcessor(options);

  // We compile to the same path (all in memory)
  const result = await proc.process(data.css, {
    from: data.outputPath,
    to: data.outputPath,
    map: false,
  });

  return Object.assign(data, {
    css: result.css,
  });
}

async function compile(path, options) {
  let result = await compileFile(path);
  return await postProcessFile(result, options);
}

wpool.worker(
  {
    compileSass: compile,
  },
  {
    onTerminate: async (code) => {
      // log.info("worker: terminate");
      await compiler.dispose();
    },
  },
);
