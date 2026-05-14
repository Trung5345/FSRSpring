// quiz.js - Quiz/test page

function showToast(message, type = 'error') {
    const existing = document.getElementById('quizToast');
    if (existing) existing.remove();

    const colors = {
        error: { bg: '#ffdad6', border: '#ba1a1a', text: '#93000a', icon: 'error' },
        warn:  { bg: '#ffdf92', border: '#6e5400', text: '#6e5400', icon: 'warning' },
        info:  { bg: '#c8e6ff', border: '#006590', text: '#00405d', icon: 'info' },
    };
    const c = colors[type] || colors.error;

    const toast = document.createElement('div');
    toast.id = 'quizToast';
    toast.style.cssText = `position:fixed;top:88px;right:2rem;z-index:9999;
        background:${c.bg};border:1.5px solid ${c.border};color:${c.text};
        padding:14px 20px;border-radius:12px;display:flex;align-items:center;gap:10px;
        font-family:'Nunito Sans',sans-serif;font-size:15px;font-weight:600;
        box-shadow:0 4px 16px rgba(0,0,0,.12);max-width:420px;`;
    toast.innerHTML = `<span class="material-symbols-outlined" style="font-size:20px;flex-shrink:0">${c.icon}</span>
        <span>${message}</span>
        <button onclick="this.parentElement.remove()" style="margin-left:8px;background:none;border:none;cursor:pointer;color:${c.text};font-size:18px;line-height:1">&#x2715;</button>`;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 5000);
}

let sessionId = null;
let questions = [];
let currentQ = 0;
let correctCount = 0;
let incorrectCount = 0;
let allWords = [];
let quizType = 'en-vi';
let questionStartTime = 0;

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
        const sel = document.getElementById('qcategory');
        cats.forEach(cat => {
            sel.innerHTML += `<option value="${escapeHtml(cat)}">${escapeHtml(cat)}</option>`;
        });
    } catch (e) { console.error(e); }
}

async function startQuiz() {
    const category = document.getElementById('qcategory').value;
    const difficulty = document.getElementById('qdifficulty').value;
    const count = parseInt(document.getElementById('qcount').value);
    quizType = document.getElementById('qtype').value;

    try {
        // Fetch all words to build distractors
        allWords = await fetch('/api/words').then(r => r.json());
        if (allWords.length < 4) {
            showToast('Cần ít nhất 4 từ vựng để bắt đầu kiểm tra!', 'warn');
            return;
        }

        // Start quiz session on backend
        let url = `/api/quiz/start?count=${count}`;
        if (category) url += `&category=${encodeURIComponent(category)}`;
        if (difficulty) url += `&difficulty=${encodeURIComponent(difficulty)}`;
        const data = await fetch(url, { method: 'POST' }).then(r => r.json());

        sessionId = data.sessionId;
        const words = data.words;

        if (!words || words.length === 0) {
            showToast('Không có từ vựng phù hợp với bộ lọc đã chọn!', 'warn');
            return;
        }

        // Build questions with options
        questions = words.map(word => {
            const correct = quizType === 'en-vi' ? word.translation : word.word;
            const questionText = quizType === 'en-vi' ? word.word : word.translation;

            // Get 3 random distractors from allWords
            const pool = allWords.filter(w => w.id !== word.id);
            const shuffled = shuffleArray(pool).slice(0, 3);
            const distractors = shuffled.map(w => quizType === 'en-vi' ? w.translation : w.word);

            const options = shuffleArray([correct, ...distractors]);
            return { word, questionText, correct, options };
        });

        currentQ = 0;
        correctCount = 0;
        incorrectCount = 0;

        document.getElementById('setupScreen').classList.add('hidden');
        document.getElementById('quizScreen').classList.remove('hidden');
        document.getElementById('resultScreen').classList.add('hidden');

        showQuestion();
    } catch (e) {
        console.error('Failed to start quiz', e);
        showToast('Có lỗi xảy ra. Vui lòng thử lại!', 'error');
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

function showQuestion() {
    if (currentQ >= questions.length) {
        finishQuiz();
        return;
    }
    const q = questions[currentQ];
    const total = questions.length;
    const pct = (currentQ / total) * 100;

    document.getElementById('quizProgress').textContent = `Câu ${currentQ + 1} / ${total}`;
    document.getElementById('quizProgressBar').style.width = `${pct}%`;
    document.getElementById('qCorrect').textContent = correctCount;
    document.getElementById('qIncorrect').textContent = incorrectCount;

    document.getElementById('questionWord').textContent = q.questionText;
    document.getElementById('questionPronunciation').textContent =
        quizType === 'en-vi' ? (q.word.pronunciation || '') : '';
    document.getElementById('questionCategory').textContent = q.word.category || '';

    document.getElementById('quizInstruction').textContent =
        quizType === 'en-vi' ? 'Chọn nghĩa đúng của từ sau:' : 'Chọn từ tiếng Anh đúng với nghĩa sau:';

    // Render options
    const optGrid = document.getElementById('optionsGrid');
    optGrid.innerHTML = q.options.map((opt, i) => `
        <button class="quiz-option" onclick="selectOption(this, ${i})" data-option="${escapeHtml(opt)}">
            <span class="quiz-option-letter">${String.fromCharCode(65 + i)}.</span>
            ${escapeHtml(opt)}
        </button>
    `).join('');

    // Hide feedback and next button
    document.getElementById('feedback').classList.add('hidden');
    const nb = document.getElementById('nextBtn');
    nb.classList.add('hidden');
    nb.style.display = 'none';
    questionStartTime = Date.now();
}

function selectOption(btn, index) {
    const q = questions[currentQ];
    const chosen = btn.getAttribute('data-option');
    const isCorrect = chosen === q.correct;

    // Disable all options
    document.querySelectorAll('.quiz-option').forEach(b => {
        b.disabled = true;
        if (b.getAttribute('data-option') === q.correct) {
            b.classList.add('correct');
        }
    });

    if (!isCorrect) {
        btn.classList.add('incorrect');
    }

    // Update counts
    if (isCorrect) {
        correctCount++;
    } else {
        incorrectCount++;
    }

    // Update counters display
    document.getElementById('qCorrect').textContent = correctCount;
    document.getElementById('qIncorrect').textContent = incorrectCount;

    // Show feedback
    const feedback = document.getElementById('feedback');
    feedback.classList.remove('hidden', 'correct-fb', 'incorrect-fb');
    if (isCorrect) {
        feedback.classList.add('correct-fb');
        document.getElementById('feedbackText').textContent = 'Chính xác';
        document.getElementById('feedbackSubtext').textContent = q.word.example ? `"${q.word.example}"` : '';
    } else {
        feedback.classList.add('incorrect-fb');
        document.getElementById('feedbackText').textContent = 'Chưa đúng';
        document.getElementById('feedbackSubtext').textContent =
            `Đáp án đúng: ${q.correct}`;
    }

    // Record on backend
    if (sessionId) {
        fetch(`/api/quiz/session/${sessionId}/answer?wordId=${q.word.id}&correct=${isCorrect}`, { method: 'POST' })
            .catch(e => console.error(e));
    }
    fetch(`/api/fsrs/review?wordId=${q.word.id}&rating=${isCorrect ? 3 : 1}&responseMs=${Date.now() - questionStartTime}`, { method: 'POST' })
        .catch(e => console.error(e));

    // Auto-advance after 2s with countdown bar; also allow manual skip
    const nextBtn = document.getElementById('nextBtn');
    nextBtn.classList.remove('hidden');
    nextBtn.style.display = '';
    if (currentQ + 1 >= questions.length) {
        nextBtn.textContent = 'Xem Kết Quả';
    } else {
        nextBtn.textContent = 'Câu Tiếp Theo';
    }

    // Animate countdown bar
    const bar = document.getElementById('countdownBar');
    const barColor = isCorrect ? '#006590' : '#ba1a1a';
    bar.style.transition = 'none';
    bar.style.background = barColor;
    bar.style.width = '100%';
    // Force reflow then animate
    bar.getBoundingClientRect();
    bar.style.transition = 'width 2s linear';
    bar.style.width = '0%';

    let autoTimer = setTimeout(() => nextQuestion(), 2000);

    // If user clicks manually, cancel auto timer
    nextBtn.onclick = () => {
        clearTimeout(autoTimer);
        nextQuestion();
    };
}

function nextQuestion() {
    currentQ++;
    showQuestion();
}

async function finishQuiz() {
    if (sessionId) {
        try {
            await fetch(`/api/quiz/session/${sessionId}/complete`, { method: 'POST' });
        } catch (e) { console.error(e); }
    }

    document.getElementById('quizScreen').classList.add('hidden');
    document.getElementById('resultScreen').classList.remove('hidden');

    const total = questions.length;
    const pct = total > 0 ? Math.round(correctCount / total * 100) : 0;

    document.getElementById('rTotal').textContent = total;
    document.getElementById('rCorrect').textContent = correctCount;
    document.getElementById('rIncorrect').textContent = incorrectCount;
    document.getElementById('scorePercent').textContent = `${pct}%`;

    // Score circle color
    const circle = document.getElementById('scoreCircle');
    circle.classList.remove('score-good', 'score-ok', 'score-low');
    let msg;
    if (pct >= 80) {
        circle.classList.add('score-good');
        msg = 'Xuất sắc. Bạn nắm vững từ vựng rất tốt.';
    } else if (pct >= 60) {
        circle.classList.add('score-ok');
        msg = 'Khá tốt. Tiếp tục ôn luyện nhé.';
    } else {
        circle.classList.add('score-low');
        msg = 'Cần ôn tập thêm. Hãy xem lại flashcard.';
    }
    document.getElementById('resultMessage').textContent = msg;
}

function resetQuiz() {
    sessionId = null;
    questions = [];
    currentQ = 0;
    correctCount = 0;
    incorrectCount = 0;
    document.getElementById('setupScreen').classList.remove('hidden');
    document.getElementById('quizScreen').classList.add('hidden');
    document.getElementById('resultScreen').classList.add('hidden');
}

window.onAppLoad(() => {
    if (document.getElementById('setupScreen')) {
        loadCategories();
    }
});
