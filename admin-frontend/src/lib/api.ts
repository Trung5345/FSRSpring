const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(text || `HTTP ${res.status}`);
  }
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  return res.text() as unknown as T;
}

// Auth
export const auth = {
  login: (username: string, password: string) =>
    request('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
  me: () => request('/api/user/me'),
};

// Words
export const words = {
  list: (params?: { page?: number; size?: number; category?: string; search?: string }) => {
    const q = new URLSearchParams();
    if (params?.page != null) q.set('page', String(params.page));
    if (params?.size != null) q.set('size', String(params.size));
    if (params?.category) q.set('category', params.category);
    if (params?.search) q.set('search', params.search);
    return request(`/api/words?${q}`);
  },
  get: (id: number) => request(`/api/words/${id}`),
  count: () => request<number>('/api/words/count'),
  categories: () => request<string[]>('/api/words/categories'),
  partsOfSpeech: () => request<string[]>('/api/words/parts-of-speech'),
  create: (data: unknown) =>
    request('/api/words', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: unknown) =>
    request(`/api/words/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) =>
    request(`/api/words/${id}`, { method: 'DELETE' }),
  random: () => request('/api/words/random'),
};

// Vocabulary Sets
export const sets = {
  list: () => request('/api/sets'),
  get: (id: number) => request(`/api/sets/${id}`),
  create: (data: unknown) =>
    request('/api/sets', { method: 'POST', body: JSON.stringify(data) }),
  delete: (id: number) =>
    request(`/api/sets/${id}`, { method: 'DELETE' }),
  words: (id: number) => request(`/api/sets/${id}/words`),
  addWord: (setId: number, wordId: number) =>
    request(`/api/sets/${setId}/words/${wordId}`, { method: 'POST' }),
  removeWord: (setId: number, wordId: number) =>
    request(`/api/sets/${setId}/words/${wordId}`, { method: 'DELETE' }),
};

// FSRS
export const fsrs = {
  due: () => request('/api/fsrs/due'),
  stats: () => request('/api/fsrs/stats'),
  review: (data: unknown) =>
    request('/api/fsrs/review', { method: 'POST', body: JSON.stringify(data) }),
};

// Progress
export const progress = {
  all: () => request('/api/progress'),
  stats: () => request('/api/progress/stats'),
  review: () => request('/api/progress/review'),
};

// Quiz
export const quiz = {
  stats: () => request('/api/quiz/stats'),
  recentSessions: () => request('/api/quiz/sessions/recent'),
};

// Streak
export const streak = {
  get: () => request('/api/streak'),
  checkIn: () => request('/api/streak/check-in', { method: 'POST' }),
};

// Notifications
export const notifications = {
  list: () => request('/api/notifications'),
  unreadCount: () => request<number>('/api/notifications/unread-count'),
  markRead: (id: number) =>
    request(`/api/notifications/${id}/read`, { method: 'POST' }),
  settings: () => request('/api/notifications/settings'),
  updateSettings: (data: unknown) =>
    request('/api/notifications/settings', { method: 'PUT', body: JSON.stringify(data) }),
};

// Import
export const importJobs = {
  list: () => request('/api/import/jobs'),
  get: (id: string) => request(`/api/import/jobs/${id}`),
  commit: (data: unknown) =>
    request('/api/import/words/commit', { method: 'POST', body: JSON.stringify(data) }),
};

// Content
export const content = {
  youtube: (params?: { q?: string }) => {
    const q = new URLSearchParams();
    if (params?.q) q.set('q', params.q);
    return request(`/api/content/youtube?${q}`);
  },
  news: (params?: { q?: string }) => {
    const q = new URLSearchParams();
    if (params?.q) q.set('q', params.q);
    return request(`/api/content/news?${q}`);
  },
  combined: () => request('/api/content/combined'),
};

// Topics
export const topics = {
  list: () => request('/api/topics'),
  get: (id: number) => request(`/api/topics/${id}`),
};

// Word of the day
export const wordOfDay = {
  get: () => request('/api/word-of-the-day'),
};

// Flashcards
export const flashcards = {
  list: () => request('/api/flashcards'),
  sources: () => request('/api/flashcards/sources'),
};
