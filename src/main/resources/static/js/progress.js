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
        NEW: 'bg-gray-400',
        LEARNING: 'bg-yellow-400',
        REVIEWING: 'bg-blue-400',
        MASTERED: 'bg-green-500',
    };

    document.getElementById('masteryBars').innerHTML = Object.entries(counts).map(([key, count]) => {
        const pct = total > 0 ? Math.round(count / total * 100) : 0;
        return `
            <div>
                <div class="flex justify-between text-sm mb-1">
                    <span class="text-gray-600">${masteryLabel(key)}</span>
                    <span class="text-gray-500">${count} từ (${pct}%)</span>
                </div>
                <div class="bg-gray-100 rounded-full h-3">
                    <div class="${colors[key]} h-3 rounded-full transition-[width] duration-700" style="width: ${pct}%"></div>
                </div>
            </div>
        `;
    }).join('');
}

function renderSessions(sessions) {
    const container = document.getElementById('recentSessions');
    if (!sessions || sessions.length === 0) {
        container.innerHTML = `
            <div class="text-center py-8 text-gray-400">
                <p>Chưa có bài kiểm tra nào. <a href="/quiz" class="text-indigo-500 hover:underline">Làm bài ngay!</a></p>
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
        const scoreColor = score >= 80 ? 'text-green-600' : score >= 60 ? 'text-yellow-600' : 'text-red-500';
        return `
            <div class="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                <div class="flex items-center gap-3">
                    <div>
                        <p class="font-medium text-gray-700">
                            ${s.correctAnswers}/${s.totalQuestions} câu đúng
                            ${s.category ? `· <span class="text-gray-400">${escapeHtml(s.category)}</span>` : ''}
                        </p>
                        <p class="text-xs text-gray-400">${date}</p>
                    </div>
                </div>
                <span class="${scoreColor} text-xl font-bold">${score}%</span>
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
        const accColor = acc >= 80 ? 'text-green-600' : acc >= 60 ? 'text-yellow-600' : 'text-red-500';
        return `
            <tr class="hover:bg-gray-50">
                <td class="py-3 pr-4">
                    <span class="font-semibold text-gray-800">${escapeHtml(p.word.word)}</span>
                    ${p.word.pronunciation ? `<span class="text-indigo-400 text-xs ml-1">${escapeHtml(p.word.pronunciation)}</span>` : ''}
                </td>
                <td class="py-3 pr-4 text-gray-600">${escapeHtml(p.word.translation)}</td>
                <td class="py-3 pr-4">${masteryBadge(p.mastery)}</td>
                <td class="py-3 pr-4 text-green-600 font-medium">${p.correctCount}</td>
                <td class="py-3 pr-4 text-red-500 font-medium">${p.incorrectCount}</td>
                <td class="py-3 ${accColor} font-semibold">${acc}%</td>
            </tr>`;
    }).join('');
}

function filterProgress() {
    renderProgressTable(allProgress);
}

// Initialize
loadProgress();
