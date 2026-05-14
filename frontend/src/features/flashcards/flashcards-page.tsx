"use client";

import { IconReload, IconSearch, IconSparkles } from "@tabler/icons-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { AppShellLoading } from "@/components/layout/app-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";
import { useToast } from "@/components/ui/toast";
import { ApiError, api } from "@/lib/api";
import type { TrustedFlashcard, UserProgress } from "@/types/api";

const ratings = [
  { value: 1, label: "Again", hint: "< 1 ngày", className: "bg-red-500 hover:bg-red-600" },
  { value: 2, label: "Hard", hint: "~2 ngày", className: "bg-orange-500 hover:bg-orange-600" },
  { value: 3, label: "Good", hint: "~5 ngày", className: "bg-emerald-500 hover:bg-emerald-600" },
  { value: 4, label: "Easy", hint: "~2 tuần", className: "bg-blue-500 hover:bg-blue-600" }
];

function uniqueTrustedCards(cards: TrustedFlashcard[]) {
  const seen = new Set<string>();
  return cards.filter((card) => {
    const key = (card.word || card.front || "").trim().toLowerCase();
    if (!key || seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

export function FlashcardsPage() {
  const [due, setDue] = useState<UserProgress[]>([]);
  const [index, setIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [trusted, setTrusted] = useState<TrustedFlashcard[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [importing, setImporting] = useState(false);
  const [savingId, setSavingId] = useState<number | null>(null);
  const { toast } = useToast();

  const load = useCallback(async () => {
    const [dueWords, cards] = await Promise.all([api.dueWords(30).catch(() => []), api.flashcards(search).catch(() => [])]);
    setDue(dueWords);
    setTrusted(uniqueTrustedCards(cards));
    setIndex(0);
    setFlipped(false);
    setLoading(false);
  }, [search]);

  useEffect(() => {
    load().catch(() => { setLoading(false); toast("Không thể tải flashcards từ backend. Đang hiển thị dữ liệu trống.", "warning"); });
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
    if (importing) return;
    setImporting(true);
    setTrusted([]);
    try {
      const cards = uniqueTrustedCards(await api.importFlashcards("Datamuse + Free Dictionary", search || "vocabulary"));
      setTrusted(cards);
      toast(cards.length ? "Trusted flashcards imported." : "Không có flashcard mới từ API ngoài.", cards.length ? "success" : "warning");
    } catch {
      toast("Không thể import flashcards.", "error");
    } finally {
      setImporting(false);
    }
  }

  async function saveTrusted(id: number) {
    if (savingId) return;
    setSavingId(id);
    try {
      await api.saveFlashcard(id);
      toast("Saved to vocabulary.", "success");
      setTrusted((cards) => uniqueTrustedCards(cards.filter((card) => card.id !== id)));
      const dueWords = await api.dueWords(30).catch(() => due);
      setDue(dueWords);
      setIndex(0);
      setFlipped(false);
    } catch (error) {
      const message = error instanceof ApiError && error.status === 401
        ? "Bạn cần đăng nhập để lưu flashcard vào vocab."
        : "Không thể lưu flashcard.";
      toast(message, "error");
    } finally {
      setSavingId(null);
    }
  }

  const sessionDone = due.length > 0 && index >= due.length;
  const filteredTrusted = useMemo(() => trusted.slice(0, 12), [trusted]);

  if (loading) return <AppShellLoading label="Loading flashcards..." />;

  return (
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
                    className="group w-full [perspective:1200px]"
                    aria-expanded={flipped}
                    aria-label="Flip flashcard"
                  >
                    <span className={`relative block min-h-80 rounded-xl transition-transform duration-500 [transform-style:preserve-3d] group-hover:[transform:rotateY(4deg)] ${flipped ? "[transform:rotateY(180deg)] group-hover:[transform:rotateY(180deg)]" : ""}`}>
                      <div className="absolute inset-0 flex flex-col items-center justify-center rounded-xl border-2 bg-card p-8 text-center shadow-lifted [backface-visibility:hidden] transition-colors group-hover:border-primary">
                        <p className="font-display text-3xl font-bold text-primary">{current.word.word}</p>
                        {current.word.pronunciation ? <p className="mt-4 font-mono text-muted-foreground">{current.word.pronunciation}</p> : null}
                        <p className="mt-6 font-bold text-muted-foreground">Click to flip</p>
                      </div>
                      <div className="absolute inset-0 flex flex-col items-center justify-center rounded-xl border-2 border-primary bg-card p-8 text-center shadow-lifted [backface-visibility:hidden] [transform:rotateY(180deg)]">
                        <p className="font-display text-3xl font-bold">{current.word.translation || "No translation yet"}</p>
                        {current.word.example ? <p className="text-lg italic text-muted-foreground">&quot;{current.word.example}&quot;</p> : null}
                        <div className="flex justify-center gap-2">
                          {current.word.category ? <Badge variant="muted">{current.word.category}</Badge> : null}
                          <Badge>{current.mastery || "NEW"}</Badge>
                        </div>
                      </div>
                    </span>
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
              <Button className="w-full" variant="secondary" onClick={importTrusted} disabled={importing}>
                <IconSparkles className="h-4 w-4" /> {importing ? "Importing..." : "Import Trusted Set"}
              </Button>
              <div className="space-y-3">
                {filteredTrusted.map((card) => (
                  <div key={card.id} className="rounded-xl border-2 bg-card p-4">
                    <p className="font-display font-bold">{card.word || card.front}</p>
                    <p className="text-sm font-semibold text-muted-foreground">{card.translation || card.definition || card.back}</p>
                    {card.example ? <p className="mt-2 text-sm italic text-muted-foreground">&quot;{card.example}&quot;</p> : null}
                    <Button className="mt-3" size="sm" variant="outline" onClick={() => saveTrusted(card.id)} disabled={savingId === card.id}>
                      {savingId === card.id ? "Saving..." : "Save to vocab"}
                    </Button>
                  </div>
                ))}
                {!filteredTrusted.length ? <p className="font-semibold text-muted-foreground">No trusted flashcards loaded.</p> : null}
              </div>
            </CardContent>
          </Card>
        </section>
    </div>
  );
}
