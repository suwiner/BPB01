/**
 * Moyu Browser V6.1 - kinetic vector scenes, accessible controls and safe downloads.
 * Standalone, zero framework / third party JS and no analytics.
 */
(() => {
  'use strict';
  const root = document.documentElement;
  root.classList.add('js');
  const $ = (s, base = document) => base.querySelector(s);
  const $$ = (s, base = document) => [...base.querySelectorAll(s)];
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  const coarse = matchMedia('(pointer: coarse)');
  const isHome = document.body.dataset.page === 'home';
  const sections = isHome ? $$('.scene') : $$('main > section');
  const progress = $('#reading-progress');
  const rail = $$('.section-rail a');
  const desktop = $$('.desktop-nav [data-nav]');
  let activeIndex = 0;
  let scheduled = false;

  function activeChapter() {
    const focalPoint = innerHeight * .47;
    let best = 0;
    let bestDistance = Infinity;
    sections.forEach((part, idx) => {
      const rect = part.getBoundingClientRect();
      const distance = Math.abs((rect.top + rect.bottom) / 2 - focalPoint);
      if (distance < bestDistance) { bestDistance = distance; best = idx; }
      if (!reduced.matches && rect.bottom > 0 && rect.top < innerHeight) {
        const fraction = Math.max(-1, Math.min(1, (rect.top + rect.height * .5 - innerHeight * .5) / Math.max(rect.height, innerHeight)));
        part.style.setProperty('--py', `${(-fraction * 28).toFixed(1)}px`);
      }
    });
    activeIndex = best;
    const current = sections[best];
    if (current) {
      sections.forEach((item, idx) => item.classList.toggle('is-active', idx === best));
      rail.forEach((a, idx) => {
        const selected = idx === best;
        a.classList.toggle('active', selected);
        selected ? a.setAttribute('aria-current','location') : a.removeAttribute('aria-current');
      });
      desktop.forEach(a => {
        const selected = a.dataset.nav === current.id;
        a.classList.toggle('active', selected);
        selected ? a.setAttribute('aria-current','location') : a.removeAttribute('aria-current');
      });
      document.body.dataset.tone = current.dataset.tone || 'dark';
      document.body.classList.toggle('header-light', current.dataset.tone === 'light');
      document.body.style.setProperty('--chapter', `${best + 1}`);
    }
    if (progress) {
      const max = Math.max(1, document.documentElement.scrollHeight - innerHeight);
      progress.style.width = `${Math.max(0, Math.min(100, scrollY / max * 100)).toFixed(2)}%`;
    }
  }
  function requestChapter() {
    if (scheduled) return;
    scheduled = true;
    requestAnimationFrame(() => { scheduled = false; activeChapter(); });
  }
  window.addEventListener('scroll', requestChapter, { passive: true });
  window.addEventListener('resize', requestChapter, { passive: true });
  requestChapter();

  const revealTargets = $$('.scene, .download-hero, .download-steps, .download-faq, .step, .security-note');
  if ('IntersectionObserver' in window) {
    const revealObserver = new IntersectionObserver(entries => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-inview');
          revealObserver.unobserve(entry.target);
        }
      }
    }, { threshold: .08, rootMargin: '0px 0px -5% 0px' });
    revealTargets.forEach(item => revealObserver.observe(item));
  } else {
    revealTargets.forEach(item => item.classList.add('is-inview'));
  }

  // Page / arrow keys navigate chapters, without intercepting focused controls.
  if (isHome) {
    document.addEventListener('keydown', ev => {
      if (!['ArrowDown','ArrowUp','PageDown','PageUp'].includes(ev.key)) return;
      if (ev.defaultPrevented || ev.altKey || ev.ctrlKey || ev.metaKey || ev.shiftKey) return;
      const target = ev.target;
      if (target instanceof Element && target.closest('input,textarea,select,button,a,summary,[contenteditable], [role="tablist"]')) return;
      if ($('#mobile-menu') && !$('#mobile-menu').hidden) return;
      const delta = ['ArrowDown','PageDown'].includes(ev.key) ? 1 : -1;
      const next = Math.max(0, Math.min(sections.length - 1, activeIndex + delta));
      if (next === activeIndex) return;
      ev.preventDefault();
      sections[next].scrollIntoView({ behavior: reduced.matches ? 'instant' : 'smooth', block: 'start' });
    });
  }

  // Mobile navigation: keyboard-safe; Escape returns focus to the trigger.
  const menu = $('#mobile-menu');
  const menuToggle = $('#menu-toggle');
  function closeMenu(returnFocus = false) {
    if (!menu || !menuToggle) return;
    menu.hidden = true;
    menuToggle.setAttribute('aria-expanded', 'false');
    menuToggle.setAttribute('aria-label', '\u6253\u5f00\u83dc\u5355');
    if (returnFocus) menuToggle.focus();
  }
  if (menu && menuToggle) {
    menuToggle.addEventListener('click', () => {
      const open = menu.hidden;
      menu.hidden = !open;
      menuToggle.setAttribute('aria-expanded', String(open));
      menuToggle.setAttribute('aria-label', open ? '\u5173\u95ed\u83dc\u5355' : '\u6253\u5f00\u83dc\u5355');
      if (open) $('a', menu)?.focus();
    });
    $$('a', menu).forEach(a => a.addEventListener('click', () => closeMenu()));
    document.addEventListener('keydown', ev => { if (ev.key === 'Escape' && !menu.hidden) closeMenu(true); });
    document.addEventListener('pointerdown', ev => {
      if (!menu.hidden && !menu.contains(ev.target) && !menuToggle.contains(ev.target)) closeMenu();
    });
  }

  // Pointer-tracked vector parallax. SVG is always rendered as vectors (no filters/canvas).
  if (!reduced.matches && !coarse.matches) {
    let pointerFrame = 0;
    let tracked = null;
    const updatePointer = () => {
      pointerFrame = 0;
      if (!tracked) return;
      const { area, u, v } = tracked;
      area.style.setProperty('--px', `${(u * 24).toFixed(2)}px`);
      area.style.setProperty('--py', `${(v * 21).toFixed(2)}px`);
      if (area.id === 'hero') {
        area.style.setProperty('--mouse-x', `${(50 + u * 45).toFixed(2)}%`);
        area.style.setProperty('--mouse-y', `${(50 + v * 45).toFixed(2)}%`);
        area.style.setProperty('--tilt-x', `${(v * -3.4).toFixed(2)}deg`);
        area.style.setProperty('--tilt-y', `${(u * 4.6).toFixed(2)}deg`);
      }
    };
    $$('.scene, .download-hero').forEach(area => {
      area.addEventListener('pointermove', ev => {
        if (ev.pointerType && ev.pointerType !== 'mouse') return;
        const r = area.getBoundingClientRect();
        tracked = { area, u: Math.max(-1, Math.min(1,(ev.clientX - r.left)/r.width * 2 - 1)), v: Math.max(-1, Math.min(1,(ev.clientY - r.top)/r.height * 2 - 1)) };
        if (!pointerFrame) pointerFrame = requestAnimationFrame(updatePointer);
      }, { passive: true });
      area.addEventListener('pointerleave', () => {
        area.style.setProperty('--px','0px');
        area.style.setProperty('--py','0px');
        area.style.setProperty('--tilt-x','0deg');
        area.style.setProperty('--tilt-y','0deg');
      });
    });

    $$('.magnet').forEach(link => {
      link.addEventListener('pointermove', ev => {
        const r = link.getBoundingClientRect();
        link.style.setProperty('--mag-x', `${((ev.clientX - r.left - r.width / 2) * .09).toFixed(1)}px`);
        link.style.setProperty('--mag-y', `${((ev.clientY - r.top - r.height / 2) * .13).toFixed(1)}px`);
      });
      link.addEventListener('pointerleave', () => { link.style.setProperty('--mag-x', '0px'); link.style.setProperty('--mag-y', '0px'); });
    });
  }

  // Motion that demonstrates functionality: each button updates both content and state.
  const aiNames = ['ChatGPT','Gemini','Claude','DeepSeek'];
  const aiDescriptions = [
    '\u5e38\u7528\u5bf9\u8bdd\u4e0e\u5199\u4f5c\u52a9\u624b\uff0c\u6253\u5f00\u65b0\u7684\u601d\u8def\u3002',
    '\u591a\u6a21\u6001\u521b\u610f\u3001\u8d44\u6599\u6574\u7406\u4e0e\u9ad8\u6548\u63a2\u7d22\u3002',
    '\u957f\u6587\u9605\u8bfb\u4e0e\u903b\u8f91\u5206\u6790\uff0c\u8ba9\u601d\u8def\u66f4\u6e05\u6670\u3002',
    '\u6df1\u5ea6\u601d\u8003\u4e0e\u6280\u672f\u63a2\u7d22\uff0c\u5f00\u542f\u66f4\u591a\u53ef\u80fd\u3002'
  ];
  $$('[data-ai]').forEach(button => button.addEventListener('click', () => {
    const idx = aiNames.indexOf(button.dataset.ai);
    if (idx < 0) return;
    $$('[data-ai]').forEach(el => {
      const chosen = el === button;
      el.classList.toggle('active', chosen);
      el.setAttribute('aria-pressed', String(chosen));
    });
    if ($('#ai-current')) $('#ai-current').textContent = aiNames[idx];
    if ($('#ai-current-desc')) $('#ai-current-desc').textContent = aiDescriptions[idx];
    const core = $('.ai-core');
    if (core) core.dataset.activeModel = String(idx + 1);
  }));

  const toolData = [
    ['\u753b\u5e03\u5c3a\u5bf8','\u5c3a\u5bf8\u3001\u5355\u4f4d\u3001DPI \u4e0e\u51fa\u8840\u8bbe\u7f6e\uff0c\u4e00\u7ad9\u5f0f\u5b8c\u6210\u3002'],
    ['\u957f\u7f51\u9875\u622a\u56fe','\u4e00\u6b21\u622a\u53d6\u66f4\u591a\u5185\u5bb9\uff0c\u4fdd\u7559\u7f51\u9875\u7075\u611f\u3002'],
    ['\u56fe\u7247\u53d6\u8272','\u53d6\u5f97\u56fe\u50cf\u50cf\u7d20\u8272\u503c\uff0c\u5feb\u6377\u590d\u5236\u5230\u526a\u8d34\u677f\u3002'],
    ['\u7ffb\u8bd1','\u5feb\u901f\u67e5\u770b\u5916\u6587\u5185\u5bb9\uff0c\u51cf\u5c11\u4e0d\u5fc5\u8981\u7684\u8df3\u8f6c\u3002'],
    ['\u65e5\u62a5\u5468\u62a5','\u5de5\u4f5c\u8bb0\u5f55\u76f4\u63a5\u7f16\u8f91\uff0c\u6bcf\u4e00\u5468\u90fd\u66f4\u6709\u6761\u7406\u3002']
  ];
  $$('[data-tool]').forEach(button => button.addEventListener('click', () => {
    const idx = Number(button.dataset.tool);
    if (!Number.isInteger(idx) || !toolData[idx]) return;
    $$('[data-tool]').forEach(el => {
      const selected = el === button;
      el.classList.toggle('selected', selected);
      el.setAttribute('aria-pressed', String(selected));
    });
    $('#tool-name').textContent = toolData[idx][0];
    $('#tool-description').textContent = toolData[idx][1];
    $('#tool-number').textContent = `${String(idx + 1).padStart(2,'0')} / 05`;
    $('.tool-hub')?.style.setProperty('--tool-rotation', `${idx * 72}deg`);
  }));

  const workData = [
    ['\u4e95\u7136\u6709\u5e8f\uff0c\u4ece\u6bcf\u4e00\u9875\u5f00\u59cb\u3002','\u5206\u7c7b\u4fdd\u5b58\u4e66\u7b7e\u4e0e\u6807\u7b7e\uff0c\u8f7b\u677e\u627e\u5230\u5e38\u7528\u8d44\u6599\u3002',['\u9879\u76ee\u8d44\u6599','\u7075\u611f\u6536\u96c6','\u5e38\u7528\u7f51\u7ad9']],
    ['\u4ece\u65e5\u62a5\u5230\u5468\u62a5\uff0c\u6d41\u7545\u8854\u63a5\u3002','\u6bcf\u5929\u76f4\u63a5\u7f16\u8f91\u4e0e\u4fdd\u5b58\uff0c\u6309\u5468\u6574\u7406\u91cd\u70b9\u3002',['\u4eca\u65e5\u8bb0\u5f55','\u672c\u5468\u5de5\u4f5c','\u5f85\u529e\u6e05\u5355']],
    ['\u5e38\u7528\u8d26\u53f7\uff0c\u59a5\u5584\u4fdd\u62a4\u3002','\u4ee5\u4e3b\u5bc6\u7801\u4fdd\u62a4\u672c\u5730\u4fdd\u9669\u5e93\uff0c\u652f\u6301\u52a0\u5bc6\u5907\u4efd\u4e0e\u6062\u590d\u3002',['\u672c\u5730\u4fdd\u9669\u5e93','\u52a0\u5bc6\u5907\u4efd','\u8d26\u53f7\u5206\u7c7b']]
  ];
  const tabs = $$('[data-work]');
  function activateWork(idx, focus = false) {
    if (!workData[idx]) return;
    tabs.forEach((button, i) => {
      const chosen = i === idx;
      button.classList.toggle('active', chosen);
      button.setAttribute('aria-selected', String(chosen));
      button.tabIndex = chosen ? 0 : -1;
    });
    $('#work-current-title').textContent = workData[idx][0];
    $('#work-current-desc').textContent = workData[idx][1];
    const pills = $('#work-pills');
    if (pills) pills.replaceChildren(...workData[idx][2].map(label => {
      const span = document.createElement('span'); span.textContent = label; return span;
    }));
    if ($('#work-view')) $('#work-view').dataset.work = String(idx);
    if (focus) tabs[idx]?.focus();
  }
  tabs.forEach((button, idx) => button.addEventListener('click', () => activateWork(idx)));
  $('.work-options')?.addEventListener('keydown', ev => {
    if (!['ArrowDown','ArrowUp','ArrowLeft','ArrowRight','Home','End'].includes(ev.key)) return;
    ev.preventDefault();
    const current = Math.max(0, tabs.findIndex(button => button.classList.contains('active')));
    const idx = ev.key === 'Home' ? 0 : ev.key === 'End' ? tabs.length - 1 : (current + (['ArrowDown','ArrowRight'].includes(ev.key) ? 1 : -1) + tabs.length) % tabs.length;
    activateWork(idx, true);
  });

  const themes = { mono:['mode-mono','MONO / 01'], blue:['mode-blue','BLUE / 02'], night:['mode-night','NIGHT / 03'] };
  $$('[data-theme]').forEach(button => button.addEventListener('click', () => {
    const mode = button.dataset.theme;
    if (!themes[mode]) return;
    const preview = $('#theme-preview');
    if (preview) {
      preview.classList.remove('mode-mono','mode-blue','mode-night');
      preview.classList.add(themes[mode][0]);
      preview.dataset.theme = mode;
    }
    if ($('#theme-current')) $('#theme-current').textContent = themes[mode][1];
    $$('[data-theme]').forEach(el => { el.classList.toggle('chosen',el === button);el.setAttribute('aria-pressed',String(el === button)); });
  }));

  // GitHub release distribution: name + exact byte-size match; digest checked when exposed.
  const releaseTarget = 'https://github.com/suwiner/BPB01/releases';
  const known = {
    installer: { name: 'MoyuBrowser_Setup_8.32.0_x64.exe', size: 180961718, sha: '3aaccf5ef714ee43ef8aa11846867af5d2e158d7499463cb579c4cc2f3ddf957' },
    portable: { name: 'MoyuBrowser_Portable_8.32.0_x64.zip', size: 181267581, sha: '2d8838c9769b7c9571af21d664096cdbfad0648ada61e6375fcd3ba41d42cc0c' }
  };
  const buttons = $$('[data-file]');
  const status = $('#release-status');
  const safeURL = url => typeof url === 'string' && url.startsWith('https://github.com/suwiner/BPB01/releases/download/') && !/[\s<>]/.test(url);
  function updateDownloads(releases) {
    const published = Array.isArray(releases) ? releases.find(r => !r.draft && !r.prerelease && ['moyu-browser-v8.32.0','v8.32.0','8.32.0'].includes(r.tag_name)) : null;
    let ready = 0;
    buttons.forEach(button => {
      const kind = button.dataset.file;
      const check = known[kind];
      if (!check) return;
      const state = $(`#${kind}-status`);
      const asset = published?.assets?.find(a => a.name === check.name && a.size === check.size && a.state === 'uploaded' && safeURL(a.browser_download_url) && (!a.digest || a.digest === `sha256:${check.sha}`));
      const available = Boolean(asset);
      button.disabled = !available;
      button.classList.toggle('available', available);
      button.querySelector('span').textContent = available ? (kind === 'installer' ? '\u7acb\u5373\u4e0b\u8f7d' : '\u4e0b\u8f7d\u4fbf\u643a\u7248') : '\u6587\u4ef6\u6682\u672a\u4e0a\u67b6';
      button.dataset.downloadUrl = available ? asset.browser_download_url : '';
      if (state) state.textContent = available ? 'PUBLISHED / DOWNLOAD READY' : 'RELEASE / NOT AVAILABLE';
      if (available) ready++;
    });
    if (status) status.textContent = ready ? `\u5df2\u627e\u5230 ${ready} \u4e2a\u6b63\u5f0f\u53d1\u5e03\u9644\u4ef6\uff1b\u4e0b\u8f7d\u540e\u8bf7\u6838\u5bf9 SHA256\u3002` : '\u5b98\u65b9\u5b89\u88c5\u9644\u4ef6\u5c1a\u672a\u516c\u5f00\u53d1\u5e03\uff0c\u8bf7\u67e5\u770b GitHub Releases\u3002';
  }
  async function loadReleases() {
    if (!buttons.length) return;
    buttons.forEach(b => { b.disabled = true; b.dataset.downloadUrl = ''; });
    try {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 7500);
      let response;
      try { response = await fetch('https://api.github.com/repos/suwiner/BPB01/releases?per_page=50', { signal: controller.signal, cache:'no-store', headers: { Accept: 'application/vnd.github+json' } }); }
      finally { clearTimeout(timeout); }
      if (!response.ok) throw new Error(`Release API HTTP ${response.status}`);
      updateDownloads(await response.json());
    } catch (_) {
      updateDownloads([]);
      if (status) status.textContent = '\u6682\u65f6\u65e0\u6cd5\u6838\u5bf9\u53d1\u5e03\u4ef6\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u6216\u67e5\u770b GitHub Releases\u3002';
    }
  }
  buttons.forEach(button => button.addEventListener('click', () => {
    if (button.disabled || !safeURL(button.dataset.downloadUrl)) return;
    window.location.assign(button.dataset.downloadUrl);
  }));
  loadReleases();
  // FAQs operate natively; constrain to one open question, without intercepting keyboard.
  $$('.faq-list details').forEach(details => details.addEventListener('toggle', () => {
    if (details.open) $$('.faq-list details').forEach(other => { if (other !== details) other.open = false; });
  }));
})();