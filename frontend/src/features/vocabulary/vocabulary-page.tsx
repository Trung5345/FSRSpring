"use client";

import { useSearchParams } from "next/navigation";
import { IconEdit, IconPlus, IconSearch, IconSparkles, IconTrash } from "@tabler/icons-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { AppShell } from "@/components/layout/app-shell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/toast";
import { api } from "@/lib/api";
import { difficultyLabel } from "@/lib/utils";
import type { DifficultyLevel, Word } from "@/types/api";

const emptyWord: Partial<Word> = {
  word: "",
  translation: "",
  pronunciation: "",
  category: "",
  difficulty: "INTERMEDIATE",
  example: ""
};

export function VocabularyPage() {
  const params = useSearchParams();
  const [words, setWords] = useState<Word[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState(params.get("category") || "");
  const [difficulty, setDifficulty] = useState("");
  const [cefr, setCefr] = useState("");
  const [editing, setEditing] = useState<Partial<Word> | null>(null);
  const [deleting, setDeleting] = useState<Word | null>(null);
  const { toast } = useToast();

  async function load() {
    const [allWords, cats] = await Promise.all([api.words().catch(() => []), api.categories().catch(() => [])]);
    setWords(allWords);
    setCategories(cats);
  }

  useEffect(() => {
    load().catch(() => toast("Không thể tải dữ liệu từ backend. Đang hiển thị danh sách trống.", "warning"));
  }, [toast]);

  const filtered = useMemo(() => {
    const q = search.toLowerCase();
    return words.filter((word) => {
      const matchesSearch = !q || word.word.toLowerCase().includes(q) || (word.translation || "").toLowerCase().includes(q);
      const matchesCategory = !category || word.category === category;
      const matchesDifficulty = !difficulty || word.difficulty === difficulty;
      const matchesCefr = !cefr || word.cefrLevel === cefr;
      return matchesSearch && matchesCategory && matchesDifficulty && matchesCefr;
    });
  }, [words, search, category, difficulty, cefr]);

  async function saveWord(event: FormEvent) {
    event.preventDefault();
    if (!editing?.word || !editing.translation) return;
    try {
      if (editing.id) {
        await api.updateWord(editing.id, editing);
        toast("Đã cập nhật từ vựng.", "success");
      } else {
        await api.createWord(editing);
        toast("Đã thêm từ mới.", "success");
      }
      setEditing(null);
      await load();
    } catch {
      toast("Không thể lưu từ vựng. Vui lòng thử lại.", "error");
    }
  }

  async function deleteWord() {
    if (!deleting) return;
    try {
      await api.deleteWord(deleting.id);
      setDeleting(null);
      toast("Đã xóa từ vựng.", "success");
      await load();
    } catch {
      toast("Không thể xóa từ vựng. Vui lòng thử lại.", "error");
    }
  }

  async function enrich(id: number) {
    try {
      await api.enrichWord(id);
      toast("Enrichment queued.", "success");
      await load();
    } catch {
      toast("Không thể enrich từ này.", "error");
    }
  }

  return (
    <AppShell>
      <div className="space-y-5">
        <div className="flex flex-col justify-between gap-3 md:flex-row md:items-center">
          <div>
            <p className="font-display text-sm font-bold uppercase tracking-widest text-muted-foreground">{filtered.length} từ</p>
            <h2 className="font-display text-2xl font-bold">Vocabulary Library</h2>
          </div>
          <Button onClick={() => setEditing(emptyWord)}>
            <IconPlus className="h-5 w-5" /> Add Word
          </Button>
        </div>
        <Card>
          <CardContent className="grid gap-3 pt-5 md:grid-cols-[1.3fr_1fr_1fr_1fr]">
            <label className="relative">
              <IconSearch className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
              <Input className="pl-10" placeholder="Search word or translation" value={search} onChange={(e) => setSearch(e.target.value)} />
            </label>
            <Select value={category} onChange={(e) => setCategory(e.target.value)}>
              <option value="">Tất Cả Danh Mục</option>
              {categories.map((cat) => <option key={cat} value={cat}>{cat}</option>)}
            </Select>
            <Select value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
              <option value="">All Difficulties</option>
              <option value="BEGINNER">Beginner</option>
              <option value="INTERMEDIATE">Intermediate</option>
              <option value="ADVANCED">Advanced</option>
            </Select>
            <Select value={cefr} onChange={(e) => setCefr(e.target.value)}>
              <option value="">All CEFR</option>
              {["A1", "A2", "B1", "B2", "C1", "C2"].map((level) => <option key={level}>{level}</option>)}
            </Select>
          </CardContent>
        </Card>

        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {filtered.map((word) => (
            <Card key={word.id} className="overflow-hidden">
              {word.imageUrl ? <img src={word.imageUrl} alt={word.word} className="aspect-video w-full object-cover" /> : null}
              <CardHeader>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <CardTitle>{word.word}</CardTitle>
                    {word.pronunciation ? <p className="font-mono text-sm text-primary">{word.pronunciation}</p> : null}
                  </div>
                  <Badge variant={word.difficulty === "ADVANCED" ? "danger" : word.difficulty === "INTERMEDIATE" ? "secondary" : "default"}>
                    {difficultyLabel(word.difficulty)}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="font-bold text-muted-foreground">{word.translation}</p>
                {word.example ? <p className="line-clamp-2 text-sm italic text-muted-foreground">&quot;{word.example}&quot;</p> : null}
                <div className="flex flex-wrap gap-2">
                  {word.category ? <Badge variant="muted">{word.category}</Badge> : null}
                  {word.topic ? <Badge>{word.topic.name}</Badge> : null}
                  {word.cefrLevel ? <Badge variant="secondary">{word.cefrLevel}</Badge> : null}
                  {word.enrichmentStatus ? <Badge variant={word.enrichmentStatus === "FAILED" ? "danger" : "success"}>{word.enrichmentStatus}</Badge> : null}
                </div>
                <div className="grid grid-cols-3 gap-2 border-t-2 pt-4">
                  <Button variant="outline" size="sm" onClick={() => setEditing(word)}><IconEdit className="h-4 w-4" /> Edit</Button>
                  <Button variant="outline" size="sm" onClick={() => enrich(word.id)}><IconSparkles className="h-4 w-4" /> Enrich</Button>
                  <Button variant="outline" size="sm" className="text-destructive hover:bg-red-50" onClick={() => setDeleting(word)}><IconTrash className="h-4 w-4" /> Delete</Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </section>

        {!filtered.length ? <Card><CardContent className="p-10 text-center font-bold text-muted-foreground">No words match your filters.</CardContent></Card> : null}
      </div>

      <Dialog open={!!editing} title={editing?.id ? "Sửa Từ" : "Thêm Từ Mới"} onClose={() => setEditing(null)}>
        <form className="grid gap-3" onSubmit={saveWord}>
          <Input required placeholder="Word" value={editing?.word || ""} onChange={(e) => setEditing({ ...editing, word: e.target.value })} />
          <Input required placeholder="Translation" value={editing?.translation || ""} onChange={(e) => setEditing({ ...editing, translation: e.target.value })} />
          <Input placeholder="Pronunciation" value={editing?.pronunciation || ""} onChange={(e) => setEditing({ ...editing, pronunciation: e.target.value })} />
          <Input placeholder="Category" value={editing?.category || ""} onChange={(e) => setEditing({ ...editing, category: e.target.value })} />
          <Select value={editing?.difficulty || "INTERMEDIATE"} onChange={(e) => setEditing({ ...editing, difficulty: e.target.value as DifficultyLevel })}>
            <option value="BEGINNER">Beginner</option>
            <option value="INTERMEDIATE">Intermediate</option>
            <option value="ADVANCED">Advanced</option>
          </Select>
          <Textarea placeholder="Example" value={editing?.example || ""} onChange={(e) => setEditing({ ...editing, example: e.target.value })} />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => setEditing(null)}>Cancel</Button>
            <Button type="submit">Save</Button>
          </div>
        </form>
      </Dialog>

      <Dialog open={!!deleting} title="Xóa từ vựng?" onClose={() => setDeleting(null)} className="max-w-md">
        <p className="mb-5 font-semibold text-muted-foreground">Bạn chắc chắn muốn xóa <strong>{deleting?.word}</strong>?</p>
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={() => setDeleting(null)}>Cancel</Button>
          <Button variant="destructive" onClick={deleteWord}>Delete</Button>
        </div>
      </Dialog>
    </AppShell>
  );
}
