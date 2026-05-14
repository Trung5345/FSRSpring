import type {
  AppUser,
  ImportJob,
  ImportRow,
  NotificationItem,
  PageResponse,
  QuizSession,
  Topic,
  TrustedFlashcard,
  UserProgress,
  Word
} from "@/types/api";

type Query = Record<string, string | number | boolean | null | undefined>;

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function toQuery(query?: Query) {
  const params = new URLSearchParams();
  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });
  const text = params.toString();
  return text ? `?${text}` : "";
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    credentials: "include",
    headers: {
      Accept: "application/json",
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...init?.headers
    },
    ...init
  });

  if (!response.ok) {
    let message = response.statusText || "Request failed";
    try {
      const body = await response.json();
      if (body?.error) message = body.error;
    } catch {
      // Keep status text when response is not JSON.
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const api = {
  me: () => request<AppUser>("/api/user/me"),
  words: (query?: Query) => request<Word[]>(`/api/words${toQuery(query)}`),
  wordsPage: (query?: Query) => request<PageResponse<Word>>(`/api/words${toQuery(query)}`),
  word: (id: number) => request<Word>(`/api/words/${id}`),
  createWord: (word: Partial<Word>) =>
    request<Word>("/api/words", { method: "POST", body: JSON.stringify(word) }),
  updateWord: (id: number, word: Partial<Word>) =>
    request<Word>(`/api/words/${id}`, { method: "PUT", body: JSON.stringify(word) }),
  deleteWord: (id: number) => request<void>(`/api/words/${id}`, { method: "DELETE" }),
  categories: () => request<string[]>("/api/words/categories"),
  partsOfSpeech: () => request<string[]>("/api/words/parts-of-speech"),
  wordCount: () => request<{ count: number }>("/api/words/count"),
  randomWords: (limit = 1) => request<Word[]>(`/api/words/random${toQuery({ limit })}`),
  topics: () => request<Topic[]>("/api/topics"),
  fsrsStats: () => request<Record<string, number>>("/api/fsrs/stats"),
  dueWords: (limit = 20) => request<UserProgress[]>(`/api/fsrs/due${toQuery({ limit })}`),
  reviewWord: (wordId: number, rating: number, responseMs = 0) =>
    request<UserProgress>(`/api/fsrs/review${toQuery({ wordId, rating, responseMs })}`, {
      method: "POST"
    }),
  progress: () => request<UserProgress[]>("/api/progress"),
  progressStats: () => request<Record<string, number>>("/api/progress/stats"),
  reviewQueue: () => request<UserProgress[]>("/api/progress/review"),
  quizStats: () => request<Record<string, number>>("/api/quiz/stats"),
  recentQuizSessions: () => request<QuizSession[]>("/api/quiz/sessions/recent"),
  startQuiz: (query: Query) =>
    request<{ sessionId: number; words: Word[]; totalQuestions: number }>(
      `/api/quiz/start${toQuery(query)}`,
      { method: "POST" }
    ),
  submitQuizAnswer: (sessionId: number, wordId: number, correct: boolean) =>
    request<QuizSession>(`/api/quiz/session/${sessionId}/answer${toQuery({ wordId, correct })}`, {
      method: "POST"
    }),
  completeQuiz: (sessionId: number) =>
    request<QuizSession>(`/api/quiz/session/${sessionId}/complete`, { method: "POST" }),
  streak: () => request<Record<string, number>>("/api/streak"),
  notifications: () => request<NotificationItem[]>("/api/notifications"),
  unreadNotifications: () => request<{ unread: number }>("/api/notifications/unread-count"),
  markNotificationRead: (id: number) => request<void>(`/api/notifications/${id}/read`, { method: "POST" }),
  flashcards: (search?: string) => request<TrustedFlashcard[]>(`/api/flashcards${toQuery({ search })}`),
  importFlashcards: (source: string, topic: string) =>
    request<TrustedFlashcard[]>(`/api/flashcards/import${toQuery({ source, topic })}`, {
      method: "POST"
    }),
  saveFlashcard: (id: number) =>
    request<Word>(`/api/flashcards/${id}/save-to-vocab${toQuery({ difficulty: "BEGINNER", category: "Imported" })}`, {
      method: "POST"
    }),
  enrichWord: (id: number) => request<unknown>(`/api/enrichment/words/${id}`, { method: "POST" }),
  translateRows: (rows: ImportRow[]) =>
    request<{ rows: ImportRow[] }>("/api/enrichment/translate", {
      method: "POST",
      body: JSON.stringify({ rows })
    }),
  lookupDictionary: (word: string) => request<Record<string, unknown>>(`/api/dictionary/lookup/${encodeURIComponent(word)}`),
  commitImport: (payload: Record<string, unknown>) =>
    request<Record<string, unknown>>("/api/import/words/commit", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  importJobs: () => request<ImportJob[]>("/api/import/jobs")
};
