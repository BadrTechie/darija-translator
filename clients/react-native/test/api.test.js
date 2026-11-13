const assert = require('node:assert/strict');
const test = require('node:test');
const { requestTranslation } = require('../api');

test('reads the documented translation field and sends Basic auth', async () => {
  let captured;
  const translation = await requestTranslation({
    backendUrl: ' http://localhost:9080/ ', username: 'demo', password: 'secret', text: 'Hello', sourceLanguage: ' English ',
    fetchImpl: async (url, options) => {
      captured = { url, options };
      return { ok: true, json: async () => ({ translation: 'سلام' }) };
    },
  });
  assert.equal(translation, 'سلام');
  assert.equal(captured.url, 'http://localhost:9080/api/translate');
  assert.equal(captured.options.method, 'POST');
  assert.equal(captured.options.headers.Authorization, 'Basic ZGVtbzpzZWNyZXQ=');
  assert.equal(captured.options.headers['Content-Type'], 'application/json');
  assert.deepEqual(JSON.parse(captured.options.body), { text: 'Hello', sourceLanguage: 'English' });
});

test('sends unspecified when the source language is blank', async () => {
  let requestBody;
  await requestTranslation({
    backendUrl: 'http://localhost:9080', username: 'demo', password: 'secret', text: 'Bonjour', sourceLanguage: '  ',
    fetchImpl: async (_url, options) => {
      requestBody = JSON.parse(options.body);
      return { ok: true, json: async () => ({ translation: 'سلام' }) };
    },
  });
  assert.deepEqual(requestBody, { text: 'Bonjour', sourceLanguage: 'unspecified' });
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
