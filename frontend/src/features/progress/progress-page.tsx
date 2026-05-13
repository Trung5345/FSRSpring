"use client";

import { IconChartBar, IconClock, IconTarget } from "@tabler/icons-react";
import { useEffect, useRef, useState } from "react";
import { AppFlameIcon } from "@/components/icons/app-icons";
import { AppShell } from "@/components/layout/app-shell";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Table, Td, Th } from "@/components/ui/table";
import { useToast } from "@/components/ui/toast";
import { api } from "@/lib/api";
import { formatDateTime, formatPercent, masteryLabel } from "@/lib/utils";
import type { QuizSession, UserProgress } from "@/types/api";

export function ProgressPage() {
  const [stats, setStats] = useState<Record<string, number>>({});
  const [fsrs, setFsrs] = useState<Record<string, number>>({});
  const [streak, setStreak] = useState<Record<string, number>>({});
  const [progress, setProgress] = useState<UserProgress[]>([]);
  const [sessions, setSessions] = useState<QuizSession[]>([]);
  const [quizStats, setQuizStats] = useState<Record<string, number>>({});
  const { toast } = useToast();
  const loadErrorShown = useRef(false);

  useEffect(() => {
    const emptyStats: Record<string, number> = {};
    function notifyLoadError() {
      if (loadErrorShown.current) return;
      loadErrorShown.current = true;
      toast("Backend API chưa sẵn sàng, trang đang hiển thị dữ liệu dự phòng.", "warning");
    }

    Promise.all([
      api.progressStats().catch(() => {
        notifyLoadError();
        return emptyStats;
      }),
      api.fsrsStats().catch(() => {
        notifyLoadError();
        return emptyStats;
      }),
      api.streak().catch(() => {
        notifyLoadError();
        return emptyStats;
      }),
      api.progress().catch(() => {
        notifyLoadError();
        return [];
      }),
      api.recentQuizSessions().catch(() => {
        notifyLoadError();
        return [];
      }),
      api.quizStats().catch(() => {
        notifyLoadError();
        return emptyStats;
      })
    ]).then(([a, b, c, d, e, f]) => {
      setStats(a);
      setFsrs(b);
      setStreak(c);
      setProgress(d);
      setSessions(e);
      setQuizStats(f);
    }).catch(notifyLoadError);
  }, [toast]);

  const total = Math.max((stats.mastered ?? 0) + (stats.learning ?? 0), 1);

  return (
    <AppShell>
      <div className="space-y-5">
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Metric
            icon={<IconClock />}
            label="Due Now"
            value={fsrs.dueNow ?? stats.dueNow ?? 0}
            sub="Cards waiting"
            tone="sky"
          />
          <Metric
            icon={<IconTarget />}
            label="Accuracy"
            value={formatPercent(stats.accuracy)}
            sub="Review success"
            tone="emerald"
          />
          <Metric
            icon={<AppFlameIcon />}
            label="Streak"
            value={streak.currentStreak ?? 0}
            sub="Days in a row"
            tone="amber"
          />
          <Metric
            icon={<IconChartBar />}
            label="Quiz Avg"
            value={formatPercent(quizStats.averageScore)}
            sub="Recent sessions"
            tone="blue"
          />
        </section>

        <section className="grid gap-5 xl:grid-cols-2">
          <Card>
            <CardHeader><CardTitle>Mastery</CardTitle></CardHeader>
            <CardContent className="space-y-5">
              <div>
                <div className="mb-2 flex justify-between font-display font-bold uppercase"><span>Mastered</span><span>{stats.mastered ?? 0}</span></div>
                <Progress value={((stats.mastered ?? 0) / total) * 100} />
              </div>
              <div>
                <div className="mb-2 flex justify-between font-display font-bold uppercase"><span>Learning</span><span>{stats.learning ?? 0}</span></div>
                <Progress value={((stats.learning ?? 0) / total) * 100} />
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader><CardTitle>Quiz Sessions</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {sessions.slice(0, 5).map((session) => (
                <div key={session.id} className="flex items-center justify-between rounded-xl bg-muted p-3">
                  <div>
                    <p className="font-display font-bold">Session #{session.id}</p>
                    <p className="text-sm font-semibold text-muted-foreground">{formatDateTime(session.completedAt || session.createdAt)}</p>
                  </div>
                  <Badge>{session.correctAnswers ?? 0}/{session.totalQuestions ?? 0}</Badge>
                </div>
              ))}
              {!sessions.length ? <p className="font-semibold text-muted-foreground">No quiz sessions yet.</p> : null}
            </CardContent>
          </Card>
        </section>

        <Card>
          <CardHeader><CardTitle>Word Progress</CardTitle></CardHeader>
          <CardContent className="overflow-x-auto">
            <Table>
              <thead><tr><Th>Word</Th><Th>Mastery</Th><Th>Correct</Th><Th>Incorrect</Th><Th>Next review</Th></tr></thead>
              <tbody>
                {progress.slice(0, 30).map((item) => (
                  <tr key={item.id}>
                    <Td className="font-display font-bold">{item.word?.word}</Td>
                    <Td><Badge>{masteryLabel(item.mastery)}</Badge></Td>
                    <Td>{item.correctCount ?? 0}</Td>
                    <Td>{item.incorrectCount ?? 0}</Td>
                    <Td>{formatDateTime(item.nextReviewAt)}</Td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </CardContent>
        </Card>
      </div>
    </AppShell>
  );
}

const metricTone = {
  sky: {
    card: "border-sky-200 bg-sky-50/40",
    icon: "bg-sky-100 text-[#0072a3]",
    value: "text-[#00658f]"
  },
  emerald: {
    card: "border-emerald-200 bg-emerald-50/40",
    icon: "bg-emerald-100 text-emerald-700",
    value: "text-emerald-800"
  },
  amber: {
    card: "border-amber-200 bg-amber-50/50",
    icon: "bg-amber-100 text-[#f4bf00]",
    value: "text-[#a86b00]"
  },
  blue: {
    card: "border-blue-200 bg-blue-50/40",
    icon: "bg-blue-100 text-blue-700",
    value: "text-blue-800"
  }
} as const;

function Metric({
  icon,
  label,
  value,
  sub,
  tone
}: {
  icon: React.ReactNode;
  label: string;
  value: string | number;
  sub: string;
  tone: keyof typeof metricTone;
}) {
  const styles = metricTone[tone];

  return (
    <Card className={`overflow-hidden ${styles.card}`}>
      <CardContent className="grid min-h-[136px] grid-cols-[1fr_auto] items-center gap-5 px-7 py-6">
        <div className="flex min-w-0 items-center gap-4">
          <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl [&>svg]:h-6 [&>svg]:w-6 [&>svg]:stroke-[2] ${styles.icon}`}>
            {icon}
          </div>
          <div className="min-w-0 pt-1">
            <p className="font-display text-[12px] font-bold uppercase tracking-[0.08em] text-muted-foreground">
              {label}
            </p>
            <p className="mt-3 text-sm font-semibold text-muted-foreground">{sub}</p>
          </div>
        </div>
        <div className="flex min-w-[82px] justify-end pr-1">
          <p className={`font-display text-[34px] font-bold leading-none ${styles.value}`}>{value}</p>
        </div>
      </CardContent>
    </Card>
  );
}
