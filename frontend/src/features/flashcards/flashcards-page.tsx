"use client";

import { IconReload, IconSearch, IconSparkles } from "@tabler/icons-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/layout/app-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";
import { useToast } from "@/components/ui/toast";
import { api } from "@/lib/api";
import type { TrustedFlashcard, UserProgress } from "@/types/api";

const ratings = [
  { value: 1, label: "Again", hint: "< 1 ngày", className: "bg-red-500 hover:bg-red-600" },
  { value: 2, label: "Hard", hint: "~2 ngày", className: "bg-orange-500 hover:bg-orange-600" },
  { value: 3, label: "Good", hint: "~5 ngày", className: "bg-emerald-500 hover:bg-emerald-600" },
  { value: 4, label: "Easy", hint: "~2 tuần", className: "bg-blue-500 hover:bg-blue-600" }
];

export function FlashcardsPage() {
  const [due, setDue] = useState<UserProgress[]>([]);
  const [index, setIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [trusted, setTrusted] = useState<TrustedFlashcard[]>([]);
  const [search, setSearch] = useState("");
  const { toast } = useToast();

  const load = useCallback(async () => {
    const [dueWords, cards] = await Promise.all([api.dueWords(30).catch(() => []), api.flashcards(search).catch(() => [])]);
    setDue(dueWords);
    setTrusted(cards);
    setIndex(0);
    setFlipped(false);
  }, [search]);

  useEffect(() => {
    load().catch(() => toast("Không thể tải flashcards từ backend. Đang hiển thị dữ liệu trống.", "warning"));
  }, [load, toast]);

  const current = due[index];
  const progress = due.length ? (index / due.length) * 100 : 0;

  async function review(rating: number) {
    if (!current?.word?.id) return;
    try {
      await api.reviewWord(current.word.id, rating, 0);
      toast(`Saved rating: ${rating}`, "success");
      setFlipped(false);
      setIndex((value) => Math.min(value + 1, due.length));
    } catch {
      toast("Không thể lưu rating. Vui lòng thử lại.", "error");
    }
  }

  async function importTrusted() {
    try {
      await api.importFlashcards("BBC Learning English", search || "general");
      toast("Trusted flashcards imported.", "success");
      await load();
    } catch {
      toast("Không thể import flashcards.", "error");
    }
  }

  async function saveTrusted(id: number) {
    try {
      await api.saveFlashcard(id);
      toast("Saved to vocabulary.", "success");
      await load();
    } catch {
      toast("Không thể lưu flashcard.", "error");
    }
  }

  const sessionDone = due.length > 0 && index >= due.length;
  const filteredTrusted = useMemo(() => trusted.slice(0, 12), [trusted]);

  return (
    <AppShell>
      <div className="grid gap-5 xl:grid-cols-[1fr_0.8fr]">
        <section className="space-y-5">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>FSRS Review</CardTitle>
                  <p className="text-sm font-semibold text-muted-foreground">{Math.min(index, due.length)} / {due.length} cards</p>
                </div>
                <Button variant="outline" onClick={load}><IconReload className="h-4 w-4" /> Reload</Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-5">
              <Progress value={progress} />
              {sessionDone ? (
                <div className="rounded-xl bg-accent p-8 text-center">
                  <h2 className="font-display text-2xl font-bold text-primary">Session complete</h2>
                  <p className="mt-2 font-semibold text-muted-foreground">No more due cards in this queue.</p>
                </div>
              ) : current ? (
                <>
                  <button
                    type="button"
                    onClick={() => setFlipped((value) => !value)}
                    className="min-h-80 w-full rounded-xl border-2 bg-card p-8 text-center shadow-lifted transition hover:border-primary"
                    aria-expanded={flipped}
                  >
                    {!flipped ? (
                      <div className="space-y-4">
                        <p className="font-display text-2xl font-bold text-primary">{current.word.word}</p>
                        <p className="font-mono text-muted-foreground">{current.word.pronunciation}</p>
                        <p className="font-bold text-muted-foreground">Click to flip</p>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        <p className="font-display text-2xl font-bold">{current.word.translation}</p>
                        {current.word.example ? <p className="text-lg italic text-muted-foreground">&quot;{current.word.example}&quot;</p> : null}
                        <div className="flex justify-center gap-2">
                          {current.word.category ? <Badge variant="muted">{current.word.category}</Badge> : null}
                          <Badge>{current.mastery || "NEW"}</Badge>
                        </div>
                      </div>
                    )}
                  </button>
                  <div className="grid gap-2 sm:grid-cols-4" role="group" aria-label="Rate your recall">
                    {ratings.map((rating) => (
                      <button
                        key={rating.value}
                        className={`rounded-xl px-3 py-4 text-white transition hover:-translate-y-0.5 ${rating.className}`}
                        onClick={() => review(rating.value)}
                      >
                        <span className="block font-display text-lg font-bold">{rating.label}</span>
                        <span className="text-xs font-semibold opacity-90">{rating.hint}</span>
                      </button>
                    ))}
                  </div>
                </>
              ) : (
                <div className="rounded-xl bg-muted p-10 text-center font-bold text-muted-foreground">No due cards right now.</div>
              )}
            </CardContent>
          </Card>
        </section>

        <section className="space-y-5">
          <Card>
            <CardHeader>
              <CardTitle>Trusted Flashcards</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex gap-2">
                <label className="relative flex-1">
                  <IconSearch className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
                  <Input className="pl-10" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search trusted cards" />
                </label>
                <Button variant="outline" onClick={load}>Search</Button>
              </div>
              <Button className="w-full" variant="secondary" onClick={importTrusted}><IconSparkles className="h-4 w-4" /> Import Trusted Set</Button>
              <div className="space-y-3">
                {filteredTrusted.map((card) => (
                  <div key={card.id} className="rounded-xl border-2 bg-card p-4">
                    <p className="font-display font-bold">{card.word || card.front}</p>
                    <p className="text-sm font-semibold text-muted-foreground">{card.definition || card.back}</p>
                    <Button className="mt-3" size="sm" variant="outline" onClick={() => saveTrusted(card.id)}>Save to vocab</Button>
                  </div>
                ))}
                {!filteredTrusted.length ? <p className="font-semibold text-muted-foreground">No trusted flashcards loaded.</p> : null}
              </div>
            </CardContent>
          </Card>
        </section>
      </div>
    </AppShell>
  );
}
