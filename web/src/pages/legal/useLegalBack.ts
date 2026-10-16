import { useLocation, useNavigate } from 'react-router';

/**
 * Back for a legal page. `navigate(-1)` is only safe when there is an in-app entry behind
 * this one: deep-linked (or opened in a new tab) it would leave Finio or do nothing at all.
 * React Router gives the very first history entry the key `'default'`, so that is the signal
 * to fall back to Settings, where both pages are linked from.
 */
export function useLegalBack() {
  const navigate = useNavigate();
  const location = useLocation();
  return () => {
    if (location.key === 'default') navigate('/settings', { replace: true });
    else navigate(-1);
  };
}
