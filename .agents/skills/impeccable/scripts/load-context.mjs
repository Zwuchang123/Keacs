/**
 * Shared context loader for every impeccable command that needs to know
 * "who is this for" and "what does this look like".
 *
 * Input: project root (process.cwd()).
 *
 * Output (JSON to stdout):
 *   {
 *     hasProduct: boolean,        // product context found (or auto-migrated)
 *     product: string | null,     // product context contents
 *     productPath: string | null, // relative path
 *     hasDesign: boolean,         // design context found
 *     design: string | null,      // design context contents
 *     designPath: string | null,
 *     migrated: boolean,          // true if we auto-renamed .impeccable.md -> PRODUCT.md
 *   }
 *
 * This project keeps product and design context under docs/. Root-level
 * PRODUCT.md / DESIGN.md remain fallback-compatible for the original skill.
 */

import fs from 'node:fs';
import path from 'node:path';

const PRODUCT_NAMES = ['docs/prd.md', 'docs/PRD.md', 'PRODUCT.md', 'Product.md', 'product.md'];
const DESIGN_NAMES = ['docs/design.md', 'docs/DESIGN.md', 'DESIGN.md', 'Design.md', 'design.md'];
const LEGACY_NAMES = ['.impeccable.md'];

export function loadContext(cwd = process.cwd()) {
  let migrated = false;

  // 1. Look for product context
  let productPath = firstExisting(cwd, PRODUCT_NAMES);

  // 2. Legacy: if no PRODUCT.md but .impeccable.md exists, rename in place
  if (!productPath) {
    const legacyPath = firstExisting(cwd, LEGACY_NAMES);
    if (legacyPath) {
      const newPath = path.join(cwd, 'PRODUCT.md');
      try {
        fs.renameSync(legacyPath, newPath);
        productPath = newPath;
        migrated = true;
      } catch {
        // Rename failed (permissions, etc.) — fall back to reading legacy in place
        productPath = legacyPath;
      }
    }
  }

  // 3. Look for design context
  const designPath = firstExisting(cwd, DESIGN_NAMES);

  const product = productPath ? safeRead(productPath) : null;
  const design = designPath ? safeRead(designPath) : null;

  return {
    hasProduct: !!product,
    product,
    productPath: productPath ? path.relative(cwd, productPath) : null,
    hasDesign: !!design,
    design,
    designPath: designPath ? path.relative(cwd, designPath) : null,
    migrated,
  };
}

function firstExisting(cwd, names) {
  for (const name of names) {
    const abs = path.join(cwd, name);
    if (fs.existsSync(abs)) return abs;
  }
  return null;
}

function safeRead(p) {
  try { return fs.readFileSync(p, 'utf-8'); } catch { return null; }
}

// ---------------------------------------------------------------------------
// CLI mode — print the context as JSON
// ---------------------------------------------------------------------------

function cli() {
  const result = loadContext(process.cwd());
  console.log(JSON.stringify(result, null, 2));
}

const _running = process.argv[1];
if (_running?.endsWith('load-context.mjs') || _running?.endsWith('load-context.mjs/')) {
  cli();
}
