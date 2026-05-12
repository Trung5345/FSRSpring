function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

async function loadContent() {
    const topic = document.getElementById('topicInput').value.trim() || 'vocabulary';
    try {
        const data = await fetch(`/api/content/combined?topic=${encodeURIComponent(topic)}`).then(r => r.json());
        renderContent('youtubeList', data.youtube || []);
        renderContent('newsList', data.news || []);
    } catch (e) {
        console.error('Failed to load content', e);
    }
}

function renderContent(containerId, items) {
    const container = document.getElementById(containerId);
    if (!items.length) {
        container.innerHTML = '<p class="empty-inline">No data. Configure API keys and retry.</p>';
        return;
    }

    container.innerHTML = items.map(item => `
        <a class="content-card" href="${escapeHtml(item.url)}" target="_blank" rel="noopener noreferrer">
            <p class="content-title">${escapeHtml(item.title)}</p>
            <p class="content-summary">${escapeHtml(item.summary || '')}</p>
        </a>
    `).join('');
}

loadContent();
