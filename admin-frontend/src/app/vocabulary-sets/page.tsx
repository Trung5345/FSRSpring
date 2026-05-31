'use client';

import { useEffect, useState, useCallback } from 'react';
import AdminLayout from '@/components/AdminLayout';
import { sets } from '@/lib/api';

interface VocabSet {
  id: number;
  name: string;
  description?: string;
  wordCount?: number;
  createdAt?: string;
}

export default function VocabularySetsPage() {
  const [setList, setSetList] = useState<VocabSet[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({ name: '', description: '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState<VocabSet | null>(null);
  const [setWords, setSetWords] = useState<unknown[]>([]);
  const [loadingWords, setLoadingWords] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await sets.list();
      setSetList(Array.isArray(res) ? (res as VocabSet[]) : []);
    } catch {
      setSetList([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await sets.create(form);
      setShowModal(false);
      setForm({ name: '', description: '' });
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this set?')) return;
    try {
      await sets.delete(id);
      if (selected?.id === id) setSelected(null);
      load();
    } catch { /* ignore */ }
  };

  const loadSetWords = async (set: VocabSet) => {
    setSelected(set);
    setLoadingWords(true);
    try {
      const res = await sets.words(set.id);
      setSetWords(Array.isArray(res) ? res : []);
    } catch {
      setSetWords([]);
    } finally {
      setLoadingWords(false);
    }
  };

  return (
    <AdminLayout title="Vocabulary Sets">
      <div className="space-y-6">
        {/* Header */}
        <div className="flex justify-end">
          <button onClick={() => setShowModal(true)}
            className="btn-tactile flex items-center gap-2 px-6 py-3 rounded-xl font-bold text-sm uppercase tracking-wide"
            style={{ backgroundColor: '#006590', color: '#ffffff', borderBottom: '4px solid #004c6e' }}>
            <span className="material-symbols-outlined text-base">add</span>
            New Set
          </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Sets List */}
          <div className="lg:col-span-1 space-y-3">
            <h3 className="text-sm font-bold uppercase tracking-widest" style={{ color: '#3e4850' }}>
              All Sets ({setList.length})
            </h3>
            {loading ? (
              <div className="p-8 rounded-2xl text-center text-sm" style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', color: '#3e4850' }}>
                Loading...
              </div>
            ) : setList.length === 0 ? (
              <div className="p-8 rounded-2xl text-center text-sm" style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', color: '#3e4850' }}>
                No sets yet. Create one!
              </div>
            ) : setList.map(s => (
              <div key={s.id}
                onClick={() => loadSetWords(s)}
                className="p-4 rounded-2xl cursor-pointer transition-all"
                style={{
                  backgroundColor: selected?.id === s.id ? '#006590' : '#ffffff',
                  color: selected?.id === s.id ? '#ffffff' : '#1b1c1c',
                  border: `2px solid ${selected?.id === s.id ? '#004c6e' : '#bdc8d2'}`,
                  borderBottom: `4px solid ${selected?.id === s.id ? '#004c6e' : '#bdc8d2'}`,
                }}>
                <div className="flex justify-between items-start">
                  <div>
                    <p className="font-bold text-sm">{s.name}</p>
                    {s.description && (
                      <p className="text-xs mt-0.5 opacity-70 truncate max-w-[160px]">{s.description}</p>
                    )}
                    <p className="text-xs mt-1 font-bold opacity-60">
                      {s.wordCount ?? 0} words
                    </p>
                  </div>
                  <button onClick={e => { e.stopPropagation(); handleDelete(s.id); }}
                    className="p-1 rounded-lg"
                    style={{ color: selected?.id === s.id ? 'rgba(255,255,255,0.7)' : '#ba1a1a' }}>
                    <span className="material-symbols-outlined text-sm">delete</span>
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Set Words */}
          <div className="lg:col-span-2 rounded-3xl overflow-hidden"
            style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', borderBottom: '4px solid #bdc8d2' }}>
            {!selected ? (
              <div className="h-64 flex items-center justify-center text-sm"
                style={{ color: '#3e4850' }}>
                <div className="text-center">
                  <span className="material-symbols-outlined text-5xl mb-3 block" style={{ color: '#bdc8d2' }}>
                    collections_bookmark
                  </span>
                  Select a set to view its words
                </div>
              </div>
            ) : (
              <>
                <div className="p-5 flex justify-between items-center"
                  style={{ borderBottom: '2px solid #bdc8d2', backgroundColor: '#f5f3f3' }}>
                  <div>
                    <h3 className="font-extrabold" style={{ color: '#1b1c1c' }}>{selected.name}</h3>
                    {selected.description && (
                      <p className="text-xs mt-0.5" style={{ color: '#3e4850' }}>{selected.description}</p>
                    )}
                  </div>
                  <span className="text-sm font-bold px-3 py-1 rounded-full"
                    style={{ backgroundColor: '#c8e6ff', color: '#004c6e' }}>
                    {setWords.length} words
                  </span>
                </div>
                {loadingWords ? (
                  <div className="p-8 text-center text-sm" style={{ color: '#3e4850' }}>Loading words...</div>
                ) : setWords.length === 0 ? (
                  <div className="p-8 text-center text-sm" style={{ color: '#3e4850' }}>
                    No words in this set yet.
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left">
                      <thead>
                        <tr style={{ backgroundColor: '#f5f3f3', borderBottom: '2px solid #efeded' }}>
                          {['Word', 'Meaning', 'Difficulty'].map(h => (
                            <th key={h} className="px-5 py-3 text-xs font-bold uppercase tracking-widest"
                              style={{ color: '#3e4850' }}>{h}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {(setWords as Record<string, unknown>[]).map((w, i) => (
                          <tr key={i} style={{ borderTop: '1px solid #efeded' }}
                            onMouseEnter={e => (e.currentTarget.style.backgroundColor = '#f5f3f3')}
                            onMouseLeave={e => (e.currentTarget.style.backgroundColor = '')}>
                            <td className="px-5 py-3 font-bold text-sm" style={{ color: '#1b1c1c' }}>
                              {String(w.word ?? '')}
                            </td>
                            <td className="px-5 py-3 text-sm" style={{ color: '#3e4850' }}>
                              {String(w.meaning ?? w.definition ?? '—')}
                            </td>
                            <td className="px-5 py-3 text-xs font-bold">
                              {w.difficulty ? (
                                <span className="px-2 py-0.5 rounded-full"
                                  style={{ backgroundColor: '#c8e6ff', color: '#004c6e' }}>
                                  {String(w.difficulty)}
                                </span>
                              ) : '—'}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </div>

      {/* Create Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
          style={{ backgroundColor: 'rgba(0,0,0,0.4)' }}>
          <div className="w-full max-w-md rounded-3xl overflow-hidden"
            style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', borderBottom: '6px solid #bdc8d2' }}>
            <div className="p-6 flex justify-between items-center"
              style={{ borderBottom: '2px solid #bdc8d2', backgroundColor: '#f5f3f3' }}>
              <h3 className="text-lg font-extrabold" style={{ color: '#1b1c1c' }}>New Vocabulary Set</h3>
              <button onClick={() => setShowModal(false)} className="p-2 rounded-xl"
                style={{ color: '#3e4850' }}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <form onSubmit={handleCreate} className="p-6 space-y-4">
              {error && (
                <div className="p-3 rounded-xl text-sm font-semibold"
                  style={{ backgroundColor: '#ffdad6', color: '#93000a' }}>{error}</div>
              )}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider mb-1.5"
                  style={{ color: '#3e4850' }}>Set Name *</label>
                <input required value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full p-3.5 rounded-xl outline-none text-sm font-medium"
                  style={{ border: '2px solid #bdc8d2', backgroundColor: '#fbf9f9', color: '#1b1c1c' }}
                  onFocus={e => (e.target.style.borderColor = '#006590')}
                  onBlur={e => (e.target.style.borderColor = '#bdc8d2')}
                  placeholder="e.g. Business English" />
              </div>
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider mb-1.5"
                  style={{ color: '#3e4850' }}>Description</label>
                <textarea value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  rows={3}
                  className="w-full p-3.5 rounded-xl outline-none text-sm font-medium resize-none"
                  style={{ border: '2px solid #bdc8d2', backgroundColor: '#fbf9f9', color: '#1b1c1c' }}
                  onFocus={e => (e.target.style.borderColor = '#006590')}
                  onBlur={e => (e.target.style.borderColor = '#bdc8d2')}
                  placeholder="Optional description..." />
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowModal(false)}
                  className="btn-tactile flex-1 py-3 rounded-xl font-bold text-sm uppercase"
                  style={{ backgroundColor: '#efeded', color: '#1b1c1c', borderBottom: '4px solid #bdc8d2' }}>
                  Cancel
                </button>
                <button type="submit" disabled={saving}
                  className="btn-tactile flex-1 py-3 rounded-xl font-bold text-sm uppercase"
                  style={{ backgroundColor: '#006590', color: '#ffffff', borderBottom: '4px solid #004c6e' }}>
                  {saving ? 'Creating...' : 'Create Set'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
