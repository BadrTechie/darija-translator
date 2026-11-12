function basicToken(username, password) {
  const value = `${username}:${password}`;
  if (typeof btoa === 'function') return btoa(unescape(encodeURIComponent(value)));
  return Buffer.from(value, 'utf8').toString('base64');
}

async function requestTranslation({ backendUrl, username, password, text, sourceLanguage = 'unspecified', fetchImpl = fetch }) {
  if (!backendUrl || !username || !password) throw new Error('Backend URL and credentials are required.');
  if (!text || !text.trim()) throw new Error('Enter text to translate.');

  const response = await fetchImpl(`${backendUrl.replace(/\/+$/, '')}/api/translate`, {
    method: 'POST',
    headers: {
      Authorization: `Basic ${basicToken(username, password)}`,
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify({ text, sourceLanguage }),
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || `Backend returned HTTP ${response.status}.`);
  if (typeof data.translation !== 'string' || !data.translation.trim()) {
    throw new Error("Backend response has no non-empty 'translation' field.");
  }
  return data.translation.trim();
}

module.exports = { requestTranslation };
