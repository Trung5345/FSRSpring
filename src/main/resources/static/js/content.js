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
        container.innerHTML = '<p class="text-sm text-slate-500">No data. Configure API keys and retry.</p>';
        return;
    }

    container.innerHTML = items.map(item => `
        <a class="block rounded-xl border border-slate-200 bg-slate-50 p-4 hover:bg-white transition" href="${escapeHtml(item.url)}" target="_blank" rel="noopener noreferrer">
            <p class="font-semibold text-slate-800">${escapeHtml(item.title)}</p>
            <p class="mt-1 text-xs text-slate-500">${escapeHtml(item.summary || '')}</p>
        </a>
    `).join('');
}

loadContent();
