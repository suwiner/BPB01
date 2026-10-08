/* MOYU BROWSER official site V7: light, purposeful motion and real interactions. */
(() => {
  'use strict';
  const $ = (selector, root=document) => root.querySelector(selector);
  const $$ = (selector, root=document) => Array.from(root.querySelectorAll(selector));
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  const coarse = matchMedia('(pointer: coarse)');
  const isHome = document.body.dataset.page === 'home';

  // Only hide reveal targets when IntersectionObserver is supported.
  if ('IntersectionObserver' in window && !reduced.matches) {
    document.documentElement.classList.add('motion-ready');
    const reveal = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          reveal.unobserve(entry.target);
        }
      });
    }, { threshold: .07, rootMargin: '0px 0px -18px 0px' });
    $$('[data-reveal]').forEach(el => reveal.observe(el));
  }

  // Navbar: reliable small-screen interaction and focus return.
  const menu = $('#mobile-menu');
  const toggle = $('#menu-toggle');
  const closeMenu = (restoreFocus=false) => {
    if (!menu || !toggle) return;
    menu.hidden = true;
    toggle.setAttribute('aria-expanded', 'false');
    toggle.setAttribute('aria-label', '打开导航菜单');
    if (restoreFocus) toggle.focus();
  };
  if (menu && toggle) {
    toggle.addEventListener('click', () => {
      const shouldOpen = menu.hidden;
      menu.hidden = !shouldOpen;
      toggle.setAttribute('aria-expanded', String(shouldOpen));
      toggle.setAttribute('aria-label', shouldOpen ? '关闭导航菜单' : '打开导航菜单');
      if (shouldOpen) $('a',menu)?.focus();
    });
    $$('a',menu).forEach(link => link.addEventListener('click', () => closeMenu()));
    document.addEventListener('keydown', ev => {
      if (ev.key === 'Escape' && !menu.hidden) closeMenu(true);
    });
    document.addEventListener('pointerdown', ev => {
      if (!menu.hidden && !menu.contains(ev.target) && !toggle.contains(ev.target)) closeMenu();
    });
    matchMedia('(min-width: 861px)').addEventListener('change', ev => {
      if (ev.matches) closeMenu();
    });
  }

  // Reading progress and consistent, single active chapter marker.
  const progress = $('#page-progress');
  const chapters = $$('.chapter');
  const navigation = $$('[data-nav]');
  let activeChapter = 0;
  let progressFrame = 0;
  const updateProgress = () => {
    progressFrame = 0;
    const range = Math.max(1, document.documentElement.scrollHeight - innerHeight);
    if (progress) progress.style.width = `${Math.max(0, Math.min(100, 100 * scrollY / range)).toFixed(2)}%`;
    if (!chapters.length) return;
    const midpoint = innerHeight * .44;
    let best = 0, distance = Number.MAX_VALUE;
    chapters.forEach((section, index) => {
      const r = section.getBoundingClientRect();
      const d = Math.abs((r.top + r.bottom) / 2 - midpoint);
      if (d < distance) { distance = d; best = index; }
    });
    if (best !== activeChapter || !document.body.dataset.currentChapter) {
      activeChapter = best;
      document.body.dataset.currentChapter = chapters[best]?.id || '';
      navigation.forEach(link => {
        const selected = link.dataset.nav === chapters[best]?.id;
        link.classList.toggle('active', selected);
        if (selected) link.setAttribute('aria-current','location');
        else link.removeAttribute('aria-current');
      });
    }
  };
  const requestProgress = () => {
    if (!progressFrame) progressFrame = requestAnimationFrame(updateProgress);
  };
  addEventListener('scroll', requestProgress, { passive:true });
  addEventListener('resize', requestProgress, { passive:true });
  requestProgress();

  // Accessible PageUp/PageDown navigation for readers outside controls.
  if (isHome) document.addEventListener('keydown', ev => {
    if (!['PageDown','PageUp'].includes(ev.key) || ev.altKey || ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.defaultPrevented) return;
    const t = ev.target;
    if (t instanceof Element && t.closest('a,button,input,textarea,select,summary,[contenteditable]')) return;
    if (menu && !menu.hidden) return;
    const next = Math.max(0, Math.min(chapters.length-1, activeChapter + (ev.key === 'PageDown' ? 1 : -1)));
    if (next !== activeChapter) {
      ev.preventDefault();
      chapters[next].scrollIntoView({ block:'start', behavior:reduced.matches?'auto':'smooth' });
    }
  });

  // Contained parallax. Motion never shifts headings or the page grid.
  const heroVisual = $('.hero-visual');
  const orbits = $('.hero-orbits');
  if (heroVisual && orbits && !reduced.matches && !coarse.matches) {
    let frame = 0, px = 0, py = 0;
    heroVisual.addEventListener('pointermove', ev => {
      const r = heroVisual.getBoundingClientRect();
      px = (ev.clientX - (r.left + r.width / 2)) / r.width;
      py = (ev.clientY - (r.top + r.height / 2)) / r.height;
      if (!frame) frame = requestAnimationFrame(() => {
        frame = 0;
        orbits.style.translate = `${(px * 13).toFixed(1)}px ${(py * 10).toFixed(1)}px`;
      });
    }, { passive:true });
    heroVisual.addEventListener('pointerleave', () => { orbits.style.translate = '0 0'; });
  }

  // AI demo is intentionally user-controlled (no auto-switching or distracting loops).
  const ai = [
    ['ChatGPT','对话、写作与代码辅助，让思路从这里开始。'],
    ['Gemini','从多模态理解到资料整理，探索更丰富的表达。'],
    ['Claude','长文理解与内容组织，适合深入阅读和分析。'],
    ['DeepSeek','技术探索与深入推理，发现不同的解题思路。']
  ];
  $$('[data-ai]').forEach(button => button.addEventListener('click', () => {
    const index = Number(button.dataset.ai);
    if (!ai[index]) return;
    $$('[data-ai]').forEach(tab => {
      const selected = tab === button;
      tab.classList.toggle('is-selected', selected);
      tab.setAttribute('aria-pressed', String(selected));
    });
    const title = $('#ai-model'), desc = $('#ai-description');
    if (title) title.textContent = ai[index][0];
    if (desc) desc.textContent = ai[index][1];
  }));

  // Tools highlight one single feature; aligned list is the only selector.
  const tools = [
    ['图片取色','快速识别与复制图像中的颜色，为设计提供更顺手的参考。'],
    ['长网页截图','从页面顶部到末尾，保存完整内容与工作线索。'],
    ['翻译工具','理解外语内容，减少反复复制与切换窗口的步骤。'],
    ['日报与周报','随时编辑工作记录，让日常复盘更清楚。']
  ];
  $$('[data-tool]').forEach(button => button.addEventListener('click', () => {
    const index = Number(button.dataset.tool);
    if (!tools[index]) return;
    $$('[data-tool]').forEach(item => {
      const selected = item === button;
      item.classList.toggle('is-selected', selected);
      item.setAttribute('aria-pressed', String(selected));
    });
    const display = $('.tool-display');
    if (display) display.dataset.variant = String(index);
    if ($('#tool-name')) $('#tool-name').textContent = tools[index][0];
    if ($('#tool-description')) $('#tool-description').textContent = tools[index][1];
    if ($('#tool-count')) $('#tool-count').textContent = `${String(index+1).padStart(2,'0')} / 04`;
  }));

  // Working tab panel: ARIA tab state, roving tabindex, left/right keyboard navigation.
  const workflows = [
    ['需要的资料，一目了然。','分类管理书签和标签，把每个常用网站都放回合适的位置。',['工作资料','灵感收藏','常用入口']],
    ['一周工作，清晰可见。','编辑每日工作记录，把重要进展顺手整理为周报。',['今日记录','本周计划','工作周报']],
    ['常用账号，统一管理。','主密码保护本地账号保险库，支持备份与恢复。',['本地保险库','加密备份','账号分类']]
  ];
  const workTabs = $$('[data-work]');
  const selectWork = (index, focus=false) => {
    if (!workflows[index]) return;
    workTabs.forEach((tab,i) => {
      const selected = i===index;
      tab.classList.toggle('is-selected',selected);
      tab.setAttribute('aria-selected',String(selected));
      tab.tabIndex = selected?0:-1;
    });
    const panel = $('#work-panel'), data = workflows[index];
    if (panel) panel.setAttribute('aria-labelledby',`work-tab-${index}`);
    if ($('#work-panel-title')) $('#work-panel-title').textContent = data[0];
    if ($('#work-panel-description')) $('#work-panel-description').textContent = data[1];
    if ($('.work-kicker')) $('.work-kicker').textContent = `WORKSPACE / ${String(index+1).padStart(2,'0')}`;
    if ($('#work-panel-chips')) $('#work-panel-chips').replaceChildren(...data[2].map(label => { const span=document.createElement('span');span.textContent=label;return span; }));
    if (focus) workTabs[index].focus();
  };
  workTabs.forEach((tab,index) => {
    tab.addEventListener('click',()=>selectWork(index));
    tab.addEventListener('keydown',ev => {
      if (!['ArrowLeft','ArrowRight','Home','End'].includes(ev.key)) return;
      ev.preventDefault();
      const next=ev.key==='Home'?0:ev.key==='End'?workTabs.length-1:(index+(ev.key==='ArrowRight'?1:-1)+workTabs.length)%workTabs.length;
      selectWork(next,true);
    });
  });

  // Theme preview only changes the mock page, never surprises the visitor's website theme.
  const preview = $('.personal-preview');
  $$('[data-theme]').forEach(button => button.addEventListener('click', () => {
    if (!preview) return;
    preview.dataset.themeActive = button.dataset.theme;
    $$('[data-theme]').forEach(tab => {
      const selected = tab===button;
      tab.classList.toggle('is-selected', selected);
      tab.setAttribute('aria-pressed',String(selected));
    });
  }));

  // Release eligibility uses immutable file identity + exact byte count + digest where provided.
  const expected = {
    installer: {name:'MoyuBrowser_Setup_8.32.0_x64.exe',size:180961718,sha:'3aaccf5ef714ee43ef8aa11846867af5d2e158d7499463cb579c4cc2f3ddf957'},
    portable: {name:'MoyuBrowser_Portable_8.32.0_x64.zip',size:181267581,sha:'2d8838c9769b7c9571af21d664096cdbfad0648ada61e6375fcd3ba41d42cc0c'}
  };
  const releaseButtons = $$('[data-file]');
  const releaseStatus = $('#release-status');
  const safeURL = url => typeof url==='string' && /^https:\/\/github\.com\/suwiner\/BPB01\/releases\/download\/[^\s<>]+$/.test(url);
  const tagWhitelist = ['moyu-browser-v8.32.0','v8.32.0','8.32.0'];
  const setDownloads = releases => {
    const published = Array.isArray(releases) ? releases.find(r=> !r.draft && !r.prerelease && tagWhitelist.includes(r.tag_name)) : null;
    let count = 0;
    releaseButtons.forEach(button => {
      const kind=button.dataset.file, spec=expected[kind];
      if (!spec) return;
      const asset=published?.assets?.find(a=> a.name===spec.name && a.size===spec.size && a.state==='uploaded' && safeURL(a.browser_download_url) && (!a.digest || a.digest===`sha256:${spec.sha}`));
      const available=Boolean(asset), state=$(`#${kind}-status`);
      button.disabled=!available;
      button.classList.toggle('available',available);
      button.dataset.downloadUrl=available?asset.browser_download_url:'';
      $('span',button).textContent=available?(kind==='installer'?'下载 Windows 安装版':'下载 Windows 便携版'):'尚未发布';
      if (state) state.textContent=available?'已发现匹配的正式发布附件':'正式安装附件尚未公开';
      if (available) count++;
    });
    if (releaseStatus) releaseStatus.textContent=count?`已找到 ${count} 个正式发布附件。下载后请对照 SHA256 清单。`:'当前尚未找到已核验的正式安装附件；可前往 GitHub Releases 查看发布状态。';
  };
  if (releaseButtons.length) {
    releaseButtons.forEach(button => button.addEventListener('click',()=> {
      if (button.disabled || !safeURL(button.dataset.downloadUrl)) return;
      window.location.assign(button.dataset.downloadUrl);
    }));
    const load = async () => {
      try {
        const controller=new AbortController();
        const timer=setTimeout(()=>controller.abort(),8000);
        let result;
        try {result=await fetch('https://api.github.com/repos/suwiner/BPB01/releases?per_page=50',{headers:{Accept:'application/vnd.github+json'},cache:'no-store',signal:controller.signal});}
        finally {clearTimeout(timer);}
        if (!result.ok) throw new Error(`HTTP ${result.status}`);
        setDownloads(await result.json());
      } catch (_) {
        setDownloads([]);
        if (releaseStatus) releaseStatus.textContent='暂时无法验证正式下载附件，请稍后再试或前往 GitHub Releases。';
      }
    };
    load();
  }

  // FAQs: native details, one open at a time.
  $$('.faq-items details').forEach(entry => entry.addEventListener('toggle',()=> {
    if (entry.open) $$('.faq-items details').forEach(other=> {if (other!==entry)other.open=false;});
  }));
})();