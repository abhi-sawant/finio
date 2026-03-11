const BASE_URL =
  process.env.EXPO_PUBLIC_API_URL ?? 'https://api.finio.slowatcoding.com'

interface ApiResponse<T = unknown> {
  [key: string]: T
}

async function apiFetch<T = ApiResponse>(
  path: string,
  options?: RequestInit & { token?: string }
): Promise<T> {
  const { token, ...fetchOptions } = options ?? {}

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(fetchOptions.headers as Record<string, string>),
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...fetchOptions,
    headers,
  })

  const data = await res.json().catch(() => ({}))

  if (!res.ok) {
    throw new Error((data as { error?: string }).error ?? `Request failed (${res.status})`)
  }

  return data as T
}

// ── Auth ────────────────────────────────────────────────────────────────────

export interface AuthUser {
  id: number
  name: string
  email: string
}

export interface LoginResult {
  token: string
  user: AuthUser
}

export const api = {
  register: (name: string, email: string, password: string) =>
    apiFetch('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password }),
    }),

  verifyOtp: (email: string, otp: string) =>
    apiFetch<LoginResult>('/auth/verify-otp', {
      method: 'POST',
      body: JSON.stringify({ email, otp }),
    }),

  resendOtp: (email: string) =>
    apiFetch('/auth/resend-otp', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  login: (email: string, password: string) =>
    apiFetch<LoginResult>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),

  forgotPassword: (email: string) =>
    apiFetch('/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  resetPassword: (email: string, otp: string, password: string) =>
    apiFetch('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ email, otp, password }),
    }),

  // ── Backup ────────────────────────────────────────────────────────────────

  uploadBackup: (token: string, data: object) =>
    apiFetch('/backup/upload', {
      method: 'POST',
      token,
      body: JSON.stringify(data),
    }),

  getLatestBackup: (token: string) =>
    apiFetch<Record<string, unknown>>('/backup/latest', {
      method: 'GET',
      token,
    }),
}
