import type { ReactNode } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router';
import { ArrowLeft } from 'lucide-react';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';
import { CloudAccountSection } from '@/components/settings/CloudAccountSection';
import { BackupSection } from '@/components/settings/BackupSection';
import { ProfileSection } from '@/components/settings/ProfileSection';
import { AppLockSection } from '@/components/settings/AppLockSection';
import { NotificationsSection } from '@/components/settings/NotificationsSection';

const categories: Record<string, { title: string; body: ReactNode }> = {
  account: { title: 'Cloud account', body: <CloudAccountSection /> },
  backup: { title: 'Backup & data', body: <BackupSection /> },
  profile: { title: 'Profile & preferences', body: <ProfileSection /> },
  security: { title: 'Security', body: <AppLockSection /> },
  notifications: { title: 'Notifications', body: <NotificationsSection /> },
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
