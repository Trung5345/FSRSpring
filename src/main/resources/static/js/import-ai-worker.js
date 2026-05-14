const TRANSFORMERS_URL = 'https://cdn.jsdelivr.net/npm/@huggingface/transformers@4.1.0';

const state = {
    module: null,
    translator: null,
    extractor: null,
    transcriber: null,
    candidateEmbeddings: null,
    candidateKey: null
};

function reply(id, type, payload) {
    self.postMessage({ id, type, payload });
}

function progress(id, label, detail) {
    reply(id, 'progress', { label, detail });
}

async function loadModule() {
    if (state.module) return state.module;
    state.module = await import(TRANSFORMERS_URL);
    if (state.module.env) {
        state.module.env.allowRemoteModels = true;
        state.module.env.useBrowserCache = true;
        state.module.env.useWasmCache = true;
        if (state.module.LogLevel) {
            state.module.env.logLevel = state.module.LogLevel.WARNING;
        }
    }
    return state.module;
}

async function makePipeline(task, model, options, cacheKey) {
    const mod = await loadModule();
    if (state[cacheKey]) return state[cacheKey];
    try {
        const pipelineOptions = { dtype: 'q4', ...options };
        if (self.navigator && self.navigator.gpu) {
            pipelineOptions.device = 'webgpu';
        }
        state[cacheKey] = await mod.pipeline(task, model, pipelineOptions);
    } catch (error) {
        state[cacheKey] = await mod.pipeline(task, model, options || {});
    }
    return state[cacheKey];
}

function translationText(output) {
    const item = Array.isArray(output) ? output[0] : output;
    return item?.translation_text || item?.generated_text || item?.text || '';
}

async function translateRows(id, rows) {
    progress(id, 'translation', 'loading');
    const translator = await makePipeline('translation', 'Xenova/opus-mt-en-vi', {}, 'translator');
    const translated = [];
    for (const row of rows) {
        if (row.translation || !row.word) {
            translated.push(row);
            continue;
        }
        progress(id, 'translation', row.word);
        const output = await translator(`>>vie<< ${row.word}`);
        translated.push({ ...row, translation: translationText(output), aiTranslated: true });
    }
    return translated;
}

function dot(a, b) {
    let sum = 0;
    for (let i = 0; i < a.length; i++) sum += a[i] * b[i];
    return sum;
}

async function embedTexts(texts) {
    const extractor = await makePipeline('feature-extraction', 'onnx-community/all-MiniLM-L6-v2-ONNX', {}, 'extractor');
    const output = await extractor(texts, { pooling: 'mean', normalize: true });
    return output.tolist();
}

async function classifyRows(id, payload) {
    const rows = payload.rows || [];
    const candidates = payload.candidates || [];
    if (candidates.length === 0) return rows;
    const key = JSON.stringify(candidates.map(c => `${c.type}:${c.id || c.label}`));
    progress(id, 'classification', 'loading');
    if (!state.candidateEmbeddings || state.candidateKey !== key) {
        const candidateTexts = candidates.map(c => c.text || c.label);
        state.candidateEmbeddings = await embedTexts(candidateTexts);
        state.candidateKey = key;
    }
    const rowTexts = rows.map(row => [row.word, row.translation, row.example].filter(Boolean).join(' '));
    const rowEmbeddings = await embedTexts(rowTexts);
    return rows.map((row, rowIndex) => {
        const bestByType = new Map();
        state.candidateEmbeddings.forEach((candidateEmbedding, candidateIndex) => {
            const current = dot(rowEmbeddings[rowIndex], candidateEmbedding);
            const candidate = candidates[candidateIndex];
            const previous = bestByType.get(candidate.type);
            if (!previous || current > previous.score) {
                bestByType.set(candidate.type, { candidate, score: current });
            }
        });
        const next = { ...row };
        const bestTopic = bestByType.get('topic');
        const bestCategory = bestByType.get('category');
        const bestCefr = bestByType.get('cefr');
        if (bestTopic) {
            next.topicId = bestTopic.candidate.id;
            next.category = next.category || bestTopic.candidate.label;
            next.confidence = Number(bestTopic.score.toFixed(3));
        }
        if (bestCategory && !next.category) {
            next.category = bestCategory.candidate.label;
        }
        if (bestCefr && !next.cefrLevel) {
            next.cefrLevel = bestCefr.candidate.label;
        }
        return next;
    });
}

async function transcribeAudio(id, url) {
    progress(id, 'speech', 'loading');
    const transcriber = await makePipeline(
        'automatic-speech-recognition',
        'onnx-community/whisper-tiny.en',
        {},
        'transcriber'
    );
    progress(id, 'speech', 'transcribing');
    const output = await transcriber(url);
    return output?.text || '';
}

self.onmessage = async event => {
    const { id, type, payload } = event.data || {};
    try {
        if (type === 'translate') {
            reply(id, 'result', await translateRows(id, payload.rows || []));
        } else if (type === 'classify') {
            reply(id, 'result', await classifyRows(id, payload || {}));
        } else if (type === 'transcribe') {
            reply(id, 'result', await transcribeAudio(id, payload.url));
        } else if (type === 'warmup') {
            await loadModule();
            reply(id, 'result', { ready: true });
        }
    } catch (error) {
        reply(id, 'error', { message: error.message || 'AI worker failed' });
    }
};
