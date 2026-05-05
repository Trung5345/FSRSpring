// app.js - Dashboard / Home page logic

const CATEGORY_ICONS = {
    'Animals': 'paw-print',
    'Food': 'apple',
    'Technology': 'cpu',
    'Nature': 'leaf',
    'Emotions': 'heart',
    'Travel': 'plane',
};

const CATEGORY_COLORS = [
    'bg-indigo-100 text-indigo-700',
    'bg-green-100 text-green-700',
    'bg-yellow-100 text-yellow-700',
    'bg-pink-100 text-pink-700',
    'bg-blue-100 text-blue-700',
    'bg-orange-100 text-orange-700',
    'bg-purple-100 text-purple-700',
    'bg-teal-100 text-teal-700',
];

async function loadDashboard() {
    await Promise.all([
        loadStats(),
        loadWordOfDay(),
        loadReviewWords(),
        loadCategories(),
        loadFsrsStats(),
        loadNotifications(),
        loadStreak(),
    ]);
}

async function loadStreak() {
    try {
        const s = await fetch('/api/streak').then(r => r.json());
        const el = id => document.getElementById(id);
        if (el('streak-count')) el('streak-count').textContent = s.currentStreak ?? 0;
        if (el('streak-best'))  el('streak-best').textContent  = s.longestStreak ?? 0;
        if (el('streak-total')) el('streak-total').textContent = s.totalDaysStudied ?? 0;
        if (el('streak-fire'))  el('streak-fire').setAttribute('data-level', (s.currentStreak >= 7) ? 'hot' : (s.currentStreak >= 3 ? 'warm' : 'cool'));
    } catch(e) { /* streak widget stays with dashes */ }
}

async function loadFsrsStats() {
    try {
        const fsrs = await fetch('/api/fsrs/stats').then(r => r.json());
        const dueEl = document.getElementById('dueNow');
        const retentionEl = document.getElementById('retentionEstimate');
        if (dueEl) dueEl.textContent = fsrs.dueNow ?? 0;
        if (retentionEl) retentionEl.textContent = (fsrs.retentionEstimate ?? 0).toFixed(1) + '%';
    } catch (e) {
        console.error('Failed to load fsrs stats', e);
    }
}

async function loadNotifications() {
    try {
        const [count, list] = await Promise.all([
            fetch('/api/notifications/unread-count').then(r => r.json()),
            fetch('/api/notifications').then(r => r.json())
        ]);
        const unreadEl = document.getElementById('unreadCount');
        if (unreadEl) unreadEl.textContent = count.unread ?? 0;

        const stream = document.getElementById('notificationStream');
        if (!stream) return;

        if (!list || list.length === 0) {
            stream.innerHTML = '<p class="text-sm text-slate-500">No notification yet.</p>';
            return;
        }

        stream.innerHTML = list.slice(0, 4).map(n => `
            <a href="${escapeHtml(n.deepLink || '/learn?mode=fsrs')}" class="block border-3 border-black bg-white shadow-[2px_2px_0px_#141414] rounded-lg bg-white p-3 hover:bg-slate-50 transition">
                <p class="text-sm font-semibold text-slate-800">${escapeHtml(n.title)}</p>
                <p class="text-xs text-slate-500 mt-1">${escapeHtml(n.message)}</p>
            </a>
        `).join('');

        initLucide();
    } catch (e) {
        console.error('Failed to load notifications', e);
    }
}

async function loadStats() {
    try {
        const [wordCount, progressStats] = await Promise.all([
            fetch('/api/words/count').then(r => r.json()),
            fetch('/api/progress/stats').then(r => r.json()),
        ]);
        document.getElementById('totalWords').textContent = wordCount.count ?? 0;
        document.getElementById('masteredWords').textContent = progressStats.mastered ?? 0;
        document.getElementById('learningWords').textContent = progressStats.learning ?? 0;
        document.getElementById('accuracy').textContent =
            (progressStats.accuracy !== undefined ? progressStats.accuracy.toFixed(1) : '0') + '%';
    } catch (e) {
        console.error('Failed to load stats', e);
    }
}

async function loadWordOfDay() {
    try {
        // Try new WotD endpoint first
        let w = null;
        try {
            const res = await fetch('/api/word-of-the-day');
            if (res.ok) {
                const wod = await res.json();
                w = wod.word;
            }
        } catch(e) {}
        if (!w) {
            const words = await fetch('/api/words/random?limit=1').then(r => r.json());
            if (words && words.length) w = words[0];
        }
        if (!w) return;
        const eid = id => document.getElementById(id);
        if (eid('wod-word')) eid('wod-word').textContent = w.word;
        if (eid('wod-pronunciation')) eid('wod-pronunciation').textContent = w.pronunciation || '';
        if (eid('wod-translation')) eid('wod-translation').textContent = w.translation;
        if (eid('wod-example')) eid('wod-example').textContent = w.example ? `"${w.example}"` : '';
        // Badge row
        if (eid('wod-badge-word')) eid('wod-badge-word').textContent = w.word;
        if (eid('wod-badge-ipa')) eid('wod-badge-ipa').textContent = w.pronunciation || '';
        if (eid('wod-badge-trans')) eid('wod-badge-trans').textContent = w.translation || '';
    } catch (e) {
        console.error('Failed to load word of day', e);
    }
}

async function loadReviewWords() {
    try {
        const reviewList = await fetch('/api/progress/review').then(r => r.json());
        const container = document.getElementById('reviewList');
        const noReview = document.getElementById('noReview');

        if (!reviewList || reviewList.length === 0) {
            container.classList.add('hidden');
            noReview.classList.remove('hidden');
            return;
        }

        container.innerHTML = reviewList.slice(0, 5).map(p => `
            <div class="flex items-center justify-between p-3 bg-gray-50 rounded-xl">
                <div>
                    <span class="font-semibold text-gray-800">${escapeHtml(p.word.word)}</span>
                    <span class="text-gray-400 text-sm ml-2">${escapeHtml(p.word.translation)}</span>
                </div>
                <span class="badge-mastery-${p.mastery}">${masteryLabel(p.mastery)}</span>
            </div>
        `).join('');

        if (reviewList.length > 5) {
            container.innerHTML += `
                <a href="/learn" class="block text-center text-indigo-500 hover:underline text-sm py-2">
                    +${reviewList.length - 5} từ nữa cần ôn tập →
                </a>`;
        }
    } catch (e) {
        console.error('Failed to load review words', e);
    }
}

async function loadCategories() {
    try {
        const categories = await fetch('/api/words/categories').then(r => r.json());
        const grid = document.getElementById('categoriesGrid');
        if (!categories || categories.length === 0) return;
        grid.innerHTML = categories.map((cat, i) => {
            const icon = CATEGORY_ICONS[cat] || 'notebook-pen';
            const color = CATEGORY_COLORS[i % CATEGORY_COLORS.length];
            return `
                <a href="/vocabulary?category=${encodeURIComponent(cat)}"
                         class="bg-white rounded-xl shadow-sm p-4 text-center hover:shadow-md transition-shadow duration-200 group">
                    <div class="text-3xl mb-2 flex justify-center"><i data-lucide="${icon}" class="h-8 w-8"></i></div>
                    <p class="font-semibold text-gray-700 text-sm group-hover:text-indigo-600 transition-colors">${escapeHtml(cat)}</p>
                </a>`;
        }).join('');
        initLucide();
    } catch (e) {
        console.error('Failed to load categories', e);
    }
}

function initLucide() {
    if (window.lucide && typeof window.lucide.createIcons === 'function') {
        window.lucide.createIcons();
    }
}

function masteryLabel(mastery) {
    const labels = { NEW: 'Mới', LEARNING: 'Đang học', REVIEWING: 'Ôn tập', MASTERED: 'Đã thuộc' };
    return labels[mastery] || mastery;
}

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// Initialize
loadDashboard();
