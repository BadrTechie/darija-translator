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

let activeRequest = null;
let latestRequestId = 0;
const handledSelections = new Set();

async function loadSettings() {
  const [settings, session] = await Promise.all([
    chrome.storage.local.get(['backendUrl', 'username']),
    chrome.storage.session.get('password'),
  ]);
  elements.backendUrl.value = settings.backendUrl || 'http://localhost:9080';
  elements.username.value = settings.username || '';
  elements.password.value = session.password || '';
  return settings;
}

async function saveSettings() {
  const backendUrl = elements.backendUrl.value.trim().replace(/\/+$/, '');
  if (!isHttpUrl(backendUrl)) {
    elements.settings.open = true;
    return setStatus('Enter a valid HTTP or HTTPS backend URL.', true);
  }
  if (!elements.username.value || !elements.password.value) {
    elements.settings.open = true;
    return setStatus('Enter the Basic-auth username and password.', true);
  }

  await Promise.all([
    chrome.storage.local.set({ backendUrl, username: elements.username.value }),
    chrome.storage.session.set({ password: elements.password.value }),
  ]);
  setStatus('Settings saved.', false);
  elements.settings.open = false;

  // Keep a context-menu selection while credentials are entered, then resume.
  if (elements.text.value.trim()) await translate();
}

async function translate() {
  const text = elements.text.value;
  if (!text.trim()) return setStatus('Select or enter text first.', true);

  const [{ backendUrl, username }, { password }] = await Promise.all([
    chrome.storage.local.get(['backendUrl', 'username']),
    chrome.storage.session.get('password'),
  ]);
  if (!backendUrl || !username || !password) {
    elements.settings.open = true;
    return setStatus('Enter and save the backend URL and Basic-auth credentials.', true);
  }

  activeRequest?.abort();
  activeRequest = new AbortController();
  const requestId = ++latestRequestId;
  elements.translate.disabled = true;
  elements.result.textContent = '';
  setStatus('Translating...', false);
  try {
    const response = await fetch(`${backendUrl.replace(/\/+$/, '')}/api/translate`, {
      method: 'POST',
      signal: activeRequest.signal,
      headers: {
        Authorization: `Basic ${btoa(unescape(encodeURIComponent(`${username}:${password}`)))}`,
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify({ text, sourceLanguage: elements.sourceLanguage.value || 'unspecified' }),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(responseError(response.status, data));
    if (typeof data.translation !== 'string' || !data.translation.trim()) {
      throw new Error("Backend response has no non-empty 'translation' field.");
    }
    if (requestId !== latestRequestId) return;
    elements.result.textContent = data.translation.trim();
    setStatus('Translation complete.', false);
  } catch (error) {
    if (error.name === 'AbortError' || requestId !== latestRequestId) return;
    setStatus(error.message || 'Translation failed.', true);
  } finally {
    if (requestId === latestRequestId) {
      elements.translate.disabled = false;
      activeRequest = null;
    }
  }
}

function responseError(status, data) {
  if (status === 401 || status === 403) {
    return 'The backend rejected those credentials. Check the username and password.';
  }
  const message = typeof data.message === 'string' ? data.message.trim() : '';
  if (status === 400) return message || 'The backend rejected the submitted text.';
  if ([502, 503, 504].includes(status)) {
    return message
      ? `Translation provider error: ${message}`
      : 'The translation provider is unavailable. Try again.';
  }
  return message || `Backend returned HTTP ${status}.`;
}

function isHttpUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

async function acceptSelection(pendingSelection) {
  const selection = typeof pendingSelection === 'string'
    ? { id: pendingSelection, text: pendingSelection }
    : pendingSelection;
  if (!selection?.text || handledSelections.has(selection.id)) return;

  handledSelections.add(selection.id);
  elements.text.value = selection.text;
  elements.result.textContent = '';
  await chrome.storage.session.remove('pendingSelection');
  await translate();
}

function setStatus(message, isError) {
  elements.status.textContent = message;
  elements.status.classList.toggle('error', isError);
}

elements.save.addEventListener('click', saveSettings);
elements.translate.addEventListener('click', translate);
chrome.storage.onChanged.addListener(async (changes, area) => {
  if (area === 'session' && changes.pendingSelection?.newValue) {
    await acceptSelection(changes.pendingSelection.newValue);
  }
});

(async () => {
  await loadSettings();
  const { pendingSelection } = await chrome.storage.session.get('pendingSelection');
  if (pendingSelection) await acceptSelection(pendingSelection);
})();
