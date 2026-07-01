# Kontiva marketing site (kept warm — not published yet)

A finished, professional single-page marketing site for **kontiva.ch**.
It is intentionally kept **out of `docs/`** so GitHub Pages does **not** serve it yet.
Nothing here is public until you deploy it.

## What's inside
- `index.html` — the full landing page (German), self-contained apart from `assets/`
- `assets/site.css`, `assets/site.js` — styling + sticky-header/scroll-reveal
- `assets/brand/` — app icon, wordmarks, favicons
- `assets/shots/` — real screenshots captured from the iPhone (iOS 26) and Android
  simulators, populated with realistic Swiss demo data

## Preview locally
```bash
python3 -m http.server 8099 --directory website
# open http://localhost:8099/
```
(The `Datenschutz` / `Privacy` links point at `./privacy-de.html` and `./privacy.html`,
which live in `docs/`; they resolve once the site is deployed into `docs/`.)

## Publish it later — two options

### A) GitHub Pages (kontiva.ch via GitHub)
1. Copy these files into `docs/` (overwriting the placeholder `index.html`):
   ```bash
   cp website/index.html docs/index.html
   cp -R website/assets docs/assets
   ```
2. Point kontiva.ch DNS (at Hostpoint) to GitHub Pages:
   ```
   A  @  185.199.108.153
   A  @  185.199.109.153
   A  @  185.199.110.153
   A  @  185.199.111.153
   CNAME  www  mrumy-dev.github.io
   ```
3. Add `docs/CNAME` containing `kontiva.ch`, commit & push, then enable
   **Enforce HTTPS** in the repo's Pages settings.

### B) Hostpoint (host directly)
Upload the contents of this `website/` folder to your Hostpoint web space
(plus `docs/privacy.html` and `docs/privacy-de.html` alongside it). No DNS change needed.
