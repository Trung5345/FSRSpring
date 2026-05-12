// notifications.js — shared notification dropdown widget
// Expects a <div id="notifWidget"> in the header.
// Call initNotifications() on DOMContentLoaded.

(function () {
  const POLL_MS = 60_000; // refresh every 60s

  function formatRelative(dateStr) {
    if (!dateStr) return '';
    const diff = Date.now() - new Date(dateStr).getTime();
    const m = Math.floor(diff / 60000);
    if (m < 1) return 'Just now';
    if (m < 60) return `${m}m ago`;
    const h = Math.floor(m / 60);
    if (h < 24) return `${h}h ago`;
    return `${Math.floor(h / 24)}d ago`;
  }

  const TYPE_ICON = {
    REVIEW_REMINDER: 'notifications_active',
    STREAK_ALERT: 'local_fire_department',
    ACHIEVEMENT: 'emoji_events',
    SYSTEM: 'info',
  };
  const TYPE_COLOR = {
    REVIEW_REMINDER: '#006590',
    STREAK_ALERT: '#f4bf00',
    ACHIEVEMENT: '#bb1522',
    SYSTEM: '#6e7881',
  };

  function buildWidget() {
    const wrap = document.getElementById('notifWidget');
    if (!wrap) return;
    wrap.style.position = 'relative';
    wrap.innerHTML = `
      <button id="notifBtn" style="position:relative;background:none;border:none;cursor:pointer;padding:6px;border-radius:50%;display:flex;align-items:center;justify-content:center;transition:background .15s;" onmouseenter="this.style.background='#efeded'" onmouseleave="this.style.background='transparent'">
        <span class="material-symbols-outlined" style="font-size:26px;color:#3e4850;font-variation-settings:'FILL' 0,'wght' 400,'GRAD' 0,'opsz' 24;">notifications</span>
        <span id="notifBadge" style="display:none;position:absolute;top:4px;right:4px;min-width:16px;height:16px;background:#ba1a1a;color:#fff;font-family:Lexend,sans-serif;font-size:10px;font-weight:700;border-radius:999px;display:flex;align-items:center;justify-content:center;padding:0 3px;line-height:1;"></span>
      </button>
      <div id="notifDropdown" style="display:none;position:absolute;top:calc(100% + 10px);right:0;width:360px;background:#fff;border:2px solid #bdc8d2;border-radius:16px;box-shadow:0 8px 32px rgba(0,0,0,.12);z-index:9999;overflow:hidden;">
        <div style="display:flex;align-items:center;justify-content:space-between;padding:14px 20px 10px;border-bottom:1px solid #efeded;">
          <span style="font-family:Lexend,sans-serif;font-size:15px;font-weight:700;color:#1b1c1c;">Notifications</span>
          <button id="notifMarkAll" style="background:none;border:none;font-family:Lexend,sans-serif;font-size:12px;font-weight:700;color:#006590;cursor:pointer;padding:4px 8px;border-radius:6px;" onmouseenter="this.style.background='#c8e6ff'" onmouseleave="this.style.background='transparent'">Mark all read</button>
        </div>
        <div id="notifList" style="max-height:380px;overflow-y:auto;"></div>
        <div id="notifEmpty" style="display:none;padding:32px 20px;text-align:center;color:#6e7881;font-family:Nunito Sans,sans-serif;font-size:15px;">
          <span class="material-symbols-outlined" style="font-size:40px;display:block;margin-bottom:8px;color:#bdc8d2;">notifications_off</span>
          No notifications
        </div>
      </div>`;

    document.getElementById('notifBtn').addEventListener('click', (e) => {
      e.stopPropagation();
      toggleDropdown();
    });
    document.getElementById('notifMarkAll').addEventListener('click', markAllRead);
    document.addEventListener('click', () => closeDropdown());
    document.getElementById('notifDropdown').addEventListener('click', e => e.stopPropagation());
  }

  function toggleDropdown() {
    const dd = document.getElementById('notifDropdown');
    if (!dd) return;
    const open = dd.style.display !== 'none';
    open ? closeDropdown() : openDropdown();
  }

  function openDropdown() {
    const dd = document.getElementById('notifDropdown');
    if (dd) dd.style.display = 'block';
    refresh();
  }

  function closeDropdown() {
    const dd = document.getElementById('notifDropdown');
    if (dd) dd.style.display = 'none';
  }

  let _notifications = [];

  async function refresh() {
    try {
      const [countData, items] = await Promise.all([
        fetch('/api/notifications/unread-count').then(r => r.json()),
        fetch('/api/notifications').then(r => r.json()),
      ]);
      _notifications = Array.isArray(items) ? items : [];
      updateBadge(countData.unread ?? 0);
      renderList(_notifications);
    } catch (e) {
      // silent fail
    }
  }

  function updateBadge(count) {
    const badge = document.getElementById('notifBadge');
    if (!badge) return;
    if (count > 0) {
      badge.style.display = 'flex';
      badge.textContent = count > 99 ? '99+' : String(count);
    } else {
      badge.style.display = 'none';
    }
    // filled icon when unread
    const icon = document.querySelector('#notifBtn .material-symbols-outlined');
    if (icon) icon.style.fontVariationSettings = count > 0
      ? "'FILL' 1,'wght' 400,'GRAD' 0,'opsz' 24"
      : "'FILL' 0,'wght' 400,'GRAD' 0,'opsz' 24";
  }

  function renderList(items) {
    const list = document.getElementById('notifList');
    const empty = document.getElementById('notifEmpty');
    if (!list || !empty) return;
    if (!items.length) {
      list.innerHTML = '';
      empty.style.display = 'block';
      return;
    }
    empty.style.display = 'none';
    list.innerHTML = items.map(n => {
      const icon = TYPE_ICON[n.type] || 'notifications';
      const color = TYPE_COLOR[n.type] || '#6e7881';
      const unread = !n.isRead;
      return `
        <div data-id="${n.id}" class="notif-item" style="display:flex;gap:12px;padding:12px 20px;border-bottom:1px solid #efeded;cursor:pointer;background:${unread ? '#f0f8ff' : '#fff'};transition:background .15s;"
          onmouseenter="this.style.background='#f5f3f3'" onmouseleave="this.style.background='${unread ? '#f0f8ff' : '#fff'}'"
          onclick="window.__notifRead(${n.id}, this, '${n.deepLink || ''}')">
          <div style="flex-shrink:0;width:36px;height:36px;background:${color}20;border-radius:50%;display:flex;align-items:center;justify-content:center;">
            <span class="material-symbols-outlined icon-filled" style="font-size:18px;color:${color};font-variation-settings:'FILL' 1,'wght' 400,'GRAD' 0,'opsz' 24;">${icon}</span>
          </div>
          <div style="flex:1;min-width:0;">
            <div style="font-family:Lexend,sans-serif;font-size:13px;font-weight:${unread ? '700' : '600'};color:#1b1c1c;line-height:1.3;">${escHtml(n.title)}</div>
            <div style="font-family:Nunito Sans,sans-serif;font-size:13px;color:#3e4850;margin-top:2px;line-height:1.4;">${escHtml(n.message)}</div>
            <div style="font-family:Nunito Sans,sans-serif;font-size:11px;color:#6e7881;margin-top:4px;">${formatRelative(n.createdAt)}</div>
          </div>
          ${unread ? '<div style="flex-shrink:0;width:8px;height:8px;background:#006590;border-radius:50%;margin-top:6px;"></div>' : ''}
        </div>`;
    }).join('');
  }

  async function markAllRead() {
    const unread = _notifications.filter(n => !n.isRead);
    await Promise.allSettled(unread.map(n =>
      fetch(`/api/notifications/${n.id}/read`, { method: 'POST' })
    ));
    refresh();
  }

  // Exposed globally so onclick inline can call it
  window.__notifRead = async function (id, el, deepLink) {
    const notif = _notifications.find(n => n.id === id);
    if (notif && !notif.isRead) {
      try { await fetch(`/api/notifications/${id}/read`, { method: 'POST' }); } catch (_) {}
      notif.isRead = true;
      el.style.background = '#fff';
      el.querySelector('div[style*="width:8px"]')?.remove();
      const titleEl = el.querySelector('div[style*="font-family:Lexend"]');
      if (titleEl) titleEl.style.fontWeight = '600';
      const unreadCount = _notifications.filter(n => !n.isRead).length;
      updateBadge(unreadCount);
    }
    if (deepLink && deepLink !== 'undefined' && deepLink !== 'null') {
      window.location.href = deepLink;
    }
  };

  function escHtml(t) {
    if (!t) return '';
    return String(t).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  function initNotifications() {
    buildWidget();
    refresh();
    setInterval(refresh, POLL_MS);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initNotifications);
  } else {
    initNotifications();
  }
})();
