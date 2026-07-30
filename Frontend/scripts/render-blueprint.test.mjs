import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const blueprintUrl = new URL('../../render.yaml', import.meta.url);

test('keeps the public Render Blueprint free and requests database secrets', async () => {
  const blueprint = await readFile(blueprintUrl, 'utf8');

  assert.doesNotMatch(blueprint, /^\s*-\s+type:\s+pserv\s*$/m);
  assert.doesNotMatch(blueprint, /^\s+disk:\s*$/m);
  assert.deepEqual(
    [...blueprint.matchAll(/^\s+plan:\s+(\S+)\s*$/gm)].map((match) => match[1]),
    ['free'],
  );

  for (const key of ['DB_URL', 'DB_USERNAME', 'DB_PASSWORD']) {
    assert.match(blueprint, new RegExp(`- key: ${key}\\s+sync: false`));
  }
});
