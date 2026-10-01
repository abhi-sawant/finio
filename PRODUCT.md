# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Individuals in India tracking personal money day to day on their phone (installed as a PWA), then reviewing it monthly. They log spends and income, watch budgets, and keep tabs on cards, loans/EMIs, FDs/RDs, goals, and money lent or owed to people. Mobile is primary; desktop gets a sidebar layout.

## Product Purpose

Finio is a privacy-first personal finance app. It lets someone record and understand their money without handing data to a server or linking a bank. Success is a user who logs transactions habitually and trusts the numbers (balances, budgets, net worth, forecast) without friction.

## Positioning

- Offline-first: all finance data lives on the device; the app works fully signed-out with no auth gate on any route.
- Self-hostable: the optional PHP/MySQL backend (cloud backup only, optionally end-to-end encrypted) can be run by anyone on ordinary shared hosting.

## Operating Context

Used in short, frequent sessions (quick add of an expense, often via the FAB, share-target from SMS, or manifest shortcuts) and in longer monthly reviews (analytics, year in review, budgets). Installed as a PWA with notifications, optional PIN/biometric screen lock, and local-folder auto-backups.

## Capabilities and Constraints

- React 19 + TypeScript + Vite + Tailwind CSS 4 + shadcn/ui (base-nova, @base-ui/react); Zustand stores persisted to localStorage; hand-written service worker.
- INR only; financial months honour a user-set start day; transfers, splits, recurring rules, loans, deposits, goals, debts, merchants, net worth snapshots, forecast, insights.
- Manual entry plus bank-statement CSV import; no bank aggregation.
- Terminology: accounts, transactions, categories, labels, budgets, recurring, templates, goals, debts, loans, merchants.
- The lock is a screen gate, not encryption; the UI says so.

## Brand Commitments

Name: Finio. The current "Focus" look (see design.md and DESIGN.md) is the incumbent, but the user stated the current visual style is not binding and is open to change.

## Evidence on Hand

Real in-repo assets only: README.md, design.md, DESIGN.md, improvements.md, a deterministic sample dataset (`src/data/sampleData.ts`) and `dummydata.json`. No testimonials, customer logos, benchmarks, or store ratings exist; do not fabricate them.

## Product Principles

1. The user's data stays theirs: local by default, cloud strictly optional and encrypted.
2. Numbers must be trustworthy and legible before they are attractive.
3. Logging a transaction should take seconds, one-handed on a phone.
4. Indian money reality (INR, UPI, EMIs, FD/RD, salary-cycle months) is native, not a localisation afterthought.
5. Honest about limits (e.g. the lock is not encryption).

## Accessibility & Inclusion

Charts that carry information are paired with a real data table; honour prefers-reduced-motion; icon-only controls carry labels. No formal WCAG target was specified.
