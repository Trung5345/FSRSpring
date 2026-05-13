"use client";

import { IconLogin2, IconLogout2, IconUser } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { AppShell } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { api } from "@/lib/api";
import { formatDateTime } from "@/lib/utils";
import type { AppUser } from "@/types/api";

export function ProfilePage() {
  const [user, setUser] = useState<AppUser | null>(null);
  const [summary, setSummary] = useState({ words: 0, streak: 0 });

  useEffect(() => {
    api.me().then(setUser).catch(() => setUser(null));
    Promise.all([api.wordCount().catch(() => ({ count: 0 })), api.streak().catch(() => ({} as Record<string, number>))]).then(([words, streak]) => {
      setSummary({ words: words.count ?? 0, streak: streak.currentStreak ?? 0 });
    });
  }, []);

  return (
    <AppShell>
      <div className="mx-auto max-w-3xl space-y-5">
        <Card>
          <CardContent className="p-8">
            <div className="flex flex-col items-center gap-4 text-center">
              {user?.avatarUrl ? <img src={user.avatarUrl} alt={user.name || user.email} className="h-24 w-24 rounded-full border-4 border-primary object-cover" /> : <div className="flex h-24 w-24 items-center justify-center rounded-full bg-accent text-primary"><IconUser className="h-12 w-12" /></div>}
              <div>
                <h2 className="font-display text-2xl font-bold">{user?.name || "Guest learner"}</h2>
                <p className="font-semibold text-muted-foreground">{user?.email || "Sign in to sync your progress"}</p>
              </div>
              {user ? (
                <Button asChildCompat="a" variant="outline"><a href="/logout"><IconLogout2 className="h-4 w-4" /> Logout</a></Button>
              ) : (
                <Button asChildCompat="a"><a href="/oauth2/authorization/google"><IconLogin2 className="h-4 w-4" /> Login with Google</a></Button>
              )}
            </div>
          </CardContent>
        </Card>

        <section className="grid gap-4 md:grid-cols-2">
          <Card><CardHeader><CardTitle>Total Words</CardTitle></CardHeader><CardContent><p className="font-display text-2xl font-bold text-primary">{summary.words}</p></CardContent></Card>
          <Card><CardHeader><CardTitle>Current Streak</CardTitle></CardHeader><CardContent><p className="font-display text-2xl font-bold text-primary">{summary.streak}</p></CardContent></Card>
        </section>

        <Card>
          <CardHeader><CardTitle>Account</CardTitle></CardHeader>
          <CardContent className="space-y-2 font-semibold text-muted-foreground">
            <p>Role: {user?.role || "Guest"}</p>
            <p>Created: {formatDateTime(user?.createdAt)}</p>
            <p>Last login: {formatDateTime(user?.lastLoginAt)}</p>
          </CardContent>
        </Card>
      </div>
    </AppShell>
  );
}
