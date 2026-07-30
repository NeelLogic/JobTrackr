import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptsDirectory = dirname(fileURLToPath(import.meta.url));
const frontendDirectory = resolve(scriptsDirectory, '..');
const outputPath = resolve(frontendDirectory, 'public', 'config.js');

export function normalizeApiUrl(value) {
  const candidate = value?.trim() || '/api';

  if (candidate.startsWith('/')) {
    if (candidate.startsWith('//')) {
      throw new Error('JOBTRACKR_API_URL must not be protocol-relative.');
    }
    return candidate.replace(/\/+$/, '') || '/';
  }

  let url;
  try {
    url = new URL(candidate);
  } catch {
    throw new Error('JOBTRACKR_API_URL must be an absolute HTTP(S) URL or a root-relative path.');
  }

  if (!['http:', 'https:'].includes(url.protocol)) {
    throw new Error('JOBTRACKR_API_URL must use HTTP or HTTPS.');
  }

  if (url.username || url.password || url.search || url.hash) {
    throw new Error('JOBTRACKR_API_URL must not contain credentials, a query, or a fragment.');
  }

  return url.toString().replace(/\/+$/, '');
}

export function createRuntimeConfig(apiUrl) {
  const escapedApiUrl = apiUrl
    .replaceAll('\\', '\\\\')
    .replaceAll("'", "\\'")
    .replaceAll('\r', '\\r')
    .replaceAll('\n', '\\n')
    .replaceAll('\u2028', '\\u2028')
    .replaceAll('\u2029', '\\u2029');

  return `globalThis.__JOBTRACKR_CONFIG__ = Object.freeze({\n  apiUrl: '${escapedApiUrl}',\n});\n`;
}

export async function writeRuntimeConfig(value = process.env.JOBTRACKR_API_URL) {
  const apiUrl = normalizeApiUrl(value);
  const contents = createRuntimeConfig(apiUrl);

  await mkdir(dirname(outputPath), { recursive: true });
  await writeFile(outputPath, contents, 'utf8');
  return apiUrl;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const apiUrl = await writeRuntimeConfig();
  console.log(`Frontend API URL: ${apiUrl}`);
}
