# GitHub Pages Documentation Deployment Evidence

Date: 2026-07-18

## Scope

This record verifies the first public deployment of the bilingual Afsm
documentation hub and its user-driven example traces.

## Repository and revision

- Repository: `kez-lab/afsm`
- Branch: `main`
- Commit: `6b900e079956e6b28f59960cb6f74bb0787a021b`
- Workflow: `.github/workflows/pages.yml`
- GitHub Actions run: `29631426699`
- Deployment: `5498951966`

## GitHub evidence

- Pages was absent before configuration: REST `GET /repos/kez-lab/afsm/pages`
  returned `404 Not Found`.
- Pages was enabled with `build_type=workflow`.
- The deployment job completed successfully at `2026-07-18T04:58:04Z`.
- The deployment status changed to `success` at `2026-07-18T04:58:06Z`.
- GitHub reported the environment URL as `http://kez-lab.org/afsm/`.

Workflow run:
<https://github.com/kez-lab/afsm/actions/runs/29631426699>

## Public endpoint evidence

Checked immediately after the successful deployment:

- `http://kez-lab.org/afsm/`: `200 OK`, HTML.
- `https://kez-lab.org/afsm/`: `200 OK`, HTML.
- `https://kez-lab.github.io/afsm/`: `301` to
  `https://kez-lab.org/afsm/`.
- Public HTML last-modified timestamp: `2026-07-18T04:57:59Z`.

## Browser evidence

At <https://kez-lab.org/afsm/>:

- document title rendered as `Afsm 시작하기 · Afsm Docs`,
- selecting Draft exposed the interactive title field,
- entering `GitHub Pages live` emitted
  `TitleChanged("GitHub Pages live")`,
- visible Data became `{ title: "GitHub Pages live", errorMessage: null }`,
- saving transitioned to `Saving` and emitted
  `SaveDraft(title = "GitHub Pages live")`,
- browser console reported zero warnings or errors,
- at `390x844`, body width remained `390px`, the lab was `358px`, and the
  form/console stayed within the viewport.

## HTTPS boundary

HTTPS is publicly reachable through the configured Cloudflare/custom-domain
path. GitHub Pages still reported `https_enforced=false`; attempting to enable
it through the Pages API returned `The certificate does not exist yet`.
Therefore public HTTPS is verified, but GitHub-managed HTTP-to-HTTPS enforcement
is not yet verified.

## Evidence boundary

This proves repository workflow success and public browser availability for the
named commit. It is not controlled human first-use evidence and does not change
the production-like Android pilot status.
