import { useNavigate } from 'react-router';
import { ChevronRight } from 'lucide-react';
import { moreNavItems } from '@/components/layout/navItems';
import Header from '@/components/ui/header';
import Main from '@/components/ui/main';

export default function Tools() {
  const navigate = useNavigate();

  return (
    <>
      <Header innerClassName="lg:max-w-2xl">
        <h1 className="text-2xl font-bold tracking-tight">Tools</h1>
      </Header>

      <Main className="lg:max-w-2xl">
        <div className="card-elevated divide-border divide-y rounded-md">
          {moreNavItems.map(({ path, icon: Icon, label, description }) => (
            <button
              key={path}
              onClick={() => navigate(path)}
              className="hover:bg-muted/50 flex w-full items-center justify-between gap-3 p-4 text-left transition-colors"
            >
              <div className="flex min-w-0 items-center gap-3">
                <Icon size={18} className="text-muted-foreground shrink-0" />
                <div className="min-w-0">
                  <p className="text-sm font-medium">{label}</p>
                  <p className="text-muted-foreground truncate text-xs">{description}</p>
                </div>
              </div>
              <ChevronRight size={16} className="text-muted-foreground shrink-0" />
            </button>
          ))}
        </div>
      </Main>
    </>
  );
}
