'use client';

import { useEffect, useState } from 'react';
import AdminLayout from '@/components/AdminLayout';
import StatsCard from '@/components/StatsCard';
import { quiz } from '@/lib/api';

interface QuizStats {
  totalSessions?: number;
  completedSessions?: number;
  totalAnswers?: number;
  correctAnswers?: number;
  accuracy?: number;
  [key: string]: unknown;
}

interface QuizSession {
  id: number | string;
  status?: string;
  score?: number;
  totalQuestions?: number;
  correctAnswers?: number;
  createdAt?: string;
  completedAt?: string;
  [key: string]: unknown;
}

export default function QuizPage() {
  const [stats, setStats] = useState<QuizStats | null>(null);
  const [sessions, setSessions] = useState<QuizSession[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      const [s, r] = await Promise.allSettled([quiz.stats(), quiz.recentSessions()]);
      if (s.status === 'fulfilled') setStats(s.value as QuizStats);
      if (r.status === 'fulfilled') {
        const val = r.value as unknown;
        setSessions(Array.isArray(val) ? (val as QuizSession[]) : []);
      }
      setLoading(false);
    };
    load();
  }, []);

  const accuracy = stats?.accuracy ?? (
    stats?.correctAnswers && stats?.totalAnswers
      ? Math.round(((stats.correctAnswers as number) / (stats.totalAnswers as number)) * 100)
      : null
  );

  return (
    <AdminLayout title="Quiz Stats">
      <div className="space-y-8">
        {/* Stats */}
        <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          <StatsCard icon="quiz" label="Total Sessions"
            value={loading ? '...' : (stats?.totalSessions ?? 0)} color="#006590" />
          <StatsCard icon="check_circle" label="Completed"
            value={loading ? '...' : (stats?.completedSessions ?? 0)} color="#755b00" />
          <StatsCard icon="help" label="Total Answers"
            value={loading ? '...' : (stats?.totalAnswers ?? 0)} color="#843ab4" />
          <StatsCard icon="target" label="Accuracy"
            value={loading ? '...' : accuracy != null ? `${Math.round(accuracy as number)}%` : 'N/A'} color="#006590" />
        </section>

        {/* Accuracy Gauge */}
        {!loading && accuracy != null && (
          <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="rounded-3xl p-8 flex flex-col items-center justify-center text-center"
              style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', borderBottom: '4px solid #bdc8d2' }}>
              <p className="text-xs font-bold uppercase tracking-widest mb-4" style={{ color: '#3e4850' }}>Overall Accuracy</p>
              <div className="relative w-32 h-32">
                <svg viewBox="0 0 100 100" className="w-full h-full -rotate-90">
                  <circle cx="50" cy="50" r="40" fill="none" stroke="#efeded" strokeWidth="12" />
                  <circle cx="50" cy="50" r="40" fill="none" stroke="#006590"
                    strokeWidth="12" strokeLinecap="round"
                    strokeDasharray={`${2 * Math.PI * 40}`}
                    strokeDashoffset={`${2 * Math.PI * 40 * (1 - (accuracy as number) / 100)}`}
                    style={{ transition: 'stroke-dashoffset 1s ease' }} />
                </svg>
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-2xl font-black" style={{ color: '#006590' }}>
                    {Math.round(accuracy as number)}%
                  </span>
                </div>
              </div>
              <p className="text-sm font-medium mt-4" style={{ color: '#3e4850' }}>
                {(accuracy as number) >= 90 ? 'Excellent!' : (accuracy as number) >= 70 ? 'Good progress' : 'Needs improvement'}
              </p>
            </div>

            <div className="lg:col-span-2 rounded-3xl overflow-hidden"
              style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', borderBottom: '4px solid #bdc8d2' }}>
              <div className="p-5" style={{ borderBottom: '2px solid #bdc8d2', backgroundColor: '#f5f3f3' }}>
                <h3 className="font-extrabold" style={{ color: '#1b1c1c' }}>Performance Breakdown</h3>
              </div>
              <div className="p-6 space-y-4">
                {[
                  { label: 'Correct Answers', value: stats?.correctAnswers ?? 0, color: '#006590' },
                  { label: 'Incorrect Answers', value: ((stats?.totalAnswers as number) ?? 0) - ((stats?.correctAnswers as number) ?? 0), color: '#ba1a1a' },
                  { label: 'Sessions Completed', value: stats?.completedSessions ?? 0, color: '#755b00' },
                ].map(({ label, value, color }) => {
                  const total = Math.max(1, (stats?.totalAnswers as number) ?? 1);
                  const pct = Math.min(100, Math.round(((value as number) / total) * 100));
                  return (
                    <div key={label}>
                      <div className="flex justify-between items-center mb-2">
                        <span className="text-sm font-bold" style={{ color: '#1b1c1c' }}>{label}</span>
                        <span className="font-extrabold text-sm" style={{ color }}>{(value as number).toLocaleString()}</span>
                      </div>
                      <div className="h-3 rounded-full" style={{ backgroundColor: '#efeded' }}>
                        <div className="h-full rounded-full"
                          style={{ width: `${pct}%`, backgroundColor: color }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </section>
        )}

        {/* Recent Sessions Table */}
        <section className="rounded-3xl overflow-hidden"
          style={{ backgroundColor: '#ffffff', border: '2px solid #bdc8d2', borderBottom: '4px solid #bdc8d2' }}>
          <div className="p-5" style={{ borderBottom: '2px solid #bdc8d2', backgroundColor: '#f5f3f3' }}>
            <h3 className="font-extrabold" style={{ color: '#1b1c1c' }}>Recent Quiz Sessions</h3>
          </div>
          <div className="overflow-x-auto">
            {loading ? (
              <div className="p-8 text-center text-sm" style={{ color: '#3e4850' }}>Loading...</div>
            ) : sessions.length === 0 ? (
              <div className="p-8 text-center">
                <span className="material-symbols-outlined text-5xl block mb-2" style={{ color: '#bdc8d2' }}>quiz</span>
                <p className="text-sm" style={{ color: '#3e4850' }}>No quiz sessions yet.</p>
              </div>
            ) : (
              <table className="w-full text-left">
                <thead>
                  <tr style={{ backgroundColor: '#f5f3f3', borderBottom: '2px solid #efeded' }}>
                    {['Session', 'Status', 'Score', 'Correct', 'Total Q', 'Date'].map(h => (
                      <th key={h} className="px-5 py-3 text-xs font-bold uppercase tracking-widest"
                        style={{ color: '#3e4850' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {sessions.map((s, i) => {
                    const statusColor = s.status === 'COMPLETED'
                      ? { bg: '#c8e6ff', color: '#004c6e' }
                      : { bg: '#ffdf92', color: '#594400' };
                    return (
                      <tr key={s.id ?? i} style={{ borderTop: '1px solid #efeded' }}
                        onMouseEnter={e => (e.currentTarget.style.backgroundColor = '#f5f3f3')}
                        onMouseLeave={e => (e.currentTarget.style.backgroundColor = '')}>
                        <td className="px-5 py-3 font-bold text-sm" style={{ color: '#1b1c1c' }}>#{String(s.id)}</td>
                        <td className="px-5 py-3">
                          <span className="px-3 py-1 rounded-full text-xs font-bold"
                            style={{ backgroundColor: statusColor.bg, color: statusColor.color }}>
                            {String(s.status ?? 'UNKNOWN')}
                          </span>
                        </td>
                        <td className="px-5 py-3 font-extrabold text-sm" style={{ color: '#006590' }}>
                          {s.score != null ? `${s.score}` : '—'}
                        </td>
                        <td className="px-5 py-3 text-sm" style={{ color: '#3e4850' }}>
                          {s.correctAnswers != null ? `${s.correctAnswers}` : '—'}
                        </td>
                        <td className="px-5 py-3 text-sm" style={{ color: '#3e4850' }}>
                          {s.totalQuestions != null ? `${s.totalQuestions}` : '—'}
                        </td>
                        <td className="px-5 py-3 text-xs" style={{ color: '#3e4850' }}>
                          {s.createdAt ? new Date(s.createdAt as string).toLocaleDateString() : '—'}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </section>
      </div>
    </AdminLayout>
  );
}
