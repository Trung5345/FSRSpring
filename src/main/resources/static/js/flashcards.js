function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

async function importFlashcards() {
    const source = document.getElementById('sourceSelect').value;
    const topic = document.getElementById('topicInput').value.trim() || 'general';

    try {
        await fetch(`/api/flashcards/import?source=${encodeURIComponent(source)}&topic=${encodeURIComponent(topic)}`, {
            method: 'POST'
        });
        await loadFlashcards();
    } catch (e) {
        console.error('Import failed', e);
    }
}

async function loadFlashcards() {
    const search = document.getElementById('searchInput').value.trim();
    const url = search ? `/api/flashcards?search=${encodeURIComponent(search)}` : '/api/flashcards';

    try {
        const cards = await fetch(url).then(r => r.json());
        const list = document.getElementById('flashcardList');
        if (!cards.length) {
            list.innerHTML = '<p class="text-sm text-slate-500">No flashcards available.</p>';
            return;
        }

        list.innerHTML = cards.map(card => `
            <article class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                <h3 class="text-xl font-semibold text-slate-800">${escapeHtml(card.word)}</h3>
                <p class="mt-1 text-slate-600">${escapeHtml(card.translation)}</p>
                <p class="mt-2 text-sm text-slate-500">${escapeHtml(card.example || '')}</p>
                <p class="mt-2 text-xs text-slate-400">${escapeHtml(card.sourceName)} | ${escapeHtml(card.topic || 'general')}</p>
                <button onclick="saveToVocabulary(${card.id})" class="mt-3 rounded-lg bg-indigo-700 px-3 py-2 text-sm font-semibold text-white">Save to Vocabulary</button>
            </article>
        `).join('');
    } catch (e) {
        console.error('Failed to load flashcards', e);
    }
}

async function saveToVocabulary(id) {
    try {
        await fetch(`/api/flashcards/${id}/save-to-vocab?difficulty=BEGINNER&category=Imported`, {
            method: 'POST'
        });
    } catch (e) {
        console.error('Failed to save flashcard', e);
    }
}

loadFlashcards();
