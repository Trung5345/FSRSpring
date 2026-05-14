(function () {
    const DB_NAME = 'fsrs-ai-import';
    const DB_VERSION = 1;

    function openDb() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(DB_NAME, DB_VERSION);
            request.onupgradeneeded = () => {
                const db = request.result;
                if (!db.objectStoreNames.contains('drafts')) {
                    db.createObjectStore('drafts', { keyPath: 'id' });
                }
                if (!db.objectStoreNames.contains('meta')) {
                    db.createObjectStore('meta', { keyPath: 'id' });
                }
                if (!db.objectStoreNames.contains('retryQueue')) {
                    db.createObjectStore('retryQueue', { keyPath: 'id' });
                }
            };
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async function withStore(storeName, mode, callback) {
        const db = await openDb();
        return new Promise((resolve, reject) => {
            const tx = db.transaction(storeName, mode);
            const store = tx.objectStore(storeName);
            let result;
            tx.oncomplete = () => {
                db.close();
                resolve(result);
            };
            tx.onerror = () => {
                db.close();
                reject(tx.error);
            };
            result = callback(store);
        });
    }

    function promisify(request) {
        return new Promise((resolve, reject) => {
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    window.ImportStore = {
        async saveDraft(id, data) {
            return withStore('drafts', 'readwrite', store => store.put({
                id,
                data,
                updatedAt: new Date().toISOString()
            }));
        },
        async loadDraft(id) {
            const db = await openDb();
            return new Promise((resolve, reject) => {
                const tx = db.transaction('drafts', 'readonly');
                const request = tx.objectStore('drafts').get(id);
                request.onsuccess = () => {
                    db.close();
                    resolve(request.result ? request.result.data : null);
                };
                request.onerror = () => {
                    db.close();
                    reject(request.error);
                };
            });
        },
        async clearDraft(id) {
            return withStore('drafts', 'readwrite', store => store.delete(id));
        },
        async setMeta(id, data) {
            return withStore('meta', 'readwrite', store => store.put({
                id,
                data,
                updatedAt: new Date().toISOString()
            }));
        },
        async getMeta(id) {
            const db = await openDb();
            return new Promise((resolve, reject) => {
                const tx = db.transaction('meta', 'readonly');
                const request = tx.objectStore('meta').get(id);
                request.onsuccess = () => {
                    db.close();
                    resolve(request.result ? request.result.data : null);
                };
                request.onerror = () => {
                    db.close();
                    reject(request.error);
                };
            });
        },
        async enqueueRetry(data) {
            const id = data.id || `retry-${Date.now()}-${Math.random().toString(16).slice(2)}`;
            return withStore('retryQueue', 'readwrite', store => store.put({
                id,
                data: { ...data, id },
                createdAt: new Date().toISOString()
            }));
        },
        async listRetries() {
            const db = await openDb();
            const tx = db.transaction('retryQueue', 'readonly');
            const request = tx.objectStore('retryQueue').getAll();
            const rows = await promisify(request);
            db.close();
            return rows.map(row => row.data);
        }
    };
})();
