# Moyu Browser V6.1 official website

A dependency-free static website with a kinetic six-scene SVG homepage and a matching Windows download center.

## Local preview

Run `python -m http.server 8080` from this directory, then open `http://localhost:8080`.

## Publish

Use `gh-pages` branch root as GitHub Pages source. Files to publish: `index.html`, `download.html`, `styles.css`, `app.js`, `assets/`, `downloads/`, `.nojekyll`, `robots.txt`.

## Download security

The published releases API is checked before enabling a download action. Release tag must be `moyu-browser-v8.32.0`, `v8.32.0`, or `8.32.0`. Filename and byte size must match the canonical build. If GitHub includes an asset SHA-256 digest it must also match. If no digest is provided, the site cannot cryptographically verify bytes before download; users should compare the published `downloads/SHA256SUMS.txt` digest after downloading. The buttons remain disabled when there is no matching official release asset.

## Motion and accessibility

Vector artwork uses inline SVG paths, not blurry bitmap backgrounds. Animations honor `prefers-reduced-motion`. Navigation, tabs, theme demos and disclosures are keyboard accessible. Mobile layout is responsive.

## Licensing

This project is a demonstration/presentation website for the Moyu Browser 8.32.0 software. Hosting large EXE/ZIP files requires an official release/file server separate from GitHub Pages.