const assert = require('node:assert/strict');
const test = require('node:test');
const { requestTranslation } = require('../api');

test('reads the documented translation field and sends Basic auth', async () => {
  let captured;
  const translation = await requestTranslation({
    backendUrl: 'http://localhost:9080/', username: 'demo', password: 'secret', text: 'Hello',
    fetchImpl: async (url, options) => {
      captured = { url, options };
      return { ok: true, json: async () => ({ translation: 'سلام' }) };
    },
  });
  assert.equal(translation, 'سلام');
  assert.equal(captured.url, 'http://localhost:9080/api/translate');
  assert.match(captured.options.headers.Authorization, /^Basic /);
});

test('surfaces backend failure messages', async () => {
  await assert.rejects(
    requestTranslation({
      backendUrl: 'http://localhost:9080', username: 'demo', password: 'secret', text: 'Hello',
      fetchImpl: async () => ({ ok: false, status: 504, json: async () => ({ message: 'Provider timed out' }) }),
    }),
    /Provider timed out/
  );
});
