/**
 * ORIGIN-FLOW — Spatial Multi-App Context Engine
 * Interactive Browser Simulator Logic
 */

document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const phoneViewport = document.getElementById('phone-viewport');
  const intentWheel = document.getElementById('intent-wheel');
  const wheelCloseBtn = document.getElementById('wheel-close-btn');
  const floatingWorkspace = document.getElementById('floating-workspace');
  const workspaceBubble = document.getElementById('workspace-bubble');
  const workspaceCard = document.getElementById('workspace-card');
  const workspaceDragHeader = document.getElementById('workspace-drag-header');
  const workspaceItemsList = document.getElementById('workspace-items-list');
  const workspaceEmptyState = document.getElementById('workspace-empty-state');
  const bubbleCount = document.getElementById('bubble-count');
  const workspaceStatsText = document.getElementById('workspace-stats-text');
  const btnMinimizeWorkspace = document.getElementById('btn-minimize-workspace');
  const btnCloseWorkspace = document.getElementById('btn-close-workspace');
  const btnClearWorkspace = document.getElementById('btn-clear-workspace');
  const toast = document.getElementById('toast');

  // Telemetry & Log Elements
  const teleTarget = document.getElementById('tele-target');
  const teleCoords = document.getElementById('tele-coords');
  const eventLog = document.getElementById('event-log');
  const statusClock = document.getElementById('status-clock');

  // State
  let currentSelectionText = "";
  let currentPackage = "com.google.android.gm";
  let workspaceItems = [];
  let isWorkspaceExpanded = true;
  let isDraggingWorkspace = false;
  let dragOffset = { x: 0, y: 0 };

  // Update clock to current time
  function updateClock() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const mins = String(now.getMinutes()).padStart(2, '0');
    statusClock.textContent = `${hours}:${mins}`;
  }
  updateClock();
  setInterval(updateClock, 30000);

  // ----------------------------------------------------
  // Logging & Telemetry Helper
  // ----------------------------------------------------
  function logEvent(type, message) {
    const entry = document.createElement('div');
    entry.className = `log-entry ${type}`;
    const timeStr = new Date().toLocaleTimeString().split(' ')[0];
    entry.textContent = `[${timeStr}] ${message}`;
    eventLog.appendChild(entry);
    eventLog.scrollTop = eventLog.scrollHeight;
  }

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('show');
    setTimeout(() => {
      toast.classList.remove('show');
    }, 2400);
  }

  // ----------------------------------------------------
  // App Switcher Navigation
  // ----------------------------------------------------
  const appTabs = document.querySelectorAll('.app-tab');
  const appContents = document.querySelectorAll('.app-content');

  const appPackageMap = {
    mail: "com.google.android.gm",
    slack: "com.slack",
    browser: "com.android.chrome"
  };

  appTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      const appKey = tab.dataset.app;
      currentPackage = appPackageMap[appKey] || "system.app";

      appTabs.forEach(t => t.classList.remove('active'));
      appContents.forEach(c => c.classList.remove('active'));

      tab.classList.add('active');
      document.getElementById(`app-${appKey}`)?.classList.add('active');

      teleTarget.textContent = currentPackage;
      logEvent('system', `App window switched: ${currentPackage}`);
      dismissIntentWheel();
    });
  });

  // ----------------------------------------------------
  // Text Selection Detection (Accessibility Engine Hook)
  // ----------------------------------------------------
  phoneViewport.addEventListener('mouseup', handleTextSelection);

  function handleTextSelection(e) {
    // If clicking on the intent wheel or floating workspace, ignore
    if (intentWheel.contains(e.target) || floatingWorkspace.contains(e.target)) {
      return;
    }

    const selection = window.getSelection();
    const text = selection.toString().trim();

    if (text && text.length > 2) {
      currentSelectionText = text;

      // Calculate anchor relative to phoneViewport
      const viewportRect = phoneViewport.getBoundingClientRect();
      const clientX = e.clientX;
      const clientY = e.clientY;

      let relativeX = clientX - viewportRect.left;
      let relativeY = clientY - viewportRect.top;

      // Constrain within phone viewport bounds
      relativeX = Math.max(90, Math.min(viewportRect.width - 90, relativeX));
      relativeY = Math.max(110, Math.min(viewportRect.height - 110, relativeY));

      showIntentWheel(relativeX, relativeY, text);
    }
  }

  // ----------------------------------------------------
  // Radial Intent-Wheel Control
  // ----------------------------------------------------
  function showIntentWheel(x, y, text) {
    currentSelectionText = text;
    intentWheel.style.left = `${x}px`;
    intentWheel.style.top = `${y}px`;
    intentWheel.classList.add('visible');

    teleCoords.textContent = `x: ${Math.round(x)}px, y: ${Math.round(y)}px`;
    teleTarget.textContent = currentPackage;
    logEvent('event', `TYPE_VIEW_TEXT_SELECTION_CHANGED: "${text.substring(0, 32)}..."`);
    logEvent('action', `Intent-Wheel anchored at (${Math.round(x)}, ${Math.round(y)})`);

    highlightStep(2);
  }

  function dismissIntentWheel() {
    intentWheel.classList.remove('visible');
  }

  wheelCloseBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    dismissIntentWheel();
    logEvent('system', 'Intent-Wheel dismissed by user');
  });

  // Action Buttons on Intent Wheel
  const wheelNodes = document.querySelectorAll('.wheel-node');
  wheelNodes.forEach(node => {
    node.addEventListener('click', (e) => {
      e.stopPropagation();
      const action = node.dataset.action;
      executeAction(action, currentSelectionText);
      dismissIntentWheel();
    });
  });

  function executeAction(action, text) {
    logEvent('action', `User selected action: [${action}]`);
    let processedResult = "";
    let actionLabel = "";

    switch (action) {
      case 'SUMMARIZE':
        actionLabel = "Summarize";
        processedResult = `📌 Summary: "${text.length > 80 ? text.substring(0, 80) + '...' : text}" — Distilled into actionable context.`;
        break;
      case 'AUTO_SCHEDULE':
        actionLabel = "Auto-Schedule";
        processedResult = `📅 Scheduled Review: Agenda detected from text; calendar invitation drafted for tomorrow 3:30 PM.`;
        break;
      case 'SOLVE_EXTRACT':
        actionLabel = "Solve/Extract";
        processedResult = `⚡ Parsed Entity: Exception 'ViewTreeLifecycleOwner' mapped to missing lifecycle injection in OverlayLifecycleOwner.`;
        break;
      case 'SAVE_TO_STACK':
        actionLabel = "Save";
        processedResult = text;
        break;
      default:
        actionLabel = "Captured";
        processedResult = text;
    }

    addToWorkspace(actionLabel, text, processedResult);
    showToast(`Pinned "${actionLabel}" to Floating Workspace`);
    highlightStep(3);
  }

  // ----------------------------------------------------
  // Floating Workspace Stack (Movable & Collapsible)
  // ----------------------------------------------------
  function addToWorkspace(actionType, originalText, processedResult) {
    const newItem = {
      id: Date.now(),
      actionType,
      originalText,
      processedResult,
      source: currentPackage,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    workspaceItems.unshift(newItem);
    renderWorkspace();

    // Ensure workspace is visible and expanded
    floatingWorkspace.style.display = 'block';
    if (!isWorkspaceExpanded) {
      setWorkspaceExpanded(true);
    }
  }

  function renderWorkspace() {
    bubbleCount.textContent = workspaceItems.length;
    workspaceStatsText.textContent = `${workspaceItems.length} item${workspaceItems.length === 1 ? '' : 's'} buffered`;

    // Remove old item cards (keep empty state element)
    const existingCards = workspaceItemsList.querySelectorAll('.workspace-item');
    existingCards.forEach(card => card.remove());

    if (workspaceItems.length === 0) {
      workspaceEmptyState.style.display = 'block';
    } else {
      workspaceEmptyState.style.display = 'none';

      workspaceItems.forEach(item => {
        const itemEl = document.createElement('div');
        itemEl.className = 'workspace-item';

        const tagClass = getTagClass(item.actionType);

        itemEl.innerHTML = `
          <div class="item-badge-row">
            <span class="item-action-tag ${tagClass}">${item.actionType}</span>
            <div class="item-actions-btns">
              <button class="item-mini-btn btn-copy" title="Copy to clipboard" data-id="${item.id}">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>
              </button>
              <button class="item-mini-btn btn-delete" title="Delete" data-id="${item.id}">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
              </button>
            </div>
          </div>
          <div class="item-text">${item.processedResult}</div>
          <div class="item-source">via ${item.source} • ${item.timestamp}</div>
        `;

        workspaceItemsList.appendChild(itemEl);
      });

      // Hook up copy and delete buttons
      workspaceItemsList.querySelectorAll('.btn-copy').forEach(btn => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          const id = Number(btn.dataset.id);
          const item = workspaceItems.find(i => i.id === id);
          if (item) {
            navigator.clipboard?.writeText(item.processedResult);
            showToast('Copied to system clipboard!');
            logEvent('action', 'ClipboardManager: Copied processed context');
          }
        });
      });

      workspaceItemsList.querySelectorAll('.btn-delete').forEach(btn => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          const id = Number(btn.dataset.id);
          workspaceItems = workspaceItems.filter(i => i.id !== id);
          renderWorkspace();
          logEvent('system', 'Workspace item deleted');
        });
      });
    }
  }

  function getTagClass(type) {
    switch (type.toLowerCase()) {
      case 'summarize': return 'tag-summarize';
      case 'auto-schedule': return 'tag-schedule';
      case 'solve/extract': return 'tag-solve';
      default: return 'tag-stack';
    }
  }

  function setWorkspaceExpanded(expand) {
    isWorkspaceExpanded = expand;
    if (expand) {
      workspaceBubble.style.display = 'none';
      workspaceCard.style.display = 'flex';
      logEvent('system', 'Workspace expanded to card layout');
    } else {
      workspaceCard.style.display = 'none';
      workspaceBubble.style.display = 'flex';
      logEvent('system', 'Workspace collapsed into floating bubble');
    }
  }

  // Toggle Collapse / Expand
  btnMinimizeWorkspace.addEventListener('click', (e) => {
    e.stopPropagation();
    setWorkspaceExpanded(false);
  });

  workspaceBubble.addEventListener('click', (e) => {
    e.stopPropagation();
    setWorkspaceExpanded(true);
  });

  btnCloseWorkspace.addEventListener('click', (e) => {
    e.stopPropagation();
    floatingWorkspace.style.display = 'none';
    logEvent('system', 'Floating Workspace overlay closed');
  });

  btnClearWorkspace.addEventListener('click', (e) => {
    e.stopPropagation();
    workspaceItems = [];
    renderWorkspace();
    showToast('Workspace stack cleared');
    logEvent('system', 'Workspace items cleared');
  });

  // ----------------------------------------------------
  // Draggable Floating Workspace (Pointer Gestures)
  // ----------------------------------------------------
  const dragHandles = [workspaceDragHeader, workspaceBubble];

  dragHandles.forEach(handle => {
    handle.addEventListener('pointerdown', (e) => {
      // Don't drag if clicking buttons
      if (e.target.closest('button')) return;

      isDraggingWorkspace = true;
      const rect = floatingWorkspace.getBoundingClientRect();
      dragOffset.x = e.clientX - rect.left;
      dragOffset.y = e.clientY - rect.top;

      floatingWorkspace.setPointerCapture?.(e.pointerId);
      e.preventDefault();
    });
  });

  window.addEventListener('pointermove', (e) => {
    if (!isDraggingWorkspace) return;

    const viewportRect = phoneViewport.getBoundingClientRect();
    let newX = e.clientX - viewportRect.left - dragOffset.x;
    let newY = e.clientY - viewportRect.top - dragOffset.y;

    // Bounds containment
    const maxX = viewportRect.width - (isWorkspaceExpanded ? 320 : 60);
    const maxY = viewportRect.height - (isWorkspaceExpanded ? 240 : 60);

    newX = Math.max(10, Math.min(maxX, newX));
    newY = Math.max(50, Math.min(maxY, newY));

    floatingWorkspace.style.left = `${newX}px`;
    floatingWorkspace.style.top = `${newY}px`;
    floatingWorkspace.style.right = 'auto';
  });

  window.addEventListener('pointerup', () => {
    if (isDraggingWorkspace) {
      isDraggingWorkspace = false;
      logEvent('system', `Workspace moved to position (${floatingWorkspace.style.left}, ${floatingWorkspace.style.top})`);
    }
  });

  // ----------------------------------------------------
  // Sidebar Interactive Buttons & Scenarios
  // ----------------------------------------------------
  document.getElementById('simulate-select-btn').addEventListener('click', () => {
    triggerPresetScenario('meeting');
  });

  document.getElementById('simulate-wheel-btn').addEventListener('click', () => {
    const viewportRect = phoneViewport.getBoundingClientRect();
    showIntentWheel(
      viewportRect.width / 2,
      viewportRect.height / 2,
      "Manual test trigger payload across active application."
    );
  });

  document.getElementById('simulate-workspace-btn').addEventListener('click', () => {
    if (floatingWorkspace.style.display === 'none') {
      floatingWorkspace.style.display = 'block';
      setWorkspaceExpanded(true);
    } else {
      setWorkspaceExpanded(!isWorkspaceExpanded);
    }
  });

  // Pre-set Scenarios
  const scenarioChips = document.querySelectorAll('.chip-btn');
  scenarioChips.forEach(chip => {
    chip.addEventListener('click', () => {
      const scenario = chip.dataset.scenario;
      triggerPresetScenario(scenario);
    });
  });

  function triggerPresetScenario(type) {
    if (type === 'meeting') {
      // Switch to Gmail
      document.querySelector('.app-tab[data-app="mail"]').click();
      const targetText = "final Q4 Spatial Engine sprint review tomorrow at 3:30 PM with the DeepMind engineers";
      highlightSimulatedText('.demo-target-1', targetText, 180, 260);
    } else if (type === 'code') {
      // Switch to Slack
      document.querySelector('.app-tab[data-app="slack"]').click();
      const targetText = "solve this issue and verify our custom OverlayLifecycleOwner attachment";
      highlightSimulatedText('.demo-target-code', targetText, 190, 310);
    } else if (type === 'summary') {
      // Switch to Chrome
      document.querySelector('.app-tab[data-app="browser"]').click();
      const targetText = "Spatial context engines replace this friction by projecting context-aware radial overlays directly at the user's focus coordinate";
      highlightSimulatedText('.demo-target-article', targetText, 185, 290);
    }
  }

  function highlightSimulatedText(selector, text, anchorX, anchorY) {
    const el = document.querySelector(selector);
    if (el) {
      el.style.backgroundColor = 'rgba(0, 229, 255, 0.45)';
      setTimeout(() => {
        el.style.backgroundColor = '';
      }, 2000);
    }
    showIntentWheel(anchorX, anchorY, text);
    showToast('Simulated text selection detected!');
  }

  function highlightStep(stepNum) {
    document.querySelectorAll('.step-item').forEach(el => el.classList.remove('active'));
    document.getElementById(`step-${stepNum}`)?.classList.add('active');
  }

  // ----------------------------------------------------
  // Guided Auto-Tour Walkthrough
  // ----------------------------------------------------
  document.getElementById('btn-quick-tour').addEventListener('click', runAutoTour);

  function runAutoTour() {
    showToast('Starting automated walkthrough demo...');
    logEvent('system', 'Auto-Tour started');

    // Step 1: Switch to Mail and select text
    highlightStep(1);
    document.querySelector('.app-tab[data-app="mail"]').click();

    setTimeout(() => {
      triggerPresetScenario('meeting');
      highlightStep(2);

      // Step 2: Auto-click "Auto-Schedule" on the wheel
      setTimeout(() => {
        const scheduleBtn = document.querySelector('.wheel-node[data-action="AUTO_SCHEDULE"]');
        scheduleBtn?.click();
        highlightStep(3);

        // Step 3: Switch to Slack, solve code error
        setTimeout(() => {
          triggerPresetScenario('code');

          setTimeout(() => {
            const solveBtn = document.querySelector('.wheel-node[data-action="SOLVE_EXTRACT"]');
            solveBtn?.click();

            showToast('Walkthrough complete! Check the workspace stack.');
            logEvent('system', 'Auto-Tour finished with 2 items captured in floating stack.');
          }, 1200);
        }, 1800);
      }, 1400);
    }, 800);
  }

  // Reset Demo
  document.getElementById('btn-reset').addEventListener('click', () => {
    dismissIntentWheel();
    workspaceItems = [];
    renderWorkspace();
    document.querySelector('.app-tab[data-app="mail"]').click();
    highlightStep(1);
    showToast('Simulator reset to initial state');
    logEvent('system', 'Simulator reset');
  });

  // Initial setup: Add 1 sample item so workspace card is visually meaningful right away
  addToWorkspace(
    "Summarize",
    "Origin-Flow spatial overlay architecture",
    "📌 Initialized: Floating Workspace Stack active over display."
  );
  setWorkspaceExpanded(true);
});
