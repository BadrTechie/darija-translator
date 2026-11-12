const MENU_ID = 'translate-selection-to-darija';

chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
      id: MENU_ID,
      title: 'Translate to Darija',
      contexts: ['selection'],
    });
  });
  chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
});

chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (info.menuItemId !== MENU_ID || !info.selectionText || !tab?.id) return;

  // Start both operations inside the user gesture. The panel handles the
  // storage event whether it was already open or has just been created.
  chrome.storage.session.set({
    pendingSelection: {
      id: crypto.randomUUID(),
      tabId: tab.id,
      text: info.selectionText,
    },
  });
  chrome.sidePanel.open({ tabId: tab.id });
});
