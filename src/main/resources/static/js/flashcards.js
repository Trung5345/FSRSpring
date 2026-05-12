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
            list.innerHTML = `<div class="empty-state">
  <div class="empty-state-icon">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/></svg>
  </div>
  <div class="empty-state-title">No flashcard sets yet</div>
  <div class="empty-state-desc">Import a curated set from a trusted source to get started quickly.</div>
</div>`;
            return;
        }

        list.innerHTML = cards.map(card => `
            <article class="flashcard-source-card">
                <h3 class="flashcard-source-word">${escapeHtml(card.word)}</h3>
                <p class="flashcard-source-translation">${escapeHtml(card.translation)}</p>
                <p class="flashcard-source-example">${escapeHtml(card.example || '')}</p>
                <p class="flashcard-source-meta">${escapeHtml(card.sourceName)} | ${escapeHtml(card.topic || 'general')}</p>
                <button onclick="saveToVocabulary(${card.id})" class="btn btn-primary flashcard-save-btn">Save to Vocabulary</button>
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
