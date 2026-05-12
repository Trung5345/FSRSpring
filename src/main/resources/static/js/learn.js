// learn.js - Flashcard learning page

let cards = [];
let currentIndex = 0;
let correctCount = 0;
let incorrectCount = 0;
let isFlipped = false;
let learnMode = 'en-vi'; // 'en-vi' or 'vi-en'
let cardFlipTime = 0; // timestamp when card was flipped (for responseMs)

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

async function loadCategories() {
    try {
        const cats = await fetch('/api/words/categories').then(r => r.json());
        const sel = document.getElementById('lcategory');
        cats.forEach(cat => {
            sel.innerHTML += `<option value="${escapeHtml(cat)}">${escapeHtml(cat)}</option>`;
        });
    } catch (e) { console.error(e); }
}

async function startLearn() {
    const category = document.getElementById('lcategory').value;
    const difficulty = document.getElementById('ldifficulty').value;
    const cardCount = parseInt(document.getElementById('lcardCount').value);
    learnMode = document.getElementById('lmode').value;

    try {
        let url = '/api/words?';
        if (category) url += `category=${encodeURIComponent(category)}&`;
        if (difficulty) url += `difficulty=${encodeURIComponent(difficulty)}&`;
        let words = await fetch(url).then(r => r.json());

        if (!words || words.length === 0) {
            alert('Không có từ vựng nào phù hợp. Hãy thêm từ mới!');
            return;
        }

        // Shuffle and limit
        words = shuffleArray(words).slice(0, cardCount);
        cards = words;
        currentIndex = 0;
        correctCount = 0;
        incorrectCount = 0;

        document.getElementById('setupScreen').classList.add('hidden');
        document.getElementById('learnScreen').classList.remove('hidden');
        document.getElementById('resultScreen').classList.add('hidden');

        showCard(0);
    } catch (e) {
        console.error('Failed to start learn session', e);
    }
}

function shuffleArray(arr) {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
}

function showCard(index) {
    if (index >= cards.length) {
        showResult();
        return;
    }
    const card = cards[index];
    isFlipped = false;

    const flashcard = document.getElementById('flashcard');
    flashcard.classList.remove('flipped');

    // Set content based on mode
    if (learnMode === 'en-vi') {
        document.getElementById('cardFront').textContent = card.word;
        document.getElementById('cardPronunciation').textContent = card.pronunciation || '';
        document.getElementById('cardCategory').textContent = card.category || '';
        document.getElementById('cardBack').textContent = card.translation;
        document.getElementById('cardHint').textContent = 'Nhấn để xem nghĩa tiếng Việt';
    } else {
        document.getElementById('cardFront').textContent = card.translation;
        document.getElementById('cardPronunciation').textContent = '';
        document.getElementById('cardCategory').textContent = card.category || '';
        document.getElementById('cardBack').textContent = card.word;
        document.getElementById('cardHint').textContent = 'Nhấn để xem từ tiếng Anh';
    }

    document.getElementById('cardExample').textContent = card.example ? `"${card.example}"` : '';

    const diffMap = {
        BEGINNER: '<span class="badge-beginner">Sơ cấp</span>',
        INTERMEDIATE: '<span class="badge-intermediate">Trung cấp</span>',
        ADVANCED: '<span class="badge-advanced">Nâng cao</span>',
    };
    document.getElementById('difficultyBadge').innerHTML = diffMap[card.difficulty] || '';

    updateProgress();
    showFlipButtons();
}

function updateProgress() {
    const total = cards.length;
    const done = currentIndex;
    const pct = total > 0 ? (done / total) * 100 : 0;
    document.getElementById('cardProgress').textContent = `${done} / ${total}`;
    document.getElementById('progressBar').style.width = `${pct}%`;
    document.getElementById('correctCount').textContent = correctCount;
    document.getElementById('incorrectCount').textContent = incorrectCount;
}

function flipCard() {
    const flashcard = document.getElementById('flashcard');
    isFlipped = !isFlipped;
    flashcard.classList.toggle('flipped', isFlipped);
    if (isFlipped) {
        showRateButtons();
    } else {
        showFlipButtons();
    }
}

function showFlipButtons() {
    document.getElementById('flipButtons').classList.remove('hidden');
    document.getElementById('rateButtons').classList.add('hidden');
    document.getElementById('flipHint').classList.remove('hidden');
    document.getElementById('rateHint').classList.add('hidden');
}

function showRateButtons() {
    document.getElementById('flipButtons').classList.add('hidden');
    document.getElementById('rateButtons').classList.remove('hidden');
    document.getElementById('flipHint').classList.add('hidden');
    document.getElementById('rateHint').classList.remove('hidden');
    cardFlipTime = Date.now();
}

async function rateCard(rating) {
    const card = cards[currentIndex];
    const responseMs = cardFlipTime > 0 ? Date.now() - cardFlipTime : 0;
    try {
        await fetch(`/api/fsrs/review?wordId=${card.id}&rating=${rating}&responseMs=${responseMs}`, { method: 'POST' });
    } catch (e) { /* non-critical */ }

    if (rating >= 3) {
        correctCount++;
    } else {
        incorrectCount++;
    }
    currentIndex++;
    showCard(currentIndex);
}

function endSession() {
    if (currentIndex > 0) {
        showResult();
    } else {
        resetLearn();
    }
}

function showResult() {
    document.getElementById('learnScreen').classList.add('hidden');
    document.getElementById('resultScreen').classList.remove('hidden');

    const total = cards.length;
    const pct = total > 0 ? Math.round(correctCount / total * 100) : 0;

    document.getElementById('rTotal').textContent = total;
    document.getElementById('rCorrect').textContent = correctCount;
    document.getElementById('rIncorrect').textContent = incorrectCount;

    let msg;
    if (pct >= 90) { msg = 'Xuất sắc. Bạn đã ghi nhớ rất tốt.'; }
    else if (pct >= 70) { msg = 'Tốt lắm. Tiếp tục cố gắng nhé.'; }
    else if (pct >= 50) { msg = 'Ổn. Hãy ôn tập thêm những từ chưa nhớ.'; }
    else { msg = 'Cần ôn tập thêm. Đừng nản lòng.'; }

    document.getElementById('resultMessage').textContent = `${msg} (${pct}%)`;
}

function resetLearn() {
    cards = [];
    currentIndex = 0;
    correctCount = 0;
    incorrectCount = 0;
    document.getElementById('setupScreen').classList.remove('hidden');
    document.getElementById('learnScreen').classList.add('hidden');
    document.getElementById('resultScreen').classList.add('hidden');
}

// Keyboard shortcuts
document.addEventListener('keydown', e => {
    const learnScreen = document.getElementById('learnScreen');
    if (learnScreen.classList.contains('hidden')) return;
    if (e.code === 'Space' && !['INPUT','SELECT','BUTTON'].includes(e.target.tagName)) {
        e.preventDefault(); flipCard();
    }
    if (isFlipped) {
        if (e.key === '1') rateCard(1);
        if (e.key === '2') rateCard(2);
        if (e.key === '3') rateCard(3);
        if (e.key === '4') rateCard(4);
        if (e.code === 'ArrowRight') rateCard(3);  // Good
        if (e.code === 'ArrowLeft')  rateCard(1);  // Again
    }
});

// Initialize
loadCategories();
