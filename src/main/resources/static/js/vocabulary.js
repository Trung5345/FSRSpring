// vocabulary.js - Vocabulary management page

let allWords = [];
let categories = [];
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
        BEGINNER: '<span class="badge-beginner">Sơ cấp</span>',
        INTERMEDIATE: '<span class="badge-intermediate">Trung cấp</span>',
        ADVANCED: '<span class="badge-advanced">Nâng cao</span>',
    };
    return map[d] || d;
}

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 400);
    }, 2500);
}

async function loadWords() {
    try {
        allWords = await fetch('/api/words').then(r => r.json());
        categories = await fetch('/api/words/categories').then(r => r.json());

        // Populate category filter
        const catFilter = document.getElementById('categoryFilter');
        const catList = document.getElementById('categoryList');
        catFilter.innerHTML = '<option value="">Tất Cả Danh Mục</option>';
        catList.innerHTML = '';
        categories.forEach(cat => {
            catFilter.innerHTML += `<option value="${escapeHtml(cat)}">${escapeHtml(cat)}</option>`;
            catList.innerHTML += `<option value="${escapeHtml(cat)}">`;
        });

        // Apply URL params (e.g. ?category=Animals)
        const params = new URLSearchParams(window.location.search);
        if (params.get('category')) {
            document.getElementById('categoryFilter').value = params.get('category');
        }

        renderWords(allWords);
    } catch (e) {
        console.error('Failed to load words', e);
    }
}

function filterWords() {
    const search = document.getElementById('searchInput').value.toLowerCase();
    const category = document.getElementById('categoryFilter').value;
    const difficulty = document.getElementById('difficultyFilter').value;

    const filtered = allWords.filter(w => {
        const matchSearch = !search ||
            w.word.toLowerCase().includes(search) ||
            w.translation.toLowerCase().includes(search);
        const matchCat = !category || w.category === category;
        const matchDiff = !difficulty || w.difficulty === difficulty;
        return matchSearch && matchCat && matchDiff;
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
        <div class="word-card bg-white rounded-2xl shadow-sm p-5 border border-gray-100">
            <div class="flex justify-between items-start mb-3">
                <div class="flex-1 min-w-0">
                    <h3 class="text-xl font-bold text-gray-800 truncate">${escapeHtml(w.word)}</h3>
                    ${w.pronunciation ? `<p class="text-indigo-400 text-sm">${escapeHtml(w.pronunciation)}</p>` : ''}
                </div>
                ${difficultyBadge(w.difficulty)}
            </div>
            <p class="text-gray-600 font-medium mb-2">${escapeHtml(w.translation)}</p>
            ${w.example ? `<p class="text-gray-400 text-sm italic mb-3 line-clamp-2">"${escapeHtml(w.example)}"</p>` : '<div class="mb-3"></div>'}
            ${w.category ? `<span class="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded-full">${escapeHtml(w.category)}</span>` : ''}
            <div class="flex gap-2 mt-4 pt-3 border-t border-gray-100">
                <button onclick="openEditModal(${w.id})"
                    class="flex-1 text-sm text-indigo-600 hover:bg-indigo-50 py-1.5 rounded-lg transition flex items-center justify-center gap-1">
                    Sửa
                </button>
                <button onclick="openDeleteModal(${w.id})"
                    class="flex-1 text-sm text-red-500 hover:bg-red-50 py-1.5 rounded-lg transition flex items-center justify-center gap-1">
                    Xóa
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
    document.getElementById('deleteModal').classList.remove('hidden');
}

function closeDeleteModal() {
    document.getElementById('deleteModal').classList.add('hidden');
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

document.getElementById('confirmDeleteBtn').addEventListener('click', async () => {
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
document.getElementById('wordModal').addEventListener('click', e => {
    if (e.target === document.getElementById('wordModal')) closeModal();
});
document.getElementById('deleteModal').addEventListener('click', e => {
    if (e.target === document.getElementById('deleteModal')) closeDeleteModal();
});

// Initialize
loadWords();
