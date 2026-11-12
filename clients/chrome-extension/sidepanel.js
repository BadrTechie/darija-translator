const elements = {
  backendUrl: document.querySelector('#backendUrl'),
  username: document.querySelector('#username'),
  password: document.querySelector('#password'),
  sourceLanguage: document.querySelector('#sourceLanguage'),
  text: document.querySelector('#text'),
  translate: document.querySelector('#translate'),
  save: document.querySelector('#save'),
  status: document.querySelector('#status'),
  result: document.querySelector('#result'),
  settings: document.querySelector('#settings'),
};

async function loadSettings() {
  const settings = await chrome.storage.local.get(['backendUrl', 'username', 'password']);
  elements.backendUrl.value = settings.backendUrl || 'http://localhost:9080';
  elements.username.value = settings.username || '';
  elements.password.value = settings.password || '';
  return settings;
}

async function saveSettings() {
  await chrome.storage.local.set({
    backendUrl: elements.backendUrl.value.trim().replace(/\/+$/, ''),
    username: elements.username.value,
    password: elements.password.value,
  });
  setStatus('Settings saved.', false);
  elements.settings.open = false;
}

async function translate() {
  const text = elements.text.value;
  if (!text.trim()) return setStatus('Select or enter text first.', true);

  const { backendUrl, username, password } = await chrome.storage.local.get([
    'backendUrl', 'username', 'password',
  ]);
  if (!backendUrl || !username || !password) {
    elements.settings.open = true;
    return setStatus('Save the backend URL and Basic-auth credentials first.', true);
  }

  elements.translate.disabled = true;
  elements.result.textContent = '';
  setStatus('Translating…', false);
  try {
    const response = await fetch(`${backendUrl.replace(/\/+$/, '')}/api/translate`, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${btoa(unescape(encodeURIComponent(`${username}:${password}`)))}`,
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify({ text, sourceLanguage: elements.sourceLanguage.value || 'unspecified' }),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.message || `Backend returned HTTP ${response.status}.`);
    if (typeof data.translation !== 'string' || !data.translation.trim()) {
      throw new Error("Backend response has no non-empty 'translation' field.");
    }
    elements.result.textContent = data.translation.trim();
    setStatus('Translation complete.', false);
  } catch (error) {
    setStatus(error.message || 'Translation failed.', true);
  } finally {
    elements.translate.disabled = false;
  }
}

function setStatus(message, isError) {
  elements.status.textContent = message;
  elements.status.classList.toggle('error', isError);
}

elements.save.addEventListener('click', saveSettings);
elements.translate.addEventListener('click', translate);

(async () => {
  await loadSettings();
  const { pendingSelection } = await chrome.storage.session.get('pendingSelection');
  if (pendingSelection) {
    elements.text.value = pendingSelection;
    await chrome.storage.session.remove('pendingSelection');
    await translate();
  }
})();
