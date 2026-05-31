'use client';

import { useState } from 'react';

interface TopBarProps {
  title?: string;
}

export default function TopBar({ title }: TopBarProps) {
  const [search, setSearch] = useState('');

  return (
    <header
      className="fixed top-0 right-0 z-30 flex justify-between items-center px-10 h-20"
      style={{
        width: 'calc(100% - 18rem)',
        backgroundColor: 'rgba(251,249,249,0.92)',
        backdropFilter: 'blur(8px)',
        borderBottom: '2px solid #bdc8d2',
      }}
    >
      {/* Search */}
      <div className="flex items-center gap-6 flex-1">
        {title && (
          <h2 className="text-xl font-bold whitespace-nowrap" style={{ color: '#1b1c1c' }}>
            {title}
          </h2>
        )}
        <div className="relative w-full max-w-md">
          <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-lg"
            style={{ color: '#3e4850' }}>search</span>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search..."
            className="w-full pl-12 pr-4 py-2.5 rounded-xl text-sm font-medium outline-none transition-all"
            style={{
              border: '2px solid #bdc8d2',
              backgroundColor: '#fbf9f9',
              color: '#1b1c1c',
            }}
            onFocus={(e) => {
              (e.target as HTMLInputElement).style.borderColor = '#006590';
            }}
            onBlur={(e) => {
              (e.target as HTMLInputElement).style.borderColor = '#bdc8d2';
            }}
          />
        </div>
      </div>

      {/* Right actions */}
      <div className="flex items-center gap-2 ml-6">
        <button
          className="p-2.5 rounded-xl transition-colors"
          style={{ color: '#3e4850' }}
          onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#e9e8e7')}
          onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '')}
          title="Streak"
        >
          <span className="material-symbols-outlined">local_fire_department</span>
        </button>
        <button
          className="p-2.5 rounded-xl transition-colors"
          style={{ color: '#3e4850' }}
          onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#e9e8e7')}
          onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '')}
          title="Notifications"
        >
          <span className="material-symbols-outlined">notifications</span>
        </button>

        <div className="w-px h-8 mx-2" style={{ backgroundColor: '#bdc8d2' }} />

        <button
          className="btn-tactile px-4 py-2 rounded-xl font-bold text-sm uppercase tracking-wide"
          style={{
            backgroundColor: '#006590',
            color: '#ffffff',
            borderBottom: '4px solid #004c6e',
          }}
        >
          Quick Start AI
        </button>

        {/* Avatar */}
        <div className="ml-2 w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm cursor-pointer"
          style={{ backgroundColor: '#c8e6ff', color: '#001e2e', border: '2px solid #bdc8d2' }}>
          A
        </div>
      </div>
    </header>
  );
}
