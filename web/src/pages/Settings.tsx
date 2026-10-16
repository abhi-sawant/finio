import { useNavigate, Link } from 'react-router';
import {
  ChevronRight,
  Cloud,
  DatabaseBackup,
  Bell,
  ShieldCheck,
  SlidersHorizontal,
  FolderOpen,
  type LucideIcon,
} from 'lucide-react';
import { useAuthStore } from '@/store/useAuthStore';
import { useAppLockStore } from '@/store/useAppLockStore';
import { useFinanceStore } from '@/store/useFinanceStore';
import { formatShortDate } from '@/utils/formatters';
import Header from '@/components/ui/header';
import Main from '@/components/ui/main';

interface Category {
  to: string;
  icon: LucideIcon;
  title: string;
  status: string;
}

export default function Settings() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const token = useAuthStore((s) => s.token);
  const lastBackupAt = useAuthStore((s) => s.lastBackupAt);
  const lockEnabled = useAppLockStore((s) => s.config?.enabled ?? false);
  const settings = useFinanceStore((s) => s.settings);

  const categories: Category[] = [
    {
      to: '/settings/account',
      icon: Cloud,
      title: 'Cloud account',
      status: token && user ? user.email : 'Not signed in',
    },
    {
      to: '/settings/backup',
      icon: DatabaseBackup,
      title: 'Backup & data',
      status: lastBackupAt
        ? `Last cloud backup ${formatShortDate(lastBackupAt)}`
        : 'Export, import, restore',
    },
    {
      to: '/settings/profile',
      icon: SlidersHorizontal,
      title: 'Profile & preferences',
      status: settings.userName || 'Name, theme, month start',
    },
    {
      to: '/settings/security',
      icon: ShieldCheck,
      title: 'Security',
      status: lockEnabled ? 'App lock on' : 'App lock off',
    },
    {
      to: '/settings/notifications',
      icon: Bell,
      title: 'Notifications',
      status: settings.notificationsEnabled ? 'Reminders on' : 'Reminders off',
    },
    {
      to: '/settings/organise',
      icon: FolderOpen,
      title: 'Categories, labels & rules',
      status: 'Organise how transactions are filed',
    },
  ];

  return (
    <>
      <Header innerClassName="lg:max-w-2xl">
        <h1 className="text-2xl font-bold tracking-tight">Settings</h1>
      </Header>

      <Main className="lg:max-w-2xl">
        <div className="card-elevated divide-border divide-y rounded-md">
          {categories.map(({ to, icon: Icon, title, status }) => (
            <button
              key={to}
              onClick={() => navigate(to)}
              className="hover:bg-muted/50 flex w-full items-center justify-between gap-3 p-4 text-left transition-colors"
            >
              <div className="flex min-w-0 items-center gap-3">
                <Icon size={18} className="text-muted-foreground shrink-0" />
                <div className="min-w-0">
                  <p className="text-sm font-medium">{title}</p>
                  <p className="text-muted-foreground truncate text-xs">{status}</p>
                </div>
              </div>
              <ChevronRight size={16} className="text-muted-foreground shrink-0" />
            </button>
          ))}
        </div>

        <p className="text-muted-foreground pt-2 text-center text-[11px]">
          <Link to="/privacy" className="hover:text-foreground hover:underline">
            Privacy Policy
          </Link>
          {' · '}
          <Link to="/terms" className="hover:text-foreground hover:underline">
            Terms of Service
          </Link>
        </p>
        <p className="text-muted-foreground text-center text-[11px]">
          Finio · Personal Finance · v{__APP_VERSION__}
        </p>
      </Main>
    </>
  );
}
