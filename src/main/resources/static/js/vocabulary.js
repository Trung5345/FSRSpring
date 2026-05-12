// vocabulary.js - Vocabulary management page

let allWords = [];
let categories = [];
let topicsMap = {};
let deleteWordId = null;

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function difficultyBadge(d) {
    const map = {
        BEGINNER: '<span style="font-size:.7rem;background:#c8e6ff;color:#006590;padding:2px 8px;border-radius:99px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;">Beginner</span>',
        INTERMEDIATE: '<span style="font-size:.7rem;background:#ffdf92;color:#6e5400;padding:2px 8px;border-radius:99px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;">Intermediate</span>',
        ADVANCED: '<span style="font-size:.7rem;background:#ffdad6;color:#93000a;padding:2px 8px;border-radius:99px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;">Advanced</span>',
    };
    return map[d] || (d ? `<span style="font-size:.7rem;background:#efeded;color:#3e4850;padding:2px 8px;border-radius:99px;font-weight:700;">${escapeHtml(d)}</span>` : '');
}

function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer') || (() => {
        const c = document.createElement('div');
        c.id = 'toastContainer';
        c.style.cssText = 'position:fixed;bottom:2rem;right:2rem;z-index:9999;display:flex;flex-direction:column;gap:8px;pointer-events:none;';
        document.body.appendChild(c);
        return c;
    })();
    const colors = { success: 'background:#006590;color:#fff;', error: 'background:#ba1a1a;color:#fff;', info: 'background:#e3e2e2;color:#1b1c1c;' };
    const icons = { success: 'check_circle', error: 'error', info: 'info' };
    const toast = document.createElement('div');
    toast.style.cssText = `pointer-events:auto;display:flex;align-items:center;gap:12px;padding:12px 20px;border-radius:12px;font-family:Nunito Sans,sans-serif;font-weight:500;font-size:17px;${colors[type]||colors.success}opacity:0;transition:opacity 0.3s,transform 0.3s;transform:translateY(16px);`;
    toast.innerHTML = `<span class="material-symbols-outlined icon-filled" style="font-size:20px;">${icons[type]||icons.success}</span><span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);
    requestAnimationFrame(() => { toast.style.opacity='1'; toast.style.transform='translateY(0)'; });
    setTimeout(() => { toast.style.opacity='0'; toast.style.transform='translateY(8px)'; setTimeout(() => toast.remove(), 350); }, 2800);
}

async function loadWords() {
    try {
        [allWords, categories] = await Promise.all([
            fetch('/api/words').then(r => r.json()),
            fetch('/api/words/categories').then(r => r.json()),
        ]);

        // Load topics for filter dropdown
        try {
            const topics = await fetch('/api/topics').then(r => r.json());
            const topicSel = document.getElementById('topicFilter');
            if (topicSel) {
                topicSel.innerHTML = '<option value="">All Topics</option>';
                topics.forEach(t => {
                    topicsMap[t.id] = t;
                    topicSel.innerHTML += `<option value="${t.id}">${escapeHtml((t.iconEmoji||'') + ' ' + t.name)}</option>`;
                });
            }
        } catch(e) {}

        // Populate category filter
        const catFilter = document.getElementById('categoryFilter');
        const catList = document.getElementById('categoryList');
        if (catFilter) catFilter.innerHTML = '<option value="">Tất Cả Danh Mục</option>';
        if (catList) catList.innerHTML = '';
        categories.forEach(cat => {
            if (catFilter) catFilter.innerHTML += `<option value="${escapeHtml(cat)}">${escapeHtml(cat)}</option>`;
            if (catList)   catList.innerHTML += `<option value="${escapeHtml(cat)}">`;
        });

        // Apply URL params
        const params = new URLSearchParams(window.location.search);
        if (params.get('category') && catFilter) catFilter.value = params.get('category');

        renderWords(allWords);
    } catch (e) {
        console.error('Failed to load words', e);
    }
}

function filterWords() {
    const search = document.getElementById('searchInput')?.value?.toLowerCase() || '';
    const category = document.getElementById('categoryFilter')?.value || '';
    const difficulty = document.getElementById('difficultyFilter')?.value || '';
    const topicId = document.getElementById('topicFilter')?.value || '';
    const cefr = document.getElementById('cefrFilter')?.value || '';

    const filtered = allWords.filter(w => {
        const matchSearch = !search ||
            w.word.toLowerCase().includes(search) ||
            (w.translation||'').toLowerCase().includes(search);
        const matchCat   = !category  || w.category === category;
        const matchDiff  = !difficulty || w.difficulty === difficulty;
        const matchTopic = !topicId   || (w.topic && String(w.topic.id) === topicId);
        const matchCefr  = !cefr      || w.cefrLevel === cefr;
        return matchSearch && matchCat && matchDiff && matchTopic && matchCefr;
    });
    renderWords(filtered);
}

function renderWords(words) {
    const grid = document.getElementById('wordGrid');
    const empty = document.getElementById('emptyState');
    document.getElementById('wordCount').textContent = `${words.length} từ`;

    if (words.length === 0) {
        grid.innerHTML = '';
        empty.classList.remove('hidden');
        return;
    }
    empty.classList.add('hidden');

    grid.innerHTML = words.map(w => `
        <div class="word-card" style="background:#fff;border:2px solid #bdc8d2;border-radius:12px;padding:20px;display:flex;flex-direction:column;gap:8px;transition:border-color .15s,box-shadow .15s;" onmouseenter="this.style.borderColor='#006590';this.style.boxShadow='0 2px 8px rgba(0,101,144,.12)'" onmouseleave="this.style.borderColor='#bdc8d2';this.style.boxShadow='none'">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px;">
                <div style="flex:1;min-width:0;">
                    <h3 style="font-size:1.1rem;font-weight:700;color:#1b1c1c;font-family:Lexend,sans-serif;margin:0 0 2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(w.word)}</h3>
                    ${w.pronunciation ? `<p style="color:#006590;font-size:0.78rem;font-family:monospace;margin:0;">${escapeHtml(w.pronunciation)}</p>` : ''}
                </div>
                ${difficultyBadge(w.difficulty)}
            </div>
            <p style="color:#3e4850;font-weight:600;font-size:0.9rem;margin:0;">${escapeHtml(w.translation)}</p>
            ${w.example ? `<p style="color:#6e7881;font-size:0.8rem;font-style:italic;margin:0;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">&ldquo;${escapeHtml(w.example)}&rdquo;</p>` : ''}
            <div style="display:flex;flex-wrap:wrap;gap:4px;">
              ${w.category  ? `<span style="font-size:.68rem;background:#efeded;color:#3e4850;padding:2px 8px;border-radius:99px;border:1px solid #bdc8d2;font-weight:600;">${escapeHtml(w.category)}</span>` : ''}
              ${w.topic     ? `<span style="font-size:.68rem;background:#c8e6ff;color:#006590;padding:2px 8px;border-radius:99px;font-weight:600;">${escapeHtml(w.topic.name)}</span>` : ''}
              ${w.cefrLevel ? `<span style="font-size:.68rem;background:#ffdf92;color:#6e5400;padding:2px 8px;border-radius:99px;font-weight:600;">${escapeHtml(w.cefrLevel)}</span>` : ''}
              ${w.partOfSpeech ? `<span style="font-size:.68rem;background:#e3e2e2;color:#6e7881;padding:2px 8px;border-radius:99px;">${escapeHtml(w.partOfSpeech)}</span>` : ''}
            </div>
            <div style="display:flex;gap:8px;margin-top:auto;padding-top:12px;border-top:1px solid #e3e2e2;">
                <button onclick="openEditModal(${w.id})" style="flex:1;display:flex;align-items:center;justify-content:center;gap:6px;padding:8px 12px;border-radius:8px;border:2px solid #bdc8d2;background:#fff;color:#006590;font-family:Lexend,sans-serif;font-size:.75rem;font-weight:700;letter-spacing:.05em;text-transform:uppercase;cursor:pointer;transition:background .15s;" onmouseenter="this.style.background='#c8e6ff'" onmouseleave="this.style.background='#fff'">
                    <span class="material-symbols-outlined" style="font-size:15px;">edit</span>Edit
                </button>
                <button onclick="openDeleteModal(${w.id})" style="flex:1;display:flex;align-items:center;justify-content:center;gap:6px;padding:8px 12px;border-radius:8px;border:2px solid #bdc8d2;background:#fff;color:#ba1a1a;font-family:Lexend,sans-serif;font-size:.75rem;font-weight:700;letter-spacing:.05em;text-transform:uppercase;cursor:pointer;transition:background .15s;" onmouseenter="this.style.background='#ffdad6'" onmouseleave="this.style.background='#fff'">
                    <span class="material-symbols-outlined" style="font-size:15px;">delete</span>Delete
                </button>
            </div>
        </div>
    `).join('');
}

function openAddModal() {
    document.getElementById('modalTitle').textContent = 'Thêm Từ Mới';
    document.getElementById('wordId').value = '';
    document.getElementById('wordForm').reset();
    document.getElementById('wordModal').classList.remove('hidden');
}

function openEditModal(id) {
    const word = allWords.find(w => w.id === id);
    if (!word) return;
    document.getElementById('modalTitle').textContent = 'Sửa Từ';
    document.getElementById('wordId').value = word.id;
    document.getElementById('wordInput').value = word.word;
    document.getElementById('translationInput').value = word.translation;
    document.getElementById('pronunciationInput').value = word.pronunciation || '';
    document.getElementById('categoryInput').value = word.category || '';
    document.getElementById('difficultyInput').value = word.difficulty;
    document.getElementById('exampleInput').value = word.example || '';
    document.getElementById('wordModal').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('wordModal').classList.add('hidden');
}

function openDeleteModal(id) {
    deleteWordId = id;
    document.getElementById('deleteModal').style.display = 'flex';
}

function closeDeleteModal() {
    document.getElementById('deleteModal').style.display = 'none';
    deleteWordId = null;
}

async function saveWord(event) {
    event.preventDefault();
    const id = document.getElementById('wordId').value;
    const body = {
        word: document.getElementById('wordInput').value.trim(),
        translation: document.getElementById('translationInput').value.trim(),
        pronunciation: document.getElementById('pronunciationInput').value.trim(),
        category: document.getElementById('categoryInput').value.trim(),
        difficulty: document.getElementById('difficultyInput').value,
        example: document.getElementById('exampleInput').value.trim(),
    };

    try {
        const url = id ? `/api/words/${id}` : '/api/words';
        const method = id ? 'PUT' : 'POST';
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
        });
        if (!res.ok) throw new Error('Failed to save');
        closeModal();
        showToast(id ? 'Đã cập nhật từ vựng!' : 'Đã thêm từ mới!');
        await loadWords();
    } catch (e) {
        showToast('Có lỗi xảy ra. Thử lại!', 'error');
    }
}

const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
if (confirmDeleteBtn) confirmDeleteBtn.addEventListener('click', async () => {
    if (!deleteWordId) return;
    try {
        const res = await fetch(`/api/words/${deleteWordId}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to delete');
        closeDeleteModal();
        showToast('Đã xóa từ vựng!', 'info');
        await loadWords();
    } catch (e) {
        showToast('Có lỗi xảy ra. Thử lại!', 'error');
    }
});

// Close modals on backdrop click
const wordModalEl = document.getElementById('wordModal');
if (wordModalEl) wordModalEl.addEventListener('click', e => {
    if (e.target === wordModalEl) closeModal();
});
const deleteModalEl = document.getElementById('deleteModal');
if (deleteModalEl) deleteModalEl.addEventListener('click', e => {
    if (e.target === deleteModalEl) closeDeleteModal();
});

// Initialize
loadWords();
