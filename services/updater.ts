import { Linking } from 'react-native'
import Constants from 'expo-constants'

// ─── CONFIGURE THESE TWO CONSTANTS ────────────────────────────────────────────
// Replace with your actual GitHub username and repository name.
// Example: if your repo URL is https://github.com/john/finio
//   GITHUB_OWNER = 'john'
//   GITHUB_REPO  = 'finio'
export const GITHUB_OWNER = 'abhi-sawant'
export const GITHUB_REPO  = 'finio'
// ───────────────────────────────────────────────────────────────────────────────

const RELEASES_API_URL = `https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`

export interface ReleaseInfo {
  /** Clean version string, e.g. "1.2.0" */
  version: string
  /** Raw tag from GitHub, e.g. "v1.2.0" */
  tagName: string
  /** URL to the release page on GitHub */
  releaseUrl: string
  /** Markdown body / release notes */
  releaseNotes: string
}

/** Parse "v1.2.3" or "1.2.3" into array [1, 2, 3] */
function parseVersion(tag: string): number[] {
  return tag
    .replace(/^v/, '')
    .split('.')
    .map((n) => parseInt(n, 10))
    .filter((n) => !isNaN(n))
}

/** Returns true when `latestTag` is strictly newer than `currentVersion`. */
function isNewer(latestTag: string, currentVersion: string): boolean {
  const latest  = parseVersion(latestTag)
  const current = parseVersion(currentVersion)
  const len = Math.max(latest.length, current.length)
  for (let i = 0; i < len; i++) {
    const l = latest[i]  ?? 0
    const c = current[i] ?? 0
    if (l > c) return true
    if (l < c) return false
  }
  return false
}

/**
 * Fetches the latest GitHub release and compares it to the installed app version.
 *
 * @returns `ReleaseInfo` when a newer version exists, or `null` when already
 *          up-to-date / owner+repo not configured / network error.
 */
export async function checkForUpdate(): Promise<ReleaseInfo | null> {
  // Guard: constants not configured yet
  if (
    GITHUB_OWNER === 'abhi-sawant' ||
    GITHUB_REPO  === 'finio'
  ) {
    return null
  }

  try {
    const res = await fetch(RELEASES_API_URL, {
      headers: {
        Accept: 'application/vnd.github+json',
        'X-GitHub-Api-Version': '2022-11-28',
      },
    })
    if (!res.ok) return null

    const data = await res.json() as {
      tag_name?: string
      html_url?: string
      body?: string
    }

    const latestTag       = data.tag_name ?? ''
    const currentVersion  = Constants.expoConfig?.version ?? '0.0.0'

    if (!latestTag || !isNewer(latestTag, currentVersion)) return null

    return {
      version:      latestTag.replace(/^v/, ''),
      tagName:      latestTag,
      releaseUrl:   data.html_url ?? `https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`,
      releaseNotes: data.body ?? '',
    }
  } catch {
    return null
  }
}

/**
 * Opens the given GitHub release page in the device browser.
 */
export function openReleasePage(url: string): void {
  Linking.openURL(url).catch(() => {
    Linking.openURL(`https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`)
  })
}
