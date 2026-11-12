const MENU_ID = 'translate-selection-to-darija';

chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.create({
    id: MENU_ID,
    title: 'Translate selection to Darija',
    contexts: ['selection'],
  });
  chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
});

chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (info.menuItemId !== MENU_ID || !info.selectionText || !tab?.id) return;

  // The sidePanel.open call remains directly in the user-gesture handler.
  chrome.storage.session.set({ pendingSelection: info.selectionText });
  chrome.sidePanel.open({ tabId: tab.id });
});
