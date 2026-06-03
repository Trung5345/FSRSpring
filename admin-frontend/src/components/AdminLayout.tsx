'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Sidebar from './Sidebar';
import TopBar from './TopBar';

interface AdminLayoutProps {
  children: React.ReactNode;
  title?: string;
}

export default function AdminLayout({ children, title }: AdminLayoutProps) {
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('admin_token');
    if (!token) {
      router.replace('/login');
    }
  }, [router]);

  return (
    <div className="flex h-full min-h-screen" style={{ backgroundColor: '#fbf9f9' }}>
      <Sidebar />
      <main className="flex-1 flex flex-col" style={{ marginLeft: '18rem' }}>
        <TopBar title={title} />
        <div className="flex-1 mt-20 p-8 max-w-7xl mx-auto w-full">
          {children}
        </div>
      </main>
    </div>
  );
}
