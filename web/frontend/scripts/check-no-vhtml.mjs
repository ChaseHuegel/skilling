import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * Forbids raw `v-html` bindings in the frontend source. All Minecraft color-code
 * rendering must go through the shared FormattedText component (or a plain text
 * interpolation of `segmentStyle` output) so user-authored text is always
 * escaped by Vue. Fails the build if any `v-html` binding is reintroduced.
 */
const SRC = join(dirname(fileURLToPath(import.meta.url)), '..', 'src');

const failures = [];

function walk(dir) {
    for (const entry of readdirSync(dir)) {
        const path = join(dir, entry);
        if (statSync(path).isDirectory()) {
            walk(path);
        } else if (path.endsWith('.vue')) {
            const content = readFileSync(path, 'utf8');
            if (/\bv-html\b/.test(content)) failures.push(path);
        }
    }
}

walk(SRC);

if (failures.length > 0) {
    console.error('v-html is forbidden; render text via FormattedText instead:');
    for (const f of failures) console.error('  ' + f);
    process.exit(1);
}
console.log('check-no-vhtml: no v-html bindings found in src');
