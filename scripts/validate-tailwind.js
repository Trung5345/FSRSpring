#!/usr/bin/env node
/**
 * validate-tailwind.js
 * Validates that every HTML page has the Tailwind CDN script BEFORE
 * the tailwind.config block — required for custom colors to work.
 *
 * Usage:
 *   node scripts/validate-tailwind.js          # check all HTML files
 *   node scripts/validate-tailwind.js --fix    # auto-fix order if wrong
 */

const fs = require("fs");
const path = require("path");

const STATIC_DIR = path.join(__dirname, "..", "src", "main", "resources", "static");
const args = process.argv.slice(2);
const FIX_MODE = args.includes("--fix");

const htmlFiles = fs.readdirSync(STATIC_DIR)
  .filter((f) => f.endsWith(".html"))
  .map((f) => path.join(STATIC_DIR, f));

let errors = 0;
let fixed = 0;

for (const file of htmlFiles) {
  const content = fs.readFileSync(file, "utf8");
  const lines = content.split("\n");

  const cdnIdx = lines.findIndex((l) => l.includes("cdn.tailwindcss.com"));
  // Match only the actual assignment, not comments mentioning "tailwind.config"
  const cfgIdx = lines.findIndex((l) => /tailwind\.config\s*=/.test(l));

  const fname = path.basename(file);

  if (cdnIdx === -1) {
    console.error(`ERROR [${fname}]: Missing Tailwind CDN script tag.`);
    errors++;
    continue;
  }
  if (cfgIdx === -1) {
    console.error(`ERROR [${fname}]: Missing tailwind.config block.`);
    errors++;
    continue;
  }

  if (cdnIdx < cfgIdx) {
    console.log(`  OK [${fname}]: CDN (line ${cdnIdx + 1}) before config (line ${cfgIdx + 1})`);
    continue;
  }

  // Order is wrong
  if (!FIX_MODE) {
    console.error(
      `ERROR [${fname}]: CDN (line ${cdnIdx + 1}) is AFTER config (line ${cfgIdx + 1}). ` +
        `Run with --fix to auto-repair.`
    );
    errors++;
    continue;
  }

  // --fix: extract CDN line, reinsert before tailwind.config line
  const cdnLine = lines.splice(cdnIdx, 1)[0]; // remove CDN line
  // After removal, config index may have shifted
  const newCfgIdx = lines.findIndex((l) => l.includes("tailwind.config"));
  lines.splice(newCfgIdx, 0, cdnLine); // insert CDN before config
  fs.writeFileSync(file, lines.join("\n"), "utf8");
  console.log(`  FIXED [${fname}]: moved CDN before tailwind.config`);
  fixed++;
}

console.log("");
if (errors > 0) {
  console.error(`FAILED: ${errors} error(s). Run with --fix to auto-repair.`);
  process.exit(1);
} else if (fixed > 0) {
  console.log(`Fixed ${fixed} file(s). Re-copy to target/classes/static/ if needed.`);
} else {
  console.log("All HTML files have correct Tailwind load order.");
}
