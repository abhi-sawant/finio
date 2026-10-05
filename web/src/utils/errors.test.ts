import { describe, expect, it } from 'vitest';
import { getErrorMessage, isNetworkError, NETWORK_ERROR_MESSAGE } from './errors';

describe('getErrorMessage', () => {
  it('returns the message from an Error', () => {
    expect(getErrorMessage(new Error('boom'), 'fallback')).toBe('boom');
  });

  it('falls back for a non-Error value', () => {
    expect(getErrorMessage('boom', 'fallback')).toBe('fallback');
  });

  it('falls back for an Error with an empty message', () => {
    expect(getErrorMessage(new Error(''), 'fallback')).toBe('fallback');
  });

  it.each([
    'Failed to fetch',
    'NetworkError when attempting to fetch resource.',
    'Load failed',
    'Network request failed',
  ])('maps the browser network TypeError "%s" to friendly copy', (msg) => {
    expect(getErrorMessage(new TypeError(msg), 'fallback')).toBe(NETWORK_ERROR_MESSAGE);
  });

  it('keeps an unrelated TypeError message', () => {
    expect(getErrorMessage(new TypeError('x is undefined'), 'fallback')).toBe('x is undefined');
  });
});

describe('isNetworkError', () => {
  it('is false for an abort', () => {
    const err = new DOMException('The user aborted a request.', 'AbortError');
    expect(isNetworkError(err)).toBe(false);
  });

  it('is false for a server-side error message', () => {
    expect(isNetworkError(new Error('Invalid credentials'))).toBe(false);
  });
});
