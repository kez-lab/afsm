---
title: GitHub Pages Documentation Verification 2026-07-18
updated: 2026-07-18
status: passed-public-deployment
---

# GitHub Pages Documentation Verification 2026-07-18

## Result

The bilingual Afsm documentation hub is publicly deployed at
<https://kez-lab.org/afsm/> from `main` commit `6b900e0`.

## Deployment proof

- GitHub Actions run `29631426699`: success.
- GitHub Pages deployment `5498951966`: success.
- Workflow artifact: repository `docs/` directory.
- `https://kez-lab.org/afsm/`: `200 OK` HTML.
- `https://kez-lab.github.io/afsm/`: redirects to the custom domain.

## Browser proof

- The Afsm Docs title and Korean locale rendered.
- Draft input emitted the entered `TitleChanged` value and matching Data diff.
- Save transitioned to `Saving` and emitted `SaveDraft`.
- Desktop and 390px mobile layouts had no horizontal overflow.
- Browser console reported zero warnings or errors.

## Boundary

HTTPS content is reachable, but GitHub Pages could not yet enable its own
`https_enforced` flag because the Pages API reported that the certificate does
not exist. The custom-domain HTTPS endpoint is verified; forced redirect is not.

This deployment evidence is not human first-use or production-like Android
pilot evidence.

Raw evidence:
[GitHub Pages deployment](../../raw/verification/2026-07-18-github-pages-docs/README.md).
