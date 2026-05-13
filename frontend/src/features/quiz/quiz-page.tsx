"use client";

import { IconCircleCheck, IconCircleX, IconPlayerPlayFilled } from "@tabler/icons-react";
import { useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/layout/app-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Progress } from "@/components/ui/progress";
import { api } from "@/lib/api";
import type { Word } from "@/types/api";

export function QuizPage() {
  const [categories, setCategories] = useState<string[]>([]);
  const [category, setCategory] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [count, setCount] = useState(10);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [words, setWords] = useState<Word[]>([]);
  const [index, setIndex] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [score, setScore] = useState(0);
  const [done, setDone] = useState(false);

  useEffect(() => {
    api.categories().then(setCategories).catch(() => undefined);
  }, []);

  const current = words[index];
  const choices = useMemo(() => {
    if (!current) return [];
    const wrong = words.filter((word) => word.id !== current.id).map((word) => word.translation).filter(Boolean).slice(0, 3);
    return [current.translation, ...wrong].sort(() => Math.random() - 0.5);
  }, [current, words]);

  async function start() {
    const data = await api.startQuiz({ count, category, difficulty });
    setSessionId(data.sessionId);
    setWords(data.words);
    setIndex(0);
    setSelected(null);
    setScore(0);
    setDone(false);
  }

  async function answer(choice: string) {
    if (!current || !sessionId || selected) return;
    const correct = choice === current.translation;
    setSelected(choice);
    if (correct) setScore((value) => value + 1);
    await api.submitQuizAnswer(sessionId, current.id, correct);
    await api.reviewWord(current.id, correct ? 3 : 1, 0).catch(() => undefined);
  }

  async function next() {
    if (index + 1 >= words.length) {
      if (sessionId) await api.completeQuiz(sessionId);
      setDone(true);
      return;
    }
    setIndex((value) => value + 1);
    setSelected(null);
  }

  const progress = words.length ? ((done ? words.length : index) / words.length) * 100 : 0;

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl space-y-5">
        <Card>
          <CardHeader>
            <CardTitle>Multiple Choice Quiz</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-4">
            <Select value={category} onChange={(e) => setCategory(e.target.value)}>
              <option value="">All Categories</option>
              {categories.map((cat) => <option key={cat}>{cat}</option>)}
            </Select>
            <Select value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
              <option value="">All Difficulties</option>
              <option value="BEGINNER">Beginner</option>
              <option value="INTERMEDIATE">Intermediate</option>
              <option value="ADVANCED">Advanced</option>
            </Select>
            <Select value={count} onChange={(e) => setCount(Number(e.target.value))}>
              {[5, 10, 15, 20].map((value) => <option key={value} value={value}>{value} questions</option>)}
            </Select>
            <Button onClick={start}><IconPlayerPlayFilled className="h-4 w-4" /> Start</Button>
          </CardContent>
        </Card>

        {done ? (
          <Card>
            <CardContent className="p-10 text-center">
              <IconCircleCheck className="mx-auto mb-4 h-16 w-16 text-emerald-500" />
              <h2 className="font-display text-2xl font-bold">Quiz complete</h2>
              <p className="mt-2 text-lg font-bold text-muted-foreground">Score: {score} / {words.length}</p>
              <Button className="mt-6" onClick={start}>Try Again</Button>
            </CardContent>
          </Card>
        ) : current ? (
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Question {index + 1}</CardTitle>
                <Badge>{score} correct</Badge>
              </div>
              <Progress value={progress} />
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="rounded-xl bg-primary p-8 text-center text-primary-foreground">
                <p className="font-display text-2xl font-bold">{current.word}</p>
                {current.pronunciation ? <p className="mt-3 font-mono text-sky-100">{current.pronunciation}</p> : null}
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                {choices.map((choice) => {
                  const answered = selected !== null;
                  const correct = choice === current.translation;
                  return (
                    <button
                      key={choice}
                      onClick={() => answer(choice)}
                      className={`rounded-xl border-2 bg-card p-5 text-left font-display font-bold transition hover:border-primary ${
                        answered && correct ? "border-emerald-500 bg-emerald-50 text-emerald-700" : ""
                      } ${answered && selected === choice && !correct ? "border-red-500 bg-red-50 text-red-700" : ""}`}
                    >
                      {choice}
                    </button>
                  );
                })}
              </div>
              {selected ? (
                <div className="flex items-center justify-between rounded-xl bg-muted p-4">
                  <span className="flex items-center gap-2 font-bold">
                    {selected === current.translation ? <IconCircleCheck className="text-emerald-500" /> : <IconCircleX className="text-red-500" />}
                    {selected === current.translation ? "Correct" : `Correct answer: ${current.translation}`}
                  </span>
                  <Button onClick={next}>{index + 1 >= words.length ? "Complete" : "Next"}</Button>
                </div>
              ) : null}
            </CardContent>
          </Card>
        ) : (
          <Card><CardContent className="p-10 text-center font-bold text-muted-foreground">Choose options and start a quiz.</CardContent></Card>
        )}
      </div>
    </AppShell>
  );
}
