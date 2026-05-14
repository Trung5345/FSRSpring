(function () {
    const DRAFT_ID = 'current-import';
    const state = {
        source: 'paste',
        rows: [],
        topics: [],
        categories: [],
        file: null,
        fileName: '',
        worker: null,
        pending: new Map(),
        saveTimer: null
    };

    const els = {};

    window.onAppLoad(init);

    async function init() {
        cacheElements();
        bindEvents();
        initWorker();
        await loadReferenceData();
        await loadJobs();
        await restoreDraft(false);
        render();
        createIcons();
    }

    function cacheElements() {
        [
            'pasteInput', 'fileInput', 'fileNameLabel', 'dropZone', 'audioInput',
            'transcribeBtn', 'audioStatus', 'defaultDifficulty', 'defaultCefr',
            'defaultCategory', 'defaultTopic', 'targetSetName', 'parseBtn',
            'restoreDraftBtn', 'clearDraftBtn', 'translateBtn', 'classifyBtn',
            'dictionaryBtn', 'commitBtn', 'translationStatus', 'classificationStatus',
            'dictionaryStatus', 'rowCount', 'readyCount', 'missingCount',
            'emptyState', 'tableWrap', 'rowsBody', 'historyList', 'reloadJobsBtn',
            'toastZone'
        ].forEach(id => els[id] = document.getElementById(id));
    }

    function bindEvents() {
        document.querySelectorAll('[data-source-tab]').forEach(btn => {
            btn.addEventListener('click', () => switchSource(btn.dataset.sourceTab));
        });
        els.parseBtn.addEventListener('click', parseCurrentSource);
        els.restoreDraftBtn.addEventListener('click', () => restoreDraft(true));
        els.clearDraftBtn.addEventListener('click', clearDraft);
        els.translateBtn.addEventListener('click', translateMissing);
        els.classifyBtn.addEventListener('click', classifyRows);
        els.dictionaryBtn.addEventListener('click', enrichDictionary);
        els.commitBtn.addEventListener('click', commitRows);
        els.reloadJobsBtn.addEventListener('click', loadJobs);
        els.transcribeBtn.addEventListener('click', transcribeAudio);
        els.fileInput.addEventListener('change', event => {
            state.file = event.target.files[0] || null;
            state.fileName = state.file ? state.file.name : '';
            els.fileNameLabel.textContent = state.fileName || 'Choose file';
            scheduleDraftSave();
        });
        els.dropZone.addEventListener('dragover', event => {
            event.preventDefault();
            els.dropZone.classList.add('drag-over');
        });
        els.dropZone.addEventListener('dragleave', () => els.dropZone.classList.remove('drag-over'));
        els.dropZone.addEventListener('drop', event => {
            event.preventDefault();
            els.dropZone.classList.remove('drag-over');
            state.file = event.dataTransfer.files[0] || null;
            state.fileName = state.file ? state.file.name : '';
            els.fileNameLabel.textContent = state.fileName || 'Choose file';
            scheduleDraftSave();
        });
        ['input', 'change'].forEach(type => {
            ['pasteInput', 'defaultDifficulty', 'defaultCefr', 'defaultCategory', 'defaultTopic', 'targetSetName']
                .forEach(id => els[id].addEventListener(type, scheduleDraftSave));
        });
        els.rowsBody.addEventListener('input', handleRowEdit);
        els.rowsBody.addEventListener('change', handleRowEdit);
        els.rowsBody.addEventListener('click', event => {
            const button = event.target.closest('[data-action="remove"]');
            if (!button) return;
            state.rows = state.rows.filter(row => row.clientRowId !== button.dataset.id);
            render();
            scheduleDraftSave();
        });
    }

    function initWorker() {
        if (!window.Worker) {
            toast('AI worker is not available in this browser.', 'error');
            return;
        }
        state.worker = new Worker('/js/import-ai-worker.js', { type: 'module' });
        state.worker.onmessage = event => {
            const { id, type, payload } = event.data || {};
            const pending = state.pending.get(id);
            if (!pending) return;
            if (type === 'progress') {
                pending.onProgress?.(payload);
            } else if (type === 'result') {
                state.pending.delete(id);
                pending.resolve(payload);
            } else if (type === 'error') {
                state.pending.delete(id);
                pending.reject(new Error(payload?.message || 'AI worker failed'));
            }
        };
        askWorker('warmup', {}, payload => {
            setStatus('translationStatus', 'Ready', 'good');
            setStatus('classificationStatus', 'Ready', 'good');
        }).then(() => {
            setStatus('translationStatus', 'Ready', 'good');
            setStatus('classificationStatus', 'Ready', 'good');
        }).catch(() => {
            setStatus('translationStatus', 'Offline', 'warn');
            setStatus('classificationStatus', 'Offline', 'warn');
        });
    }

    function askWorker(type, payload, onProgress) {
        if (!state.worker) return Promise.reject(new Error('AI worker is unavailable'));
        const id = `ai-${Date.now()}-${Math.random().toString(16).slice(2)}`;
        return new Promise((resolve, reject) => {
            state.pending.set(id, { resolve, reject, onProgress });
            state.worker.postMessage({ id, type, payload });
        });
    }

    async function loadReferenceData() {
        try {
            const [topics, categories] = await Promise.all([
                fetch('/api/topics').then(okJson),
                fetch('/api/words/categories').then(okJson)
            ]);
            state.topics = Array.isArray(topics) ? topics : [];
            state.categories = Array.isArray(categories) ? categories : [];
            renderTopicOptions();
            renderCategoryOptions();
        } catch (error) {
            toast('Reference data could not be loaded.', 'error');
        }
    }

    function renderTopicOptions() {
        const current = els.defaultTopic.value;
        els.defaultTopic.innerHTML = '<option value="">Auto</option>';
        state.topics.forEach(topic => {
            const option = document.createElement('option');
            option.value = topic.id;
            option.textContent = topic.name;
            els.defaultTopic.appendChild(option);
        });
        els.defaultTopic.value = current || '';
    }

    function renderCategoryOptions() {
        const list = document.getElementById('categoryList');
        list.innerHTML = '';
        state.categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category;
            list.appendChild(option);
        });
    }

    function switchSource(source) {
        state.source = source;
        document.querySelectorAll('[data-source-tab]').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.sourceTab === source);
        });
        ['paste', 'file', 'audio'].forEach(name => {
            document.getElementById(`panel-${name}`).hidden = name !== source;
        });
        scheduleDraftSave();
    }

    function defaults() {
        return {
            difficulty: els.defaultDifficulty.value || 'INTERMEDIATE',
            cefrLevel: els.defaultCefr.value || '',
            category: els.defaultCategory.value.trim(),
            topicId: els.defaultTopic.value ? Number(els.defaultTopic.value) : null
        };
    }

    async function parseCurrentSource() {
        try {
            let rows = [];
            if (state.source === 'file') {
                if (!state.file) {
                    toast('Choose a file first.', 'error');
                    return;
                }
                rows = await ImportParser.parseFile(state.file, defaults());
            } else {
                rows = ImportParser.parseText(els.pasteInput.value, defaults());
            }
            state.rows = rows;
            render();
            await saveDraftNow();
            toast(`Parsed ${rows.length} rows.`, rows.length ? 'success' : 'info');
        } catch (error) {
            toast(error.message || 'Could not parse input.', 'error');
        }
    }

    async function translateMissing() {
        const missing = state.rows.filter(row => row.word && !row.translation);
        if (missing.length === 0) {
            toast('No missing translations.', 'info');
            return;
        }
        setBusy(els.translateBtn, true);
        setStatus('translationStatus', 'Loading', 'warn');
        try {
            const translated = await translateWithBackend();
            state.rows = translated;
            render();
            await saveDraftNow();
            await ImportStore.setMeta('translation-model', { status: 'backend', updatedAt: new Date().toISOString() });
            setStatus('translationStatus', 'Ready', 'good');
            toast('Missing translations filled.', 'success');
        } catch (error) {
            try {
                const translated = await askWorker('translate', { rows: state.rows }, progress => {
                    setStatus('translationStatus', progress.detail || 'Working', 'warn');
                });
                state.rows = translated;
                render();
                await saveDraftNow();
                setStatus('translationStatus', 'Fallback', 'warn');
                toast('Backend unavailable. Browser model filled missing translations.', 'info');
            } catch (fallbackError) {
                setStatus('translationStatus', 'Fallback', 'warn');
                toast('Translation unavailable. Save can still enrich after commit.', 'error');
            }
        } finally {
            setBusy(els.translateBtn, false);
        }
    }

    async function translateWithBackend() {
        const response = await fetch('/api/enrichment/translate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ rows: state.rows })
        });
        if (!response.ok) throw new Error('Backend translation failed');
        const payload = await response.json();
        return Array.isArray(payload.rows) ? payload.rows : state.rows;
    }

    async function classifyRows() {
        if (state.rows.length === 0) {
            toast('Parse rows first.', 'info');
            return;
        }
        const candidates = buildClassificationCandidates();
        if (candidates.length === 0) {
            toast('No topics or categories available.', 'error');
            return;
        }
        setBusy(els.classifyBtn, true);
        setStatus('classificationStatus', 'Loading', 'warn');
        try {
            const classified = await askWorker('classify', { rows: state.rows, candidates }, progress => {
                setStatus('classificationStatus', progress.detail || 'Working', 'warn');
            });
            state.rows = classified;
            render();
            await saveDraftNow();
            await ImportStore.setMeta('classification-model', { status: 'ready', updatedAt: new Date().toISOString() });
            setStatus('classificationStatus', 'Ready', 'good');
            toast('Rows classified.', 'success');
        } catch (error) {
            setStatus('classificationStatus', 'Fallback', 'warn');
            fallbackClassify();
            render();
            await saveDraftNow();
            toast('Classifier unavailable. Applied simple topic matching.', 'info');
        } finally {
            setBusy(els.classifyBtn, false);
        }
    }

    function buildClassificationCandidates() {
        const cefrTexts = {
            A1: 'basic simple everyday beginner words objects greetings numbers family',
            A2: 'elementary common routine daily travel food simple descriptions',
            B1: 'intermediate work school plans feelings opinions stories',
            B2: 'upper intermediate business abstract technology news explanations',
            C1: 'advanced academic professional nuanced formal specialized vocabulary',
            C2: 'proficient expert idiomatic literary precise rare vocabulary'
        };
        const topicCandidates = state.topics.map(topic => ({
            type: 'topic',
            id: topic.id,
            label: topic.name,
            text: [topic.name, topic.description].filter(Boolean).join(' ')
        }));
        const categoryCandidates = state.categories.map(category => ({
            type: 'category',
            label: category,
            text: category
        }));
        const cefrCandidates = Object.entries(cefrTexts).map(([label, text]) => ({
            type: 'cefr',
            label,
            text
        }));
        return [...topicCandidates, ...categoryCandidates, ...cefrCandidates];
    }

    function fallbackClassify() {
        state.rows = state.rows.map(row => {
            const text = `${row.word} ${row.example}`.toLowerCase();
            const topic = state.topics.find(t => text.includes(String(t.name).toLowerCase()));
            return {
                ...row,
                topicId: row.topicId || topic?.id || null,
                category: row.category || topic?.name || els.defaultCategory.value.trim(),
                cefrLevel: row.cefrLevel || els.defaultCefr.value || ''
            };
        });
    }

    async function enrichDictionary() {
        const targets = state.rows.filter(row => row.word && (!row.pronunciation || !row.audioUrl || !row.partOfSpeech || !row.example));
        if (targets.length === 0) {
            toast('Dictionary fields are already filled.', 'info');
            return;
        }
        setBusy(els.dictionaryBtn, true);
        setStatus('dictionaryStatus', 'Working', 'warn');
        let count = 0;
        for (const row of targets) {
            try {
                const data = await fetch(`/api/dictionary/lookup/${encodeURIComponent(row.word)}`).then(okJson);
                row.pronunciation = row.pronunciation || data.bestPhonetic || data.phonetic || '';
                row.audioUrl = normalizeAudio(row.audioUrl || data.bestAudioUrl || '');
                row.partOfSpeech = row.partOfSpeech || data.primaryPartOfSpeech || firstPartOfSpeech(data) || '';
                row.example = row.example || data.firstExample || firstExample(data) || '';
                count++;
            } catch (error) {
                row.message = row.message || 'Dictionary not found';
            }
        }
        setStatus('dictionaryStatus', count ? 'Ready' : 'No hits', count ? 'good' : 'warn');
        render();
        await saveDraftNow();
        setBusy(els.dictionaryBtn, false);
        toast(`Dictionary enriched ${count} rows.`, count ? 'success' : 'info');
    }

    async function transcribeAudio() {
        const file = els.audioInput.files[0];
        if (!file) {
            toast('Choose an audio file first.', 'error');
            return;
        }
        setBusy(els.transcribeBtn, true);
        els.audioStatus.textContent = 'Transcribing';
        els.audioStatus.className = 'pill warn';
        const url = URL.createObjectURL(file);
        try {
            const text = await askWorker('transcribe', { url }, progress => {
                els.audioStatus.textContent = progress.detail || 'Working';
            });
            els.pasteInput.value = text;
            switchSource('paste');
            state.rows = ImportParser.parseText(text, defaults());
            render();
            await saveDraftNow();
            els.audioStatus.textContent = 'Done';
            els.audioStatus.className = 'pill good';
            toast('Audio transcribed.', 'success');
        } catch (error) {
            els.audioStatus.textContent = 'Failed';
            els.audioStatus.className = 'pill bad';
            toast('Speech model unavailable.', 'error');
        } finally {
            URL.revokeObjectURL(url);
            setBusy(els.transcribeBtn, false);
        }
    }

    async function commitRows() {
        if (state.rows.length === 0) {
            toast('Parse rows first.', 'info');
            return;
        }
        const payload = buildCommitPayload();
        setBusy(els.commitBtn, true);
        try {
            const response = await fetch('/api/import/words/commit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new Error(error.error || 'Import failed');
            }
            const result = await response.json();
            applyCommitResult(result);
            render();
            await saveDraftNow();
            await loadJobs();
            toast(`Saved ${result.created}, skipped ${result.skipped}, failed ${result.failed}.`, result.failed ? 'error' : 'success');
        } catch (error) {
            await ImportStore.enqueueRetry({ payload, error: error.message, createdAt: new Date().toISOString() });
            toast(error.message || 'Import failed. Added to retry queue.', 'error');
        } finally {
            setBusy(els.commitBtn, false);
        }
    }

    function buildCommitPayload() {
        const targetSetName = els.targetSetName.value.trim();
        return {
            sourceType: state.source.toUpperCase(),
            fileName: state.fileName || null,
            targetSet: targetSetName ? {
                name: targetSetName,
                topicId: els.defaultTopic.value ? Number(els.defaultTopic.value) : null,
                cefrLevel: els.defaultCefr.value || null
            } : null,
            options: { duplicatePolicy: 'SKIP', translationDirection: 'EN_TO_VI', autoEnrich: true },
            rows: state.rows.map(row => ({
                clientRowId: row.clientRowId,
                word: row.word,
                translation: row.translation,
                example: row.example,
                pronunciation: row.pronunciation,
                category: row.category,
                difficulty: row.difficulty || 'BEGINNER',
                topicId: row.topicId || null,
                cefrLevel: row.cefrLevel || null,
                partOfSpeech: row.partOfSpeech,
                audioUrl: row.audioUrl
            }))
        };
    }

    function applyCommitResult(result) {
        const byId = new Map((result.rows || []).map(row => [row.clientRowId, row]));
        state.rows = state.rows.map(row => {
            const outcome = byId.get(row.clientRowId);
            return outcome ? {
                ...row,
                status: outcome.status,
                wordId: outcome.wordId,
                enrichmentStatus: outcome.enrichmentStatus,
                message: outcome.message
            } : row;
        });
    }

    function render() {
        renderRows();
        renderSummary();
        createIcons();
    }

    function renderRows() {
        els.emptyState.hidden = state.rows.length > 0;
        els.tableWrap.hidden = state.rows.length === 0;
        els.rowsBody.innerHTML = state.rows.map((row, index) => `
            <tr>
                <td>
                    <span class="pill ${statusClass(row.status)}">${index + 1}</span>
                    ${row.enrichmentStatus ? `<br><small>${escapeHtml(row.enrichmentStatus)}</small>` : ''}
                    ${row.confidence ? `<br><small>${escapeHtml(row.confidence)}</small>` : ''}
                </td>
                <td><input data-id="${row.clientRowId}" data-field="word" value="${escapeAttr(row.word)}"></td>
                <td><input data-id="${row.clientRowId}" data-field="translation" value="${escapeAttr(row.translation)}"></td>
                <td>${topicSelect(row)}</td>
                <td>${cefrSelect(row)}</td>
                <td>${difficultySelect(row)}</td>
                <td><input data-id="${row.clientRowId}" data-field="pronunciation" value="${escapeAttr(row.pronunciation)}"></td>
                <td><input data-id="${row.clientRowId}" data-field="partOfSpeech" value="${escapeAttr(row.partOfSpeech)}"></td>
                <td><input data-id="${row.clientRowId}" data-field="example" value="${escapeAttr(row.example)}" title="${escapeAttr(row.message)}"></td>
                <td><button class="row-action" data-action="remove" data-id="${row.clientRowId}" type="button" aria-label="Remove row"><i data-lucide="x"></i></button></td>
            </tr>
        `).join('');
    }

    function topicSelect(row) {
        const options = ['<option value="">Auto</option>'].concat(state.topics.map(topic => {
            const selected = String(row.topicId || '') === String(topic.id) ? 'selected' : '';
            return `<option value="${topic.id}" ${selected}>${escapeHtml(topic.name)}</option>`;
        }));
        return `<select data-id="${row.clientRowId}" data-field="topicId">${options.join('')}</select>`;
    }

    function cefrSelect(row) {
        const levels = ['', 'A1', 'A2', 'B1', 'B2', 'C1', 'C2'];
        return `<select data-id="${row.clientRowId}" data-field="cefrLevel">${levels.map(level => {
            const label = level || 'Auto';
            const selected = (row.cefrLevel || '') === level ? 'selected' : '';
            return `<option value="${level}" ${selected}>${label}</option>`;
        }).join('')}</select>`;
    }

    function difficultySelect(row) {
        const values = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];
        return `<select data-id="${row.clientRowId}" data-field="difficulty">${values.map(value => {
            const selected = (row.difficulty || 'BEGINNER') === value ? 'selected' : '';
            return `<option value="${value}" ${selected}>${value}</option>`;
        }).join('')}</select>`;
    }

    function renderSummary() {
        const ready = state.rows.filter(row => row.word).length;
        const missing = state.rows.filter(row => row.word && !row.translation).length;
        els.rowCount.textContent = `${state.rows.length} rows`;
        els.readyCount.textContent = `${ready} saveable`;
        els.missingCount.textContent = `${missing} translations`;
        els.missingCount.className = `pill ${missing ? 'warn' : 'good'}`;
    }

    function handleRowEdit(event) {
        const field = event.target.dataset.field;
        const id = event.target.dataset.id;
        if (!field || !id) return;
        const row = state.rows.find(item => item.clientRowId === id);
        if (!row) return;
        row[field] = field === 'topicId'
            ? (event.target.value ? Number(event.target.value) : null)
            : event.target.value;
        row.status = 'draft';
        renderSummary();
        scheduleDraftSave();
    }

    async function loadJobs() {
        try {
            const jobs = await fetch('/api/import/jobs').then(okJson);
            els.historyList.innerHTML = (jobs || []).map(job => `
                <div class="history-item">
                    <div>
                        <strong>${escapeHtml(job.fileName || job.sourceType)}</strong><br>
                        <span>${formatDate(job.createdAt)} · ${escapeHtml(job.status)}</span>
                    </div>
                    <span class="pill">${job.created}/${job.totalRows}</span>
                </div>
            `).join('') || '<p class="page-subtitle">No import jobs yet.</p>';
        } catch (error) {
            els.historyList.innerHTML = '<p class="page-subtitle">Import history unavailable.</p>';
        }
    }

    function scheduleDraftSave() {
        clearTimeout(state.saveTimer);
        state.saveTimer = setTimeout(saveDraftNow, 250);
    }

    async function saveDraftNow() {
        if (!window.ImportStore) return;
        await ImportStore.saveDraft(DRAFT_ID, {
            source: state.source,
            rows: state.rows,
            fileName: state.fileName,
            pasteInput: els.pasteInput.value,
            defaultDifficulty: els.defaultDifficulty.value,
            defaultCefr: els.defaultCefr.value,
            defaultCategory: els.defaultCategory.value,
            defaultTopic: els.defaultTopic.value,
            targetSetName: els.targetSetName.value
        });
    }

    async function restoreDraft(showToast) {
        if (!window.ImportStore) return;
        const draft = await ImportStore.loadDraft(DRAFT_ID);
        if (!draft) {
            if (showToast) toast('No draft found.', 'info');
            return;
        }
        state.rows = draft.rows || [];
        state.fileName = draft.fileName || '';
        els.fileNameLabel.textContent = state.fileName || 'Choose file';
        els.pasteInput.value = draft.pasteInput || '';
        els.defaultDifficulty.value = draft.defaultDifficulty || 'INTERMEDIATE';
        els.defaultCefr.value = draft.defaultCefr || '';
        els.defaultCategory.value = draft.defaultCategory || '';
        els.defaultTopic.value = draft.defaultTopic || '';
        els.targetSetName.value = draft.targetSetName || '';
        switchSource(draft.source || 'paste');
        render();
        if (showToast) toast('Draft restored.', 'success');
    }

    async function clearDraft() {
        state.rows = [];
        state.file = null;
        state.fileName = '';
        els.fileInput.value = '';
        els.audioInput.value = '';
        els.fileNameLabel.textContent = 'Choose file';
        els.pasteInput.value = '';
        els.targetSetName.value = '';
        await ImportStore.clearDraft(DRAFT_ID);
        render();
        toast('Draft cleared.', 'info');
    }

    function setBusy(button, busy) {
        button.disabled = busy;
        button.dataset.originalText = button.dataset.originalText || button.innerHTML;
        if (busy) button.innerHTML = '<i data-lucide="loader-circle"></i>Working';
        else button.innerHTML = button.dataset.originalText;
        createIcons();
    }

    function setStatus(id, text, tone) {
        const el = els[id];
        el.textContent = text;
        el.className = `pill ${tone || ''}`.trim();
    }

    function firstPartOfSpeech(data) {
        return data.meanings?.find(m => m.partOfSpeech)?.partOfSpeech || '';
    }

    function firstExample(data) {
        for (const meaning of data.meanings || []) {
            for (const def of meaning.definitions || []) {
                if (def.example) return def.example;
            }
        }
        return '';
    }

    function normalizeAudio(url) {
        if (!url) return '';
        return url.startsWith('//') ? `https:${url}` : url;
    }

    async function okJson(response) {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    }

    function toast(message, type) {
        const node = document.createElement('div');
        node.className = `toast ${type || 'info'}`;
        const icon = type === 'success' ? 'check-circle' : (type === 'error' ? 'circle-alert' : 'info');
        node.innerHTML = `<i data-lucide="${icon}"></i><span>${escapeHtml(message)}</span>`;
        els.toastZone.appendChild(node);
        createIcons();
        setTimeout(() => node.remove(), 3600);
    }

    function statusClass(status) {
        if (status === 'CREATED') return 'good';
        if (status === 'SKIPPED') return 'warn';
        if (status === 'FAILED') return 'bad';
        return '';
    }

    function formatDate(value) {
        if (!value) return '';
        return new Date(value).toLocaleString();
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function escapeAttr(value) {
        return escapeHtml(value);
    }

    function createIcons() {
        if (window.lucide && typeof window.lucide.createIcons === 'function') {
            window.lucide.createIcons();
        }
    }
})();
