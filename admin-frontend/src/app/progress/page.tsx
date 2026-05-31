'use client';

import { useEffect, useState } from 'react';
import AdminLayout from '@/components/AdminLayout';
import StatsCard from '@/components/StatsCard';
import { fsrs, progress, streak } from '@/lib/api';

interface FsrsStats {
  dueCount?: number;
  totalReviewed?: number;
  averageStability?: number;
  averageDifficulty?: number;
  retentionRate?: number;
  newCards?: number;
  learningCards?: number;
  reviewCards?: number;
  [key: string]: unknown;
}

interface ProgressStats {
  totalWords?: number;
  masteredWords?: number;
  learningWords?: number;
  newWords?: number;
  accuracy?: number;
  [key: string]: unknown;
}

interface StreakData {
  currentStreak?: number;
  longestStreak?: number;
  lastCheckIn?: string;
  [key: string]: unknown;
}

interface DueItem {
  id: number;
  word?: string;
  nextReviewAt?: string;
  stability?: number;
  difficulty?: number;
  reps?: number;
  lapses?: number;
  [key: string]: unknown;
}

export default function ProgressPage() {
  const [fsrsStats, setFsrsStats] = useState<FsrsStats | null>(null);
  const [progStats, setProgStats] = useState<ProgressStats | null>(null);
  const [streakData, setStreakData] = useState<StreakData | null>(null);
  const [dueItems, setDueItems] = useState<DueItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      const [f, p, s, d] = await Promise.allSettled([
        fsrs.stats(),
        progress.stats(),
        streak.get(),
        fsrs.due(),
      ]);
      if (f.status === 'fulfilled') setFsrsStats(f.value as FsrsStats);
      if (p.status === 'fulfilled') setProgStats(p.value as ProgressStats);
      if (s.status === 'fulfilled') setStreakData(s.value as StreakData);
      if (d.status === 'fulfilled') {
        const items = d.value as unknown;
        setDueItems(Array.isArray(items) ? (items as DueItem[]) : []);
      }
      setLoading(false);
    };
    load();
  }, []);

  return (
    <AdminLayout title="Progress & FSRS">
      <div className="space-y-8">
        {/* Stats Row */}
        <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          <StatsCard icon="schedule" label="Due for Review"
            value={loading ? '...' : (fsrsStats?.dueCount ?? 0)} color="#006590" />
          <StatsCard icon="star" label="Mastered Words"
            value={loading ? '...' : (progStats?.masteredWords ?? 0)} color="#755b00" />
          <StatsCard icon="local_fire_department" label="Current Streak"
            value={loading ? '...' : `${streakData?.currentStreak ?? 0} days`} color="#ba1a1a" />
          <StatsCard icon="trending_up" label="Retention Rate"
            value={loading ? '...' : fsrsStats?.retentionRate != null ? `${Math.round(fsrsStats.retentionRate as number)}%` : 'N/A'} color="#843ab4" />
        </section>

        {/* FSRS Detail */}
        <section className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* FSRS State Distribution */}
          <div className="rounded-3xl overflow-hidden"
            style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', borderBottom: '4px solid #bdc8d2' }}>
            <div className="p-5" style={{ borderBottom: '2px solid #bdc8d2', backgroundColor: '#f5f3f3' }}>
              <h3 className="font-extrabold" style={{ color: '#1b1c1c' }}>FSRS State Distribution</h3>
            </div>
            <div className="p-6 space-y-4">
              {[
                { label: 'New Cards', key: 'newCards', color: '#1cb0f6', bg: '#e8f4ff' },
                { label: 'Learning', key: 'learningCards', color: '#fec700', bg: '#fff9e0' },
                { label: 'Review', key: 'reviewCards', color: '#006590', bg: '#c8e6ff' },
                { label: 'Due Now', key: 'dueCount', color: '#843ab4', bg: '#f4d9ff' },
              ].map(({ label, key, color, bg }) => {
                const val = (fsrsStats?.[key] as number) ?? 0;
                const total = ((fsrsStats?.newCards as number) ?? 0) +
                  ((fsrsStats?.learningCards as number) ?? 0) +
                  ((fsrsStats?.reviewCards as number) ?? 0) + 1;
                const pct = Math.min(100, Math.round((val / total) * 100));
                return (
                  <div key={key}>
                    <div className="flex justify-between items-center mb-1.5">
                      <span className="text-sm font-bold" style={{ color: '#1b1c1c' }}>{label}</span>
                      <span className="text-sm font-extrabold" style={{ color }}>{loading ? '...' : val}</span>
                    </div>
                    <div className="h-3 rounded-full overflow-hidden" style={{ backgroundColor: '#efeded' }}>
                      <div className="h-full rounded-full transition-all"
                        style={{ width: `${loading ? 0 : pct}%`, backgroundColor: color, background: `linear-gradient(90deg, ${bg}, ${color})` }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* FSRS Metrics */}
          <div className="rounded-3xl overflow-hidden"
            style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', borderBottom: '4px solid #bdc8d2' }}>
            <div className="p-5" style={{ borderBottom: '2px solid #bdc8d2', backgroundColor: '#f5f3f3' }}>
              <h3 className="font-extrabold" style={{ color: '#1b1c1c' }}>FSRS Metrics</h3>
            </div>
            <div className="p-6 space-y-4">
              {[
                { label: 'Average Stability', value: fsrsStats?.averageStability != null ? `${(fsrsStats.averageStability as number).toFixed(1)} days` : 'N/A', icon: 'psychology', color: '#006590' },
                { label: 'Average Difficulty', value: fsrsStats?.averageDifficulty != null ? (fsrsStats.averageDifficulty as number).toFixed(2) : 'N/A', icon: 'fitness_center', color: '#843ab4' },
                { label: 'Total Reviews', value: (fsrsStats?.totalReviewed ?? 0).toLocaleString(), icon: 'replay', color: '#755b00' },
                { label: 'Longest Streak', value: `${streakData?.longestStreak ?? 0} days`, icon: 'emoji_events', color: '#ba1a1a' },
                { label: 'Accuracy', value: progStats?.accuracy != null ? `${Math.round(progStats.accuracy as number)}%` : 'N/A', icon: 'target', color: '#006590' },
              ].map(({ label, value, icon, color }) => (
                <div key={label} className="flex items-center gap-4 p-3 rounded-xl"
                  style={{ backgroundColor: '#f5f3f3' }}>
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                    style={{ backgroundColor: color + '20' }}>
                    <span className="material-symbols-outlined text-lg" style={{ color }}>{icon}</span>
                  </div>
                  <div className="flex-1">
                    <p className="text-xs font-bold uppercase tracking-wide" style={{ color: '#3e4850' }}>{label}</p>
                    <p className="font-extrabold text-base" style={{ color: '#1b1c1c' }}>
                      {loading ? '...' : value}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Due Items Table */}
        <section className="rounded-3xl overflow-hidden"
          style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', borderBottom: '4px solid #bdc8d2' }}>
          <div className="p-5 flex justify-between items-center"
            style={{ borderBottom: '2px solid #bdc8d2', backgroundColor: '#f5f3f3' }}>
            <h3 className="font-extrabold" style={{ color: '#1b1c1c' }}>
              Due for Review ({dueItems.length})
            </h3>
            <span className="text-xs font-bold px-3 py-1 rounded-full"
              style={{ backgroundColor: '#ffdad6', color: '#93000a' }}>
              Requires attention
            </span>
          </div>
          <div className="overflow-x-auto">
            {loading ? (
              <div className="p-8 text-center text-sm" style={{ color: '#3e4850' }}>Loading...</div>
            ) : dueItems.length === 0 ? (
              <div className="p-8 text-center">
                <span className="material-symbols-outlined text-4xl block mb-2" style={{ color: '#1cb0f6' }}>check_circle</span>
                <p className="text-sm font-bold" style={{ color: '#3e4850' }}>All caught up! No cards due.</p>
              </div>
            ) : (
              <table className="w-full text-left">
                <thead>
                  <tr style={{ backgroundColor: '#f5f3f3', borderBottom: '2px solid #efeded' }}>
                    {['Word', 'Stability', 'Difficulty', 'Reps', 'Lapses', 'Next Review'].map(h => (
                      <th key={h} className="px-5 py-3 text-xs font-bold uppercase tracking-widest"
                        style={{ color: '#3e4850' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {dueItems.slice(0, 20).map((item, i) => (
                    <tr key={item.id ?? i} style={{ borderTop: '1px solid #efeded' }}
                      onMouseEnter={e => (e.currentTarget.style.backgroundColor = '#f5f3f3')}
                      onMouseLeave={e => (e.currentTarget.style.backgroundColor = '')}>
                      <td className="px-5 py-3 font-bold text-sm" style={{ color: '#1b1c1c' }}>
                        {String(item.word ?? item.id)}
                      </td>
                      <td className="px-5 py-3 text-sm font-medium" style={{ color: '#006590' }}>
                        {item.stability != null ? (item.stability as number).toFixed(1) : '—'}
                      </td>
                      <td className="px-5 py-3 text-sm font-medium" style={{ color: '#843ab4' }}>
                        {item.difficulty != null ? (item.difficulty as number).toFixed(2) : '—'}
                      </td>
                      <td className="px-5 py-3 text-sm font-bold">{item.reps ?? '—'}</td>
                      <td className="px-5 py-3">
                        {(item.lapses as number) > 0 ? (
                          <span className="px-2 py-0.5 rounded-full text-xs font-bold"
                            style={{ backgroundColor: '#ffdad6', color: '#93000a' }}>
                            {item.lapses}
                          </span>
                        ) : (
                          <span className="text-sm" style={{ color: '#bdc8d2' }}>0</span>
                        )}
                      </td>
                      <td className="px-5 py-3 text-xs font-medium" style={{ color: '#3e4850' }}>
                        {item.nextReviewAt ? new Date(item.nextReviewAt as string).toLocaleDateString() : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </section>
      </div>
    </AdminLayout>
  );
}
