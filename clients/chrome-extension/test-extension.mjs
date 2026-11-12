import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import vm from 'node:vm';

const directory = new URL('./', import.meta.url);

async function source(name) {
  return readFile(new URL(name, directory), 'utf8');
}

function event() {
  const listeners = [];
  return {
    listeners,
    addListener(listener) {
      listeners.push(listener);
    },
  };
}

function element(value = '') {
  const listeners = {};
  const classes = new Set();
  return {
    value,
    textContent: '',
    disabled: false,
    open: false,
    listeners,
    classList: {
      contains: (name) => classes.has(name),
      toggle(name, enabled) {
        if (enabled) classes.add(name);
        else classes.delete(name);
      },
    },
    addEventListener(name, listener) {
      listeners[name] = listener;
    },
  };
}

function storageArea(data) {
  return {
    async get(keys) {
      const requested = typeof keys === 'string' ? [keys] : keys;
      return Object.fromEntries(requested.filter((key) => key in data).map((key) => [key, data[key]]));
    },
    async set(values) {
      Object.assign(data, values);
    },
    async remove(key) {
      delete data[key];
    },
  };
}

const flush = () => new Promise((resolve) => setImmediate(resolve));

test('manifest declares the MV3 side panel contract', async () => {
  const manifest = JSON.parse(await source('manifest.json'));
  const panelHtml = await source('sidepanel.html');

  assert.equal(manifest.manifest_version, 3);
  assert.equal(manifest.minimum_chrome_version, '116');
  assert.ok(manifest.permissions.includes('contextMenus'));
  assert.ok(manifest.permissions.includes('sidePanel'));
  assert.ok(manifest.permissions.includes('storage'));
  assert.equal(manifest.side_panel.default_path, 'sidepanel.html');
  assert.match(panelHtml, /id="result"[^>]*lang="ar"[^>]*dir="rtl"/);
});

test('context menu stores the exact selection and opens the tab side panel', async () => {
  const installed = event();
  const clicked = event();
  const session = {};
  const menus = [];
  const opened = [];
  let removedMenus = false;

  const chrome = {
    runtime: { onInstalled: installed },
    contextMenus: {
      onClicked: clicked,
      removeAll(callback) {
        removedMenus = true;
        callback();
      },
      create(options) {
        menus.push(options);
      },
    },
    sidePanel: {
      async setPanelBehavior() {},
      async open(options) {
        opened.push(options);
      },
    },
    storage: { session: storageArea(session) },
  };

  vm.runInNewContext(await source('service-worker.js'), {
    chrome,
    crypto: { randomUUID: () => 'selection-1' },
  });

  installed.listeners[0]();
  clicked.listeners[0]({
    menuItemId: 'translate-selection-to-darija',
    selectionText: 'Exact selected sentence.',
  }, { id: 42 });
  await flush();

  assert.equal(removedMenus, true);
  assert.equal(menus[0].title, 'Translate to Darija');
  assert.equal(opened.length, 1);
  assert.equal(opened[0].tabId, 42);
  assert.equal(session.pendingSelection.text, 'Exact selected sentence.');
  assert.equal(session.pendingSelection.tabId, 42);
});

test('panel preserves selection for credentials and refreshes a second selection', async () => {
  const ids = [
    'backendUrl', 'username', 'password', 'sourceLanguage', 'text',
    'translate', 'save', 'status', 'result', 'settings',
  ];
  const elements = Object.fromEntries(ids.map((id) => [id, element()]));
  elements.sourceLanguage.value = 'unspecified';

  const localData = {};
  const sessionData = {
    pendingSelection: { id: 'selection-1', tabId: 42, text: 'First exact selection.' },
  };
  const storageChanged = event();
  const requests = [];
  const translations = ['الترجمة اللولة', 'الترجمة الثانية'];

  const chrome = {
    storage: {
      local: storageArea(localData),
      session: storageArea(sessionData),
      onChanged: storageChanged,
    },
  };
  const document = {
    querySelector(selector) {
      return elements[selector.slice(1)];
    },
  };
  const fetch = async (url, options) => {
    requests.push({ url, options });
    return {
      ok: true,
      status: 200,
      async json() {
        return { translation: translations[requests.length - 1] };
      },
    };
  };

  vm.runInNewContext(await source('sidepanel.js'), {
    AbortController,
    URL,
    btoa,
    chrome,
    document,
    encodeURIComponent,
    fetch,
    setTimeout,
    unescape,
  });
  await flush();
  await flush();

  assert.equal(elements.text.value, 'First exact selection.');
  assert.equal(elements.settings.open, true);
  assert.equal(requests.length, 0);

  elements.backendUrl.value = 'http://localhost:9080';
  elements.username.value = 'demo-user';
  elements.password.value = 'private-test-password';
  await elements.save.listeners.click();

  assert.equal(requests.length, 1);
  assert.equal(requests[0].url, 'http://localhost:9080/api/translate');
  assert.equal(JSON.parse(requests[0].options.body).text, 'First exact selection.');
  assert.match(requests[0].options.headers.Authorization, /^Basic /);
  assert.equal(elements.result.textContent, 'الترجمة اللولة');
  assert.equal(localData.password, undefined);
  assert.equal(sessionData.password, 'private-test-password');

  await storageChanged.listeners[0]({
    pendingSelection: {
      newValue: { id: 'selection-2', tabId: 42, text: 'Second exact selection.' },
    },
  }, 'session');

  assert.equal(elements.text.value, 'Second exact selection.');
  assert.equal(requests.length, 2);
  assert.equal(JSON.parse(requests[1].options.body).text, 'Second exact selection.');
  assert.equal(elements.result.textContent, 'الترجمة الثانية');
  assert.equal(elements.status.textContent, 'Translation complete.');
});
