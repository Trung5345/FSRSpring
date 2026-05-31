'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

const navItems = [
  { href: '/dashboard', icon: 'dashboard', label: 'Dashboard' },
  { href: '/vocabulary', icon: 'translate', label: 'Vocabulary' },
  { href: '/vocabulary-sets', icon: 'collections_bookmark', label: 'Vocab Sets' },
  { href: '/progress', icon: 'bar_chart', label: 'Progress & FSRS' },
  { href: '/quiz', icon: 'quiz', label: 'Quiz Stats' },
  { href: '/import', icon: 'upload_file', label: 'Import Jobs' },
  { href: '/notifications', icon: 'notifications', label: 'Notifications' },
  { href: '/content', icon: 'movie', label: 'Content' },
];

const bottomItems = [
  { href: '/settings', icon: 'settings', label: 'Settings' },
];

export default function Sidebar() {
  const pathname = usePathname();

  const isActive = (href: string) => pathname === href || pathname.startsWith(href + '/');

  return (
    <aside className="fixed left-0 top-0 h-full z-40 flex flex-col w-72"
      style={{ backgroundColor: '#f5f3f3', borderRight: '2px solid #bdc8d2' }}>
      {/* Logo */}
      <div className="p-8">
        <h1 className="text-3xl font-black" style={{ color: '#006590', letterSpacing: '-0.02em' }}>
          Linguist
        </h1>
        <p className="text-xs font-bold uppercase tracking-widest mt-1" style={{ color: '#3e4850', opacity: 0.7 }}>
          Admin Dashboard
        </p>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-4 space-y-1 overflow-y-auto custom-scrollbar">
        {navItems.map((item) => {
          const active = isActive(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className="flex items-center gap-3 px-4 py-3 mx-2 my-1 rounded-xl transition-all"
              style={active ? {
                backgroundColor: '#006590',
                color: '#ffffff',
                borderBottom: '4px solid #004c6e',
              } : {
                color: '#3e4850',
              }}
              onMouseEnter={(e) => {
                if (!active) {
                  (e.currentTarget as HTMLElement).style.backgroundColor = '#e9e8e7';
                  (e.currentTarget as HTMLElement).style.color = '#1b1c1c';
                }
              }}
              onMouseLeave={(e) => {
                if (!active) {
                  (e.currentTarget as HTMLElement).style.backgroundColor = '';
                  (e.currentTarget as HTMLElement).style.color = '#3e4850';
                }
              }}
            >
              <span className="material-symbols-outlined text-xl">{item.icon}</span>
              <span className="text-sm font-bold uppercase tracking-wide">{item.label}</span>
            </Link>
          );
        })}
      </nav>

      {/* Create button */}
      <div className="px-6 py-4">
        <Link
          href="/vocabulary"
          className="btn-tactile w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl font-bold text-sm uppercase tracking-wide"
          style={{
            backgroundColor: '#1cb0f6',
            color: '#00405d',
            borderBottom: '4px solid #004c6e',
          }}
        >
          <span className="material-symbols-outlined text-base">add</span>
          Add Word
        </Link>
      </div>

      {/* Bottom links */}
      <div className="px-4 pb-6 space-y-1" style={{ borderTop: '2px solid #bdc8d2', paddingTop: '12px' }}>
        {bottomItems.map((item) => {
          const active = isActive(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className="flex items-center gap-3 px-4 py-3 mx-2 rounded-xl transition-all"
              style={active ? {
                backgroundColor: '#006590',
                color: '#ffffff',
              } : {
                color: '#3e4850',
              }}
              onMouseEnter={(e) => {
                if (!active) {
                  (e.currentTarget as HTMLElement).style.backgroundColor = '#e9e8e7';
                }
              }}
              onMouseLeave={(e) => {
                if (!active) {
                  (e.currentTarget as HTMLElement).style.backgroundColor = '';
                }
              }}
            >
              <span className="material-symbols-outlined text-xl">{item.icon}</span>
              <span className="text-sm font-bold uppercase tracking-wide">{item.label}</span>
            </Link>
          );
        })}
      </div>
    </aside>
  );
}
