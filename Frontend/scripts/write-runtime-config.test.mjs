import assert from 'node:assert/strict';
import test from 'node:test';

import { createRuntimeConfig, normalizeApiUrl } from './write-runtime-config.mjs';

test('uses the same-origin API path by default', () => {
  assert.equal(normalizeApiUrl(), '/api');
});

test('normalizes root-relative and absolute API URLs', () => {
  assert.equal(normalizeApiUrl('/api/'), '/api');
  assert.equal(
    normalizeApiUrl('https://jobtrackr-api.example.com/api/'),
    'https://jobtrackr-api.example.com/api',
  );
});

test('rejects unsafe or malformed API URLs', () => {
  assert.throws(() => normalizeApiUrl('//example.com/api'), /protocol-relative/);
  assert.throws(() => normalizeApiUrl('javascript:alert(1)'), /HTTP or HTTPS/);
  assert.throws(() => normalizeApiUrl('https://user:password@example.com/api'), /credentials/);
  assert.throws(() => normalizeApiUrl('not a URL'), /absolute HTTP/);
});

test('escapes runtime values as a JavaScript string literal', () => {
  assert.equal(
    createRuntimeConfig("https://example.com/team's-api"),
    "globalThis.__JOBTRACKR_CONFIG__ = Object.freeze({\n  apiUrl: 'https://example.com/team\\'s-api',\n});\n",
  );
});
