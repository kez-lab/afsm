---
title: Documentation Website
updated: 2026-09-09
---

# Documentation Website

The static bilingual documentation site uses `docs/index.html`, `docs/style.css`,
`docs/js/app.js`, and bundled Markdown in `docs/js/guide-data.js`.
GitHub Pages publishes `docs/`; no framework build is required.

## Accepted reading experience

- Preserve the existing English/Korean guides and interactive examples.
- Search titles, keywords, and bundled guide bodies; show matching excerpts and
  allow Arrow Up/Down, Enter, Escape, slash, and Cmd/Ctrl+K navigation.
- Guide heading and table-of-contents links retain the guide route so copied
  URLs and reloads return to the same section. Browser titles identify the guide.
- Offer light/dark themes with a persisted explicit choice and system default.
- Keep the green accent, readable line lengths, clear dividers, responsive
  navigation, visible keyboard focus, and reduced-motion support.

## Evidence boundary

Local browser verification is separate from public deployment. The 2026-09-09 deployment of `2bd508c` passed Pages and public HTTPS verification.

## Verification (2026-09-09)

- `node scripts/check-docs.mjs` passes initial language reset and all four trace
  initializations. Before the fix it failed with `activeExampleKey is not defined`;
  simulator state now belongs to the simulator module.
- Local browser: Korean body search for `PaymentStatusUnknown`, Arrow Down/Enter
  navigation, guide titles, encoded Korean section reload, persisted dark theme,
  mobile menu, and Draft Editing → Saving with user-entered title verified.
- Desktop and 390px mobile had no horizontal page overflow. Light/dark screenshots
  were visually reviewed.
- Public deployment: [Pages run](https://github.com/kez-lab/afsm/actions/runs/34352408410)
  succeeded; all four changed web assets matched repository bytes over HTTPS.
  Public body search and keyboard navigation to Checkout passed without console
  errors. [Evidence](../../raw/verification/2026-09-09-docs-deployment/README.md).
