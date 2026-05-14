(function () {
    const HEADER_ALIASES = {
        word: ['word', 'term', 'english', 'en', 'front'],
        translation: ['translation', 'meaning', 'vietnamese', 'vi', 'back', 'definition'],
        example: ['example', 'sentence', 'sample'],
        pronunciation: ['pronunciation', 'ipa', 'phonetic'],
        category: ['category', 'group'],
        difficulty: ['difficulty', 'level'],
        cefrLevel: ['cefr', 'cefrlevel', 'cefr_level'],
        partOfSpeech: ['partofspeech', 'part_of_speech', 'pos'],
        audioUrl: ['audiourl', 'audio_url', 'audio']
    };

    function newId(index) {
        return `row-${Date.now()}-${index}-${Math.random().toString(16).slice(2, 8)}`;
    }

    function clean(value) {
        if (value === undefined || value === null) return '';
        return String(value).trim();
    }

    function normalizeHeader(value) {
        return clean(value).toLowerCase().replace(/[^a-z0-9]/g, '');
    }

    function findHeader(headers, field) {
        const aliases = HEADER_ALIASES[field];
        return headers.findIndex(header => aliases.includes(normalizeHeader(header)));
    }

    function normalizeDifficulty(value, fallback) {
        const raw = clean(value || fallback).toUpperCase();
        if (['BEGINNER', 'A1', 'A2', 'EASY'].includes(raw)) return 'BEGINNER';
        if (['ADVANCED', 'C1', 'C2', 'HARD'].includes(raw)) return 'ADVANCED';
        return 'INTERMEDIATE';
    }

    function normalizeCefr(value, fallback) {
        const raw = clean(value || fallback).toUpperCase();
        return ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'].includes(raw) ? raw : '';
    }

    function applyDefaults(row, defaults, index) {
        return {
            clientRowId: row.clientRowId || newId(index),
            word: clean(row.word),
            translation: clean(row.translation),
            example: clean(row.example),
            pronunciation: clean(row.pronunciation),
            category: clean(row.category || defaults.category),
            difficulty: normalizeDifficulty(row.difficulty, defaults.difficulty),
            topicId: row.topicId || defaults.topicId || null,
            cefrLevel: normalizeCefr(row.cefrLevel, defaults.cefrLevel),
            partOfSpeech: clean(row.partOfSpeech),
            audioUrl: clean(row.audioUrl),
            confidence: row.confidence || null,
            status: row.status || 'draft',
            message: row.message || ''
        };
    }

    function parseDelimitedLine(line, delimiter) {
        const cols = [];
        let cur = '';
        let quoted = false;
        for (let i = 0; i < line.length; i++) {
            const ch = line[i];
            const next = line[i + 1];
            if (ch === '"' && quoted && next === '"') {
                cur += '"';
                i++;
            } else if (ch === '"') {
                quoted = !quoted;
            } else if (ch === delimiter && !quoted) {
                cols.push(cur.trim());
                cur = '';
            } else {
                cur += ch;
            }
        }
        cols.push(cur.trim());
        return cols;
    }

    function splitRows(text) {
        const rows = [];
        let cur = '';
        let quoted = false;
        for (let i = 0; i < text.length; i++) {
            const ch = text[i];
            const next = text[i + 1];
            if (ch === '"' && quoted && next === '"') {
                cur += '""';
                i++;
            } else if (ch === '"') {
                quoted = !quoted;
                cur += ch;
            } else if ((ch === '\n' || ch === '\r') && !quoted) {
                if (cur.trim()) rows.push(cur);
                cur = '';
                if (ch === '\r' && next === '\n') i++;
            } else {
                cur += ch;
            }
        }
        if (cur.trim()) rows.push(cur);
        return rows;
    }

    function detectDelimiter(headerLine) {
        const comma = (headerLine.match(/,/g) || []).length;
        const tab = (headerLine.match(/\t/g) || []).length;
        const semicolon = (headerLine.match(/;/g) || []).length;
        if (tab >= comma && tab >= semicolon) return '\t';
        if (semicolon > comma) return ';';
        return ',';
    }

    function parseDelimited(text, defaults) {
        const lines = splitRows(text);
        if (lines.length === 0) return [];
        const delimiter = detectDelimiter(lines[0]);
        const headers = parseDelimitedLine(lines[0], delimiter);
        const wordIndex = findHeader(headers, 'word');
        const translationIndex = findHeader(headers, 'translation');
        const hasHeader = wordIndex >= 0 || translationIndex >= 0;
        const headerMap = {
            word: hasHeader ? wordIndex : 0,
            translation: hasHeader ? translationIndex : 1,
            example: hasHeader ? findHeader(headers, 'example') : -1,
            pronunciation: hasHeader ? findHeader(headers, 'pronunciation') : -1,
            category: hasHeader ? findHeader(headers, 'category') : -1,
            difficulty: hasHeader ? findHeader(headers, 'difficulty') : -1,
            cefrLevel: hasHeader ? findHeader(headers, 'cefrLevel') : -1,
            partOfSpeech: hasHeader ? findHeader(headers, 'partOfSpeech') : -1,
            audioUrl: hasHeader ? findHeader(headers, 'audioUrl') : -1
        };
        const dataLines = hasHeader ? lines.slice(1) : lines;
        return dataLines.map((line, index) => {
            const cols = parseDelimitedLine(line, delimiter);
            const row = {};
            Object.entries(headerMap).forEach(([field, idx]) => {
                if (idx >= 0) row[field] = cols[idx] || '';
            });
            return applyDefaults(row, defaults, index);
        }).filter(row => row.word || row.translation);
    }

    function parseText(text, defaults) {
        return text.split(/\r?\n/)
            .map(line => line.trim())
            .filter(Boolean)
            .map((line, index) => {
                const match = line.match(/\s+-\s+|\s*\|\s*|\s*:\s*|,/);
                if (!match) return applyDefaults({ word: line }, defaults, index);
                const idx = match.index;
                const width = match[0].length;
                return applyDefaults({
                    word: line.slice(0, idx),
                    translation: line.slice(idx + width)
                }, defaults, index);
            })
            .filter(row => row.word || row.translation);
    }

    function parseJson(text, defaults) {
        const parsed = JSON.parse(text);
        const items = Array.isArray(parsed) ? parsed : (Array.isArray(parsed.words) ? parsed.words : []);
        return items.map((item, index) => applyDefaults({
            word: item.word || item.term || item.en || item.english,
            translation: item.translation || item.meaning || item.vi || item.vietnamese,
            example: item.example || item.sentence,
            pronunciation: item.pronunciation || item.ipa || item.phonetic,
            category: item.category,
            difficulty: item.difficulty,
            topicId: item.topicId,
            cefrLevel: item.cefrLevel || item.cefr,
            partOfSpeech: item.partOfSpeech || item.pos,
            audioUrl: item.audioUrl || item.audio
        }, defaults, index)).filter(row => row.word || row.translation);
    }

    async function parseFile(file, defaults) {
        const text = await file.text();
        const name = file.name.toLowerCase();
        if (name.endsWith('.json') || file.type === 'application/json') {
            return parseJson(text, defaults);
        }
        if (name.endsWith('.csv') || name.endsWith('.tsv') || name.endsWith('.txt')) {
            const firstLine = text.split(/\r?\n/).find(Boolean) || '';
            if (firstLine.includes(',') || firstLine.includes('\t') || firstLine.includes(';')) {
                return parseDelimited(text, defaults);
            }
        }
        return parseText(text, defaults);
    }

    window.ImportParser = {
        parseText,
        parseDelimited,
        parseJson,
        parseFile,
        applyDefaults
    };
})();
