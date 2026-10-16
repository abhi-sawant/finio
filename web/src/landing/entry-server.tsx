import { renderToString } from 'react-dom/server';
import { Landing } from './Landing';

/** Build-time only: the markup that ships inside `#root` so crawlers read real content. */
export function render(): string {
  return renderToString(<Landing onOpenApp={() => {}} />);
}
