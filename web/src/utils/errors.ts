/** Copy shown when a request never reached the server (offline, DNS, CORS, blocked). */
export const NETWORK_ERROR_MESSAGE =
  "Can't reach the Finio server. Check your connection and try again.";

/** True for a `fetch` rejection that means the request never got a response — browsers throw a
 * bare `TypeError` ("Failed to fetch", "NetworkError when attempting to fetch resource.",
 * "Load failed", "Network request failed") rather than anything typed. */
export function isNetworkError(err: unknown): boolean {
  if (!(err instanceof Error)) return false;
  if (err.name === 'AbortError') return false;
  if (err instanceof TypeError) {
    return /fetch|network|load failed/i.test(err.message);
  }
  return /^(failed to fetch|networkerror|load failed|network request failed)/i.test(err.message);
}

/** Narrows a `catch` block's `unknown` error into a user-facing message, falling back when it
 * isn't an `Error` (or has no message) — e.g. a thrown string, or a fetch rejection with no
 * `.message`. Raw network failures ("Failed to fetch") become friendly copy. */
export function getErrorMessage(err: unknown, fallback: string): string {
  if (isNetworkError(err)) return NETWORK_ERROR_MESSAGE;
  return err instanceof Error && err.message ? err.message : fallback;
}
