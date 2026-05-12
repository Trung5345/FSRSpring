// progress.js - Progress tracking page

let allProgress = [];

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function masteryLabel(mastery) {
    const labels = {
        NEW: 'Mới',
        LEARNING: 'Đang Học',
        REVIEWING: 'Ôn Tập',
        MASTERED: 'Đã Thuộc'
    };
    return labels[mastery] || mastery;
}

function masteryBadge(mastery) {
    return `<span class="badge-mastery-${mastery}">${masteryLabel(mastery)}</span>`;
}

async function loadProgress() {
    try {
        const [wordCount, progressStats, quizStats, progressList, sessions] = await Promise.all([
            fetch('/api/words/count').then(r => r.json()),
            fetch('/api/progress/stats').then(r => r.json()),
            fetch('/api/quiz/stats').then(r => r.json()),
            fetch('/api/progress').then(r => r.json()),
            fetch('/api/quiz/sessions/recent').then(r => r.json()),
        ]);

        // Stats cards
        document.getElementById('pTotalWords').textContent = wordCount.count ?? 0;
        document.getElementById('pMastered').textContent = progressStats.mastered ?? 0;
        document.getElementById('pLearning').textContent = progressStats.learning ?? 0;
        const acc = progressStats.accuracy ?? 0;
        document.getElementById('pAccuracy').textContent = acc.toFixed(1) + '%';
        document.getElementById('pCorrect').textContent = progressStats.totalCorrect ?? 0;
        document.getElementById('pIncorrect').textContent = progressStats.totalIncorrect ?? 0;

        // Quiz stats
        document.getElementById('qCompleted').textContent = quizStats.completedSessions ?? 0;
        const avg = quizStats.averageScore ?? 0;
        document.getElementById('qAvgScore').textContent = avg.toFixed(1) + '%';

        // Mastery distribution bars
        allProgress = progressList;
        renderMasteryBars(progressList, wordCount.count);

        // Recent sessions
        renderSessions(sessions);

        // Progress table
        renderProgressTable(progressList);

    } catch (e) {
        console.error('Failed to load progress', e);
    }
}

function renderMasteryBars(progressList, totalWords) {
    const counts = { NEW: 0, LEARNING: 0, REVIEWING: 0, MASTERED: 0 };
    progressList.forEach(p => { if (counts[p.mastery] !== undefined) counts[p.mastery]++; });
    // Words never studied = NEW
    const studied = progressList.length;
    const neverStudied = Math.max(0, totalWords - studied);
    counts.NEW += neverStudied;

    const total = Object.values(counts).reduce((a, b) => a + b, 0);
    const colors = {
        NEW: 'mastery-fill-new',
        LEARNING: 'mastery-fill-learning',
        REVIEWING: 'mastery-fill-reviewing',
        MASTERED: 'mastery-fill-mastered',
    };

    document.getElementById('masteryBars').innerHTML = Object.entries(counts).map(([key, count]) => {
        const pct = total > 0 ? Math.round(count / total * 100) : 0;
        return `
            <div class="mastery-row">
                <div class="mastery-row-head">
                    <span>${masteryLabel(key)}</span>
                    <span>${count} từ (${pct}%)</span>
                </div>
                <div class="mastery-bar-track">
                    <div class="mastery-bar-fill ${colors[key]}" style="width: ${pct}%"></div>
                </div>
            </div>
        `;
    }).join('');
}

function renderSessions(sessions) {
    const container = document.getElementById('recentSessions');
    if (!sessions || sessions.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <p class="empty-state-desc">Chưa có bài kiểm tra nào.</p>
                <a href="/quiz" class="review-more">Làm bài ngay!</a>
            </div>`;
        return;
    }

    container.innerHTML = sessions.map(s => {
        const score = s.totalQuestions > 0
            ? Math.round(s.correctAnswers / s.totalQuestions * 100)
            : 0;
        const date = s.completedAt
            ? new Date(s.completedAt).toLocaleString('vi-VN')
            : 'Chưa hoàn thành';
        const scoreColor = score >= 80 ? 'progress-score-high' : score >= 60 ? 'progress-score-mid' : 'progress-score-low';
        return `
            <div class="session-item">
                <div>
                    <p class="session-title">
                        ${s.correctAnswers}/${s.totalQuestions} câu đúng
                        ${s.category ? `· <span class="session-meta">${escapeHtml(s.category)}</span>` : ''}
                    </p>
                    <p class="session-meta">${date}</p>
                </div>
                <span class="${scoreColor}" style="font-size:1.2rem;font-weight:800;">${score}%</span>
            </div>`;
    }).join('');
}

function renderProgressTable(progressList) {
    const filter = document.getElementById('progressFilter').value;
    const filtered = filter ? progressList.filter(p => p.mastery === filter) : progressList;

    const tbody = document.getElementById('progressTable');
    const noData = document.getElementById('noProgressData');

    if (filtered.length === 0) {
        tbody.innerHTML = '';
        noData.classList.remove('hidden');
        return;
    }
    noData.classList.add('hidden');

    tbody.innerHTML = filtered.map(p => {
        const acc = p.totalAttempts > 0 ? Math.round(p.correctCount / p.totalAttempts * 100) : 0;
        const accColor = acc >= 80 ? 'progress-accuracy-high' : acc >= 60 ? 'progress-accuracy-mid' : 'progress-accuracy-low';
        return `
            <tr>
                <td>
                    <span class="progress-word">${escapeHtml(p.word.word)}</span>
                    ${p.word.pronunciation ? `<span class="progress-pronunciation">${escapeHtml(p.word.pronunciation)}</span>` : ''}
                </td>
                <td>${escapeHtml(p.word.translation)}</td>
                <td>${masteryBadge(p.mastery)}</td>
                <td class="text-success" style="font-weight:700;">${p.correctCount}</td>
                <td class="text-danger" style="font-weight:700;">${p.incorrectCount}</td>
                <td class="${accColor}" style="font-weight:800;">${acc}%</td>
            </tr>`;
    }).join('');
}

function filterProgress() {
    renderProgressTable(allProgress);
}

// Initialize
loadProgress();
