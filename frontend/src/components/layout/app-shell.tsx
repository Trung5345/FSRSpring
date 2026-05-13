"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  IconBell,
  IconLogin2,
  IconLogout2,
  IconUserFilled
} from "@tabler/icons-react";
import { useEffect, useMemo, useState } from "react";
import { AppDiamondIcon, AppFlameIcon, navigationIcons } from "@/components/icons/app-icons";
import { NotificationWidget } from "@/components/layout/notifications";
import { Button } from "@/components/ui/button";
import { api, ApiError } from "@/lib/api";
import { cn } from "@/lib/utils";
import type { AppUser } from "@/types/api";

const nav = [
  { href: "/", label: "Home", icon: navigationIcons.home },
  { href: "/vocabulary", label: "Words", icon: navigationIcons.words },
  { href: "/flashcards", label: "Flashcards", icon: navigationIcons.flashcards },
  { href: "/quiz", label: "Quiz", icon: navigationIcons.quiz },
  { href: "/progress", label: "Progress", icon: navigationIcons.progress },
  { href: "/import", label: "Import", icon: navigationIcons.import }
];

const titles: Record<string, string> = {
  "/": "Home",
  "/vocabulary": "Words",
  "/flashcards": "Flashcards",
  "/quiz": "Quiz",
  "/progress": "Progress",
  "/import": "Import",
  "/profile": "Profile"
};

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const [user, setUser] = useState<AppUser | null>(null);
  const [streak, setStreak] = useState(0);
  const [xp] = useState(0);
  const [unread, setUnread] = useState(0);

  const pageTitle = useMemo(() => titles[pathname] || "FSRSpring", [pathname]);

  useEffect(() => {
    api.me().then(setUser).catch((error) => {
      if (!(error instanceof ApiError && error.status === 401)) setUser(null);
    });
    api.streak().then((data) => setStreak(data.currentStreak ?? 0)).catch(() => undefined);
    api.unreadNotifications().then((data) => setUnread(data.unread ?? 0)).catch(() => undefined);
  }, []);

  return (
    <div className="min-h-screen bg-background text-foreground">
      <aside className="fixed left-0 top-0 z-40 hidden h-full w-64 flex-col border-r-2 border-border bg-card p-6 lg:flex">
        <Link href="/" className="mb-10 flex items-center">
          <span className="font-display text-[32px] font-bold leading-tight tracking-normal text-primary">Linguist</span>
        </Link>
        <nav className="flex flex-1 flex-col gap-2">
          {nav.map((item) => {
            const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex items-center gap-4 rounded-xl px-4 py-3 font-display text-[15px] font-bold uppercase tracking-[0.05em] transition",
                  active
                    ? "border-2 border-primary bg-accent text-primary"
                    : "text-[#404a52] hover:bg-muted hover:text-primary"
                )}
              >
                <Icon className="h-6 w-6 shrink-0" stroke={2} fill="none" />
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="border-t-2 border-border pt-6">
          {user ? (
            <div className="space-y-6">
              <Link href="/profile" className="flex items-center gap-3">
                <Avatar user={user} size="lg" />
                <span className="min-w-0">
                  <span className="block max-w-[145px] truncate font-display text-[18px] font-bold leading-tight text-foreground">{user.name || "Learner"}</span>
                  <span className="block max-w-[145px] truncate text-[15px] font-bold text-[#404a52]">{user.email}</span>
                </span>
              </Link>
              <a
                href="/logout"
                className="flex items-center justify-center gap-2 rounded-xl px-4 py-2 font-display text-[17px] font-bold uppercase tracking-[0.08em] text-[#c01f1f] transition hover:bg-red-50"
              >
                <IconLogout2 className="h-6 w-6" stroke={2} />
                Logout
              </a>
            </div>
          ) : (
            <Button asChildCompat="a" className="w-full">
              <a href="/oauth2/authorization/google">
                <IconLogin2 className="h-5 w-5" stroke={2} />
                Login
              </a>
            </Button>
          )}
        </div>
      </aside>

      <header className="sticky top-0 z-30 border-b-2 border-border bg-card px-4 py-4 lg:fixed lg:left-64 lg:right-0 lg:flex lg:h-20 lg:items-center lg:justify-between lg:px-12 lg:py-0">
        <div className="flex items-center justify-between gap-4 lg:contents">
          <h1 className="font-display text-2xl font-bold text-foreground">{pageTitle}</h1>
          <div className="flex items-center gap-6">
            <button className="flex items-center gap-2 transition hover:opacity-80" aria-label="Daily streak">
              <AppFlameIcon className="text-[34px] text-[#f4bf00]" />
              <span className="font-display text-[24px] font-bold leading-none text-[#f4bf00]">{streak}</span>
            </button>
            <button className="flex items-center gap-2 transition hover:opacity-80" aria-label="XP">
              <AppDiamondIcon className="text-[34px] text-[#1cb0f6]" />
              <span className="font-display text-[24px] font-bold leading-none text-[#1cb0f6]">{xp}</span>
            </button>
            <NotificationWidget />
            {user ? (
              <Link href="/profile" className="hidden items-center transition hover:opacity-80 sm:flex" aria-label="Profile">
                <Avatar user={user} size="sm" />
              </Link>
            ) : null}
          </div>
        </div>
        <nav className="mt-4 flex gap-2 overflow-x-auto lg:hidden">
          {nav.map((item) => {
            const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
            return (
              <Link key={item.href} href={item.href} className={cn("rounded-full px-3 py-2 font-display text-xs font-bold uppercase tracking-wide", active ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground")}>
                {item.label}
              </Link>
            );
          })}
        </nav>
      </header>

      <main className="px-4 py-6 lg:ml-64 lg:pt-28 xl:px-12">{children}</main>
    </div>
  );
}

function Avatar({ user, size }: { user: AppUser; size: "sm" | "lg" }) {
  const box = size === "lg" ? "h-12 w-12 border-[3px]" : "h-10 w-10 border-2";

  if (user.avatarUrl) {
    return (
      <img
        src={user.avatarUrl}
        alt={user.name || user.email}
        className={cn(box, "shrink-0 rounded-full border-primary object-cover")}
      />
    );
  }

  return (
    <span className={cn(box, "flex shrink-0 items-center justify-center rounded-full border-primary bg-accent text-primary")}>
      <IconUserFilled className="h-6 w-6" />
    </span>
  );
}
