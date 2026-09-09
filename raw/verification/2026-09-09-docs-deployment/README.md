# Documentation deployment — 2026-09-09

- Deployed commit: `2bd508cde60b0b9199661ffe1089e3a1ef4ee5c5`.
- Pages workflow: https://github.com/kez-lab/afsm/actions/runs/34352408410
- Result: success; simulator regression passed; deploy job completed in 15 seconds.
- Public endpoint: https://kez-lab.org/afsm/ (HTTPS 200).
- curl downloads of index.html, style.css, js/app.js, js/trace-lab.js matched
  repository bytes exactly after deployment.
- Public browser: Korean page, theme control, populated Draft form, body search
  PaymentStatusUnknown and keyboard navigation to Checkout guide verified.
- Final URL: https://kez-lab.org/afsm/#/guide/checkout-walkthrough
- Final title: Checkout 결제 화면 가이드 · Afsm Docs. No captured console errors.

Python urllib with a verification query received HTTP 403; ordinary curl GET and
browser navigation succeeded without changing TLS or site protections.
