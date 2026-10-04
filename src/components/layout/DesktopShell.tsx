import { Outlet } from 'react-router';
import { Sidebar } from './Sidebar';

/**
 * The desktop shell for full-screen routes (forms, Tools pages): keeps the fixed Sidebar on lg+
 * so navigation never disappears after clicking a Tools item. On mobile it adds nothing — these
 * screens stay full-screen with their own back button and no tab bar or FAB.
 */
export function DesktopShell() {
  return (
    <>
      <Sidebar />
      <div className="flex min-h-0 min-w-0 flex-1 flex-col lg:pl-60">
        <Outlet />
      </div>
    </>
  );
}
