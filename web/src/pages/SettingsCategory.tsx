import type { ReactNode } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router';
import { ArrowLeft, ChevronRight, FolderOpen, Tag, Wand2 } from 'lucide-react';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';
import { CloudAccountSection } from '@/components/settings/CloudAccountSection';
import { BackupSection } from '@/components/settings/BackupSection';
import { ProfileSection } from '@/components/settings/ProfileSection';
import { AppLockSection } from '@/components/settings/AppLockSection';
import { NotificationsSection } from '@/components/settings/NotificationsSection';

function OrganiseLinks() {
  const navigate = useNavigate();
  const rows = [
    { to: '/manage-categories', icon: FolderOpen, title: 'Manage categories' },
    { to: '/manage-labels', icon: Tag, title: 'Manage labels' },
    {
      to: '/category-rules',
      icon: Wand2,
      title: 'Categorization rules',
      hint: 'File transactions automatically from their note',
    },
  ];
  return (
    <div className="card-elevated divide-border divide-y rounded-md">
      {rows.map(({ to, icon: Icon, title, hint }) => (
        <button
          key={to}
          onClick={() => navigate(to)}
          className="hover:bg-muted/50 flex w-full items-center justify-between gap-3 p-4 text-left transition-colors"
        >
          <div className="flex items-center gap-3">
            <Icon size={18} className="text-muted-foreground shrink-0" />
            <div>
              <p className="text-sm font-medium">{title}</p>
              {hint && <p className="text-muted-foreground text-xs">{hint}</p>}
            </div>
          </div>
          <ChevronRight size={16} className="text-muted-foreground shrink-0" />
        </button>
      ))}
    </div>
  );
}

const categories: Record<string, { title: string; body: ReactNode }> = {
  account: { title: 'Cloud account', body: <CloudAccountSection /> },
  backup: { title: 'Backup & data', body: <BackupSection /> },
  profile: { title: 'Profile & preferences', body: <ProfileSection /> },
  security: { title: 'Security', body: <AppLockSection /> },
  notifications: { title: 'Notifications', body: <NotificationsSection /> },
  organise: { title: 'Categories, labels & rules', body: <OrganiseLinks /> },
};

export default function SettingsCategory() {
  const { category = '' } = useParams();
  const navigate = useNavigate();
  const entry = Object.hasOwn(categories, category) ? categories[category] : null;
  if (!entry) return <Navigate to="/settings" replace />;

  return (
    <>
      <Header innerClassName="lg:max-w-2xl justify-start gap-2">
        <HeaderIconButton onClick={() => navigate('/settings')} aria-label="Back to settings">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">{entry.title}</h1>
      </Header>
      <Main className="lg:max-w-2xl">{entry.body}</Main>
    </>
  );
}
