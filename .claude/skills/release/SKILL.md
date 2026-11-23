---
name: release
description: Prepare a Finio release (version bump, CHANGELOG section, local checks) for a given version; GitHub Actions builds and publishes it on push.
disable-model-invocation: true
argument-hint: <version, e.g. 2.0.1>
---

Prepare the release `$ARGUMENTS` for Finio, following "Releasing" in the root CLAUDE.md. Work from the repo root.

This skill only changes files and checks them. The artifacts are built and published by
`.github/workflows/release.yml` once the bump reaches `main`: it tags the commit `X.Y.Z`, builds the
web zip and the signed APK, and creates the GitHub Release with the CHANGELOG section as its notes.

## 0. Validate

- `$ARGUMENTS` must be SemVer `X.Y.Z` (strip a leading `v`). If missing or malformed, ask for it and stop.
- It must be greater than the current `VERSION`, unless `VERSION` already equals it (re-running a release whose bump was done by hand).
- If tag `X.Y.Z` already exists (`git ls-remote --tags origin X.Y.Z`), the version is already released: stop and say so, because the workflow would skip it.

## 1. Work out what changed

Take the latest changes in the working tree and since the last release tag (tags are bare `X.Y.Z`): `git status`, `git diff`, and `git log <last tag>..HEAD`. Read the diffs of user-visible changes (features, fixes, behaviour); ignore pure refactors and docs unless they matter to users or self-hosters. If the tree has changes, they are part of this release; do not commit them.

## 2. Bump and document

- Write the new version to repo-root `VERSION` (newline-terminated) and to `version` in `web/package.json` (a vitest test and the release workflow both fail if they differ). Leave `web/package-lock.json` alone.
- Add a `## X.Y.Z — <today's date, YYYY-MM-DD>` section at the top of `CHANGELOG.md`, above the previous release, using `### Added / Changed / Fixed / Removed` as needed. Write for users, in the existing voice. Mention backup-format changes and link `spec/backup-format.md`.
- **Keep that heading exact** (`## `, the bare version, a space). The workflow finds the section by it, and the section becomes the GitHub Release body, which the Android app shows in its update dialog. Everything up to the next `## ` heading is included, so the section must read well on its own.

## 3. Verify

Run, stopping on any failure and reporting it:

```bash
cd web && npm run gen:fixtures && npm test -- --run && npm run build
cd ../android && export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :core:test
```

If `gen:fixtures` modified `spec/fixtures/`, mention it in the summary. A local signed APK is not needed; CI builds and signs it, and fails if its certificate differs from the one pinned in the workflow.

## 4. Report

Summarise: the version and the changelog entry. Do NOT commit, tag, push, or create a GitHub release. Tell the user the remaining step: commit and push (or merge) to `main`. The Release workflow then publishes `X.Y.Z` with `finio-X.Y.Z.apk`, `finio-web-X.Y.Z.zip` and `SHA256SUMS.txt` attached; its run is under the repo's Actions tab.
