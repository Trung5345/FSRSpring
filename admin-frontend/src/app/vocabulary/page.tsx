'use client';

import { useEffect, useState, useCallback } from 'react';
import AdminLayout from '@/components/AdminLayout';
import { words } from '@/lib/api';

interface Word {
  id: number;
  word: string;
  meaning?: string;
  definition?: string;
  category?: string;
  partOfSpeech?: string;
  difficulty?: string;
  example?: string;
  imageUrl?: string;
}

interface WordsPage {
  content?: Word[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
}

const DIFFICULTIES = ['', 'BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

function Modal({ word, onClose, onSave }: {
  word: Partial<Word> | null;
  onClose: () => void;
  onSave: (data: Partial<Word>) => Promise<void>;
}) {
  const [form, setForm] = useState<Partial<Word>>(word ?? {});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const set = (k: keyof Word, v: string) => setForm(f => ({ ...f, [k]: v }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await onSave(form);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(0,0,0,0.4)' }}>
      <div className="w-full max-w-lg rounded-3xl overflow-hidden"
        style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', borderBottom: '6px solid #bdc8d2' }}>
        <div className="p-6 flex justify-between items-center"
          style={{ borderBottom: '2px solid #bdc8d2', backgroundColor: '#f5f3f3' }}>
          <h3 className="text-lg font-extrabold" style={{ color: '#1b1c1c' }}>
            {form.id ? 'Edit Word' : 'Add New Word'}
          </h3>
          <button onClick={onClose} className="p-2 rounded-xl"
            style={{ color: '#3e4850' }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#e9e8e7')}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '')}>
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
        <form onSubmit={submit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 rounded-xl text-sm font-semibold"
              style={{ backgroundColor: '#ffdad6', color: '#93000a' }}>{error}</div>
          )}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider mb-1.5"
              style={{ color: '#3e4850' }}>Word / Phrase *</label>
            <input required value={form.word ?? ''} onChange={e => set('word', e.target.value)}
              className="w-full p-3.5 rounded-xl outline-none text-sm font-medium transition-all"
              style={{ border: '2px solid #bdc8d2', backgroundColor: '#fbf9f9', color: '#1b1c1c' }}
              onFocus={e => (e.target.style.borderColor = '#006590')}
              onBlur={e => (e.target.style.borderColor = '#bdc8d2')}
              placeholder="e.g. Ephemeral" />
          </div>
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider mb-1.5"
              style={{ color: '#3e4850' }}>Meaning / Translation</label>
            <textarea value={form.meaning ?? ''} onChange={e => set('meaning', e.target.value)}
              rows={2}
              className="w-full p-3.5 rounded-xl outline-none text-sm font-medium transition-all resize-none"
              style={{ border: '2px solid #bdc8d2', backgroundColor: '#fbf9f9', color: '#1b1c1c' }}
              onFocus={e => (e.target.style.borderColor = '#006590')}
              onBlur={e => (e.target.style.borderColor = '#bdc8d2')}
              placeholder="Enter meaning..." />
          </div>
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider mb-1.5"
              style={{ color: '#3e4850' }}>Definition</label>
            <textarea value={form.definition ?? ''} onChange={e => set('definition', e.target.value)}
              rows={2}
              className="w-full p-3.5 rounded-xl outline-none text-sm font-medium transition-all resize-none"
              style={{ border: '2px solid #bdc8d2', backgroundColor: '#fbf9f9', color: '#1b1c1c' }}
              onFocus={e => (e.target.style.borderColor = '#006590')}
              onBlur={e => (e.target.style.borderColor = '#bdc8d2')}
              placeholder="Enter definition..." />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider mb-1.5"
                style={{ color: '#3e4850' }}>Difficulty</label>
              <select value={form.difficulty ?? ''} onChange={e => set('difficulty', e.target.value)}
                className="w-full p-3.5 rounded-xl outline-none text-sm font-medium"
                style={{ border: '2px solid #bdc8d2', backgroundColor: '#fbf9f9', color: '#1b1c1c' }}>
                {DIFFICULTIES.map(d => (
                  <option key={d} value={d}>{d || 'Select...'}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider mb-1.5"
                style={{ color: '#3e4850' }}>Category</label>
              <input value={form.category ?? ''} onChange={e => set('category', e.target.value)}
                className="w-full p-3.5 rounded-xl outline-none text-sm font-medium transition-all"
                style={{ border: '2px solid #bdc8d2', backgroundColor: '#fbf9f9', color: '#1b1c1c' }}
                onFocus={e => (e.target.style.borderColor = '#006590')}
                onBlur={e => (e.target.style.borderColor = '#bdc8d2')}
                placeholder="e.g. Travel" />
            </div>
          </div>
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider mb-1.5"
              style={{ color: '#3e4850' }}>Example Sentence</label>
            <input value={form.example ?? ''} onChange={e => set('example', e.target.value)}
              className="w-full p-3.5 rounded-xl outline-none text-sm font-medium transition-all"
              style={{ border: '2px solid #bdc8d2', backgroundColor: '#fbf9f9', color: '#1b1c1c' }}
              onFocus={e => (e.target.style.borderColor = '#006590')}
              onBlur={e => (e.target.style.borderColor = '#bdc8d2')}
              placeholder="e.g. The ephemeral beauty of cherry blossoms..." />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose}
              className="btn-tactile flex-1 py-3 rounded-xl font-bold text-sm uppercase tracking-wide"
              style={{ backgroundColor: '#efeded', color: '#1b1c1c', borderBottom: '4px solid #bdc8d2' }}>
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="btn-tactile flex-1 py-3 rounded-xl font-bold text-sm uppercase tracking-wide"
              style={{ backgroundColor: saving ? '#bdc8d2' : '#006590', color: '#ffffff', borderBottom: '4px solid #004c6e' }}>
              {saving ? 'Saving...' : (form.id ? 'Update' : 'Add Word')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

const difficultyColors: Record<string, { bg: string; color: string }> = {
  BEGINNER: { bg: '#c8e6ff', color: '#004c6e' },
  INTERMEDIATE: { bg: '#ffdf92', color: '#594400' },
  ADVANCED: { bg: '#ffdad6', color: '#93000a' },
};

export default function VocabularyPage() {
  const [data, setData] = useState<WordsPage | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState<{ open: boolean; word: Partial<Word> | null }>({ open: false, word: null });
  const [deleting, setDeleting] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await words.list({ page, size: 15, search: search || undefined });
      setData(res as WordsPage);
    } catch {
      // API might return array or page object
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => { load(); }, [load]);

  const handleSave = async (formData: Partial<Word>) => {
    if (formData.id) {
      await words.update(formData.id, formData);
    } else {
      await words.create(formData);
    }
    load();
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this word?')) return;
    setDeleting(id);
    try {
      await words.delete(id);
      load();
    } finally {
      setDeleting(null);
    }
  };

  const wordList: Word[] = Array.isArray(data)
    ? (data as unknown as Word[])
    : (data?.content ?? []);

  const totalPages = data?.totalPages ?? 1;

  return (
    <AdminLayout title="Vocabulary">
      <div className="space-y-6">
        {/* Header Actions */}
        <div className="flex flex-col sm:flex-row gap-4 justify-between items-start sm:items-center">
          <div className="flex gap-3">
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-lg"
                style={{ color: '#3e4850' }}>search</span>
              <input
                value={searchInput}
                onChange={e => setSearchInput(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') { setSearch(searchInput); setPage(0); } }}
                placeholder="Search words..."
                className="pl-10 pr-4 py-2.5 rounded-xl text-sm font-medium outline-none"
                style={{ border: '2px solid #bdc8d2', backgroundColor: '#ffffff', color: '#1b1c1c', width: '260px' }}
                onFocus={e => (e.target.style.borderColor = '#006590')}
                onBlur={e => (e.target.style.borderColor = '#bdc8d2')}
              />
            </div>
            <button onClick={() => { setSearch(searchInput); setPage(0); }}
              className="btn-tactile px-4 py-2.5 rounded-xl font-bold text-sm"
              style={{ backgroundColor: '#efeded', color: '#1b1c1c', borderBottom: '3px solid #bdc8d2' }}>
              Search
            </button>
          </div>
          <button
            onClick={() => setModal({ open: true, word: {} })}
            className="btn-tactile flex items-center gap-2 px-6 py-3 rounded-xl font-bold text-sm uppercase tracking-wide"
            style={{ backgroundColor: '#006590', color: '#ffffff', borderBottom: '4px solid #004c6e' }}>
            <span className="material-symbols-outlined text-base">add</span>
            Add Word
          </button>
        </div>

        {/* Table */}
        <div className="rounded-3xl overflow-hidden"
          style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', borderBottom: '4px solid #bdc8d2' }}>
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr style={{ backgroundColor: '#f5f3f3', borderBottom: '2px solid #bdc8d2' }}>
                  {['Word', 'Meaning', 'Category', 'Difficulty', 'Actions'].map(h => (
                    <th key={h} className="px-5 py-4 text-xs font-bold uppercase tracking-widest"
                      style={{ color: '#3e4850' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={5} className="p-10 text-center text-sm" style={{ color: '#3e4850' }}>
                      Loading...
                    </td>
                  </tr>
                ) : wordList.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="p-10 text-center text-sm" style={{ color: '#3e4850' }}>
                      No words found. Add your first word!
                    </td>
                  </tr>
                ) : wordList.map(w => {
                  const dc = difficultyColors[w.difficulty ?? ''] ?? { bg: '#efeded', color: '#1b1c1c' };
                  return (
                    <tr key={w.id} style={{ borderTop: '1px solid #efeded' }}
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#f5f3f3')}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '')}>
                      <td className="px-5 py-4 font-bold text-sm" style={{ color: '#1b1c1c' }}>{w.word}</td>
                      <td className="px-5 py-4 text-sm max-w-xs truncate" style={{ color: '#3e4850' }}>
                        {w.meaning ?? w.definition ?? '—'}
                      </td>
                      <td className="px-5 py-4 text-sm" style={{ color: '#3e4850' }}>{w.category ?? '—'}</td>
                      <td className="px-5 py-4">
                        {w.difficulty ? (
                          <span className="px-3 py-1 rounded-full text-xs font-bold"
                            style={{ backgroundColor: dc.bg, color: dc.color }}>
                            {w.difficulty}
                          </span>
                        ) : <span style={{ color: '#bdc8d2' }}>—</span>}
                      </td>
                      <td className="px-5 py-4">
                        <div className="flex gap-2">
                          <button onClick={() => setModal({ open: true, word: w })}
                            className="p-1.5 rounded-lg transition-colors"
                            style={{ color: '#006590' }}
                            onMouseEnter={e => (e.currentTarget.style.backgroundColor = '#e8f4ff')}
                            onMouseLeave={e => (e.currentTarget.style.backgroundColor = '')}>
                            <span className="material-symbols-outlined text-base">edit</span>
                          </button>
                          <button onClick={() => handleDelete(w.id)}
                            disabled={deleting === w.id}
                            className="p-1.5 rounded-lg transition-colors"
                            style={{ color: '#ba1a1a' }}
                            onMouseEnter={e => (e.currentTarget.style.backgroundColor = '#ffdad6')}
                            onMouseLeave={e => (e.currentTarget.style.backgroundColor = '')}>
                            <span className="material-symbols-outlined text-base">
                              {deleting === w.id ? 'hourglass_empty' : 'delete'}
                            </span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-between items-center px-5 py-4"
              style={{ borderTop: '2px solid #bdc8d2' }}>
              <span className="text-sm font-medium" style={{ color: '#3e4850' }}>
                Page {page + 1} of {totalPages}
              </span>
              <div className="flex gap-2">
                <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
                  className="btn-tactile px-4 py-2 rounded-xl text-sm font-bold"
                  style={{
                    backgroundColor: page === 0 ? '#efeded' : '#006590',
                    color: page === 0 ? '#bdc8d2' : '#ffffff',
                    borderBottom: '3px solid #004c6e',
                  }}>
                  ← Prev
                </button>
                <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}
                  className="btn-tactile px-4 py-2 rounded-xl text-sm font-bold"
                  style={{
                    backgroundColor: page >= totalPages - 1 ? '#efeded' : '#006590',
                    color: page >= totalPages - 1 ? '#bdc8d2' : '#ffffff',
                    borderBottom: '3px solid #004c6e',
                  }}>
                  Next →
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Modal */}
      {modal.open && (
        <Modal
          word={modal.word}
          onClose={() => setModal({ open: false, word: null })}
          onSave={handleSave}
        />
      )}
    </AdminLayout>
  );
}
