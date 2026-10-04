import { useLocation, useNavigate } from 'react-router';
import { Plus } from 'lucide-react';
import { cn } from '@/lib/utils';
import { navTabs, moreNavItems, isTabActive } from './navItems';

export function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <aside className="glass-chrome fixed top-0 left-0 z-30 hidden h-dvh w-60 shrink-0 flex-col gap-1 overflow-y-auto border-r border-[var(--glass-border)] px-3 py-5 lg:flex">
      {/* Brand */}
      <div className="mb-4 flex items-center gap-2.5 px-2">
        <div className="bg-coin font-money flex h-9 w-9 items-center justify-center rounded-full text-sm">
          F
        </div>
        <span className="font-heading text-lg font-bold tracking-tight">Finio</span>
      </div>

      {/* Add transaction */}
      <button
        onClick={() => navigate('/add-transaction')}
        className="bg-grad-primary shadow-glow-primary mb-3 flex items-center justify-center gap-2 rounded-full py-3 text-sm font-semibold text-white transition-transform active:scale-[0.98]"
      >
        <Plus size={18} strokeWidth={2.4} />
        Add Transaction
      </button>

      {/* Nav */}
      <nav className="flex flex-col gap-1">
        {navTabs.map((tab) => {
          const isActive = isTabActive(location.pathname, tab.path);
          const Icon = tab.icon;
          return (
            <button
              key={tab.path}
              onClick={() => navigate(tab.path)}
              aria-current={isActive ? 'page' : undefined}
              className={cn(
                'flex items-center gap-3 rounded-full px-3 py-3 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-accent text-accent-foreground'
                  : 'text-muted-foreground hover:bg-muted/60 hover:text-foreground',
              )}
            >
              <Icon size={19} strokeWidth={isActive ? 2.4 : 2} />
              {tab.label === 'Txns' ? 'Transactions' : tab.label}
            </button>
          );
        })}
      </nav>

      {/* Tools */}
      <p className="text-muted-foreground mt-4 px-3 pb-1 text-xs font-medium">
        Tools
      </p>
      <nav className="flex flex-col gap-1">
        {moreNavItems.map((item) => {
          const isActive = location.pathname.startsWith(item.path);
          const Icon = item.icon;
          return (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              aria-current={isActive ? 'page' : undefined}
              className={cn(
                'flex items-center gap-3 rounded-full px-3 py-3 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-accent text-accent-foreground'
                  : 'text-muted-foreground hover:bg-muted/60 hover:text-foreground',
              )}
            >
              <Icon size={19} strokeWidth={isActive ? 2.4 : 2} />
              {item.label}
            </button>
          );
        })}
      </nav>
    </aside>
  );
}
