/* MOYU official site v11 — microinteractions and responsive concept preview. */
(() => {
  'use strict';
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)');
  const fine = window.matchMedia('(pointer: fine)');
  const moving = () => !reduce.matches && document.documentElement.dataset.motion !== 'off';
  const demoItems = [
    { name:'WORKSPACE / 01', tiles:[['✦','AI 平台','灵感探索','iris'],['▤','收藏夹','分类整理','coral'],['↗','翻译工具','即时阅读','mint'],['◫','工作记录','清晰管理','lime']] },
    { name:'AI DISCOVERY / 02', tiles:[['✳','智能对话','灵感辅助','iris'],['◈','创意设计','灵感素材','coral'],['⌘','代码创作','效率优先','mint'],['✦','知识探索','拓展思路','lime']] },
    { name:'BOOKMARKS / 03', tiles:[['▣','工作资料','常用链接','mint'],['◇','设计灵感','随手收藏','iris'],['▤','学习计划','有序分类','coral'],['☼','生活日常','轻松找回','lime']] }
  ];
  const root = document.documentElement;
  const demos = [...document.querySelectorAll('[data-mobile-demo]')];
  demos.forEach((scene, sceneIndex) => {
    const tabs = [...scene.querySelectorAll('[data-demo-tab]')];
    const panel = scene.querySelector('.demo-content');
    const title = scene.querySelector('[data-demo-label]');
    const count = scene.querySelector('[data-demo-count]');
    const target = scene.querySelector('[data-demo-tiles]');
    if (!tabs.length || !panel || !target) return;
    let selected = 0;
    let interacted = false;
    let visible = false;
    let timer = 0;
    const panelId = `moyu-demo-panel-${sceneIndex}`;
    panel.id = panelId;
    tabs.forEach((tab,i) => {
      tab.id = `moyu-demo-tab-${sceneIndex}-${i}`;
      tab.setAttribute('aria-controls', panelId);
    });
    const render = (index, focus = false) => {
      selected = ((index % demoItems.length) + demoItems.length) % demoItems.length;
      const current = demoItems[selected];
      tabs.forEach((tab,i) => {
        const active = i === selected;
        tab.classList.toggle('is-active',active);
        tab.setAttribute('aria-selected',String(active));
        tab.tabIndex = active ? 0 : -1;
      });
      panel.setAttribute('aria-labelledby', tabs[selected].id);
      if(title) title.textContent = current.name;
      if(count) count.textContent = '04 ENTRIES';
      target.replaceChildren(...current.tiles.map(([symbol,name,desc,tone]) => {
        const tile = document.createElement('div');tile.className='demo-tile';
        const icon = document.createElement('span');icon.className=`demo-tile-symbol sym-${tone}`;icon.textContent=symbol;
        const label = document.createElement('strong');label.textContent=name;
        const sub = document.createElement('small');sub.textContent=desc;
        tile.append(icon,label,sub);return tile;
      }));
      if (focus) tabs[selected].focus();
    };
    tabs.forEach((tab,i) => {
      tab.addEventListener('click',() => {interacted=true;render(i);});
      tab.addEventListener('keydown',ev => {
        if(!['ArrowRight','ArrowLeft','Home','End'].includes(ev.key))return;
        ev.preventDefault();interacted=true;
        const next = ev.key==='Home'?0:ev.key==='End'?tabs.length-1:(i+(ev.key==='ArrowRight'?1:-1)+tabs.length)%tabs.length;
        render(next,true);
      });
    });
    panel.setAttribute('aria-labelledby',tabs[0].id);
    // Automatic demonstration only when visible, motion is permitted, and visitor hasn't interacted.
    const advance = () => {
      if (!visible || !moving() || interacted || document.hidden) return;
      render(selected+1);
    };
    timer = window.setInterval(advance,5800);
    if ('IntersectionObserver' in window) {
      new IntersectionObserver(entries => {visible=entries.some(e=>e.isIntersecting);},{threshold:.22}).observe(scene);
    } else visible=true;
    scene.addEventListener('pointerenter',()=>{interacted=true;},{once:true});
    scene.addEventListener('focusin',()=>{interacted=true;},{once:true});
  });
  // Soft pointer-tracking spotlight for scene; never displace headings or the layout grid.
  if (fine.matches && !reduce.matches) {
    [...document.querySelectorAll('.moyu-app-scene')].forEach(scene => {
      let frame = 0;
      scene.addEventListener('pointermove',ev => {
        if (!moving()) return;
        if(frame) cancelAnimationFrame(frame);
        frame=requestAnimationFrame(() => {
          frame=0;
          const rect=scene.getBoundingClientRect();
          scene.style.setProperty('--pointer-x',`${(100*(ev.clientX-rect.left)/rect.width).toFixed(1)}%`);
          scene.style.setProperty('--pointer-y',`${(100*(ev.clientY-rect.top)/rect.height).toFixed(1)}%`);
        });
      },{passive:true});
      scene.addEventListener('pointerleave',() => {
        if(frame) cancelAnimationFrame(frame);
        scene.style.setProperty('--pointer-x','50%');scene.style.setProperty('--pointer-y','48%');
      });
    });
    const hero = document.querySelector('.hero-visual-v11');
    const frame = hero?.querySelector('.hero-window-frame');
    if(hero && frame) {
      let raf = 0;
      hero.addEventListener('pointermove',ev => {
        if(!moving()) return;
        if(raf)cancelAnimationFrame(raf);
        raf=requestAnimationFrame(() => {
          raf=0;
          const bounds=hero.getBoundingClientRect();
          const x=(ev.clientX-bounds.left)/bounds.width-.5;
          const y=(ev.clientY-bounds.top)/bounds.height-.5;
          frame.style.transform=`perspective(1600px) rotateX(${(2.0-y*2.4).toFixed(2)}deg) rotateY(${(x*2.7).toFixed(2)}deg) translateY(-3px)`;
        });
      },{passive:true});
      hero.addEventListener('pointerleave',() => {
        if(raf)cancelAnimationFrame(raf);
        frame.style.removeProperty('transform');
      });
    }
  }
  // Bring in benefit cards independently on scroll; preserve static cards if observers are unavailable.
  if ('IntersectionObserver' in window && !reduce.matches) {
    const rows=[...document.querySelectorAll('.hero-experience-bar>div, .android-details-v9 .android-features article')];
    if(rows.length) {
      rows.forEach(node=>node.classList.add('v11-card-pending'));
      const observer=new IntersectionObserver(entries => entries.forEach(entry => {
        if(entry.isIntersecting){entry.target.classList.add('v11-card-visible');observer.unobserve(entry.target);}
      }),{threshold:.11,rootMargin:'0px 0px -40px 0px'});
      rows.forEach((node,i)=>{node.style.setProperty('--card-delay',`${Math.min(i%3,2)*95}ms`);observer.observe(node);});
    }
  }
  // If motion is paused by the existing global toggle, disable additional decorative motion too.
  const toggle=document.getElementById('motion-toggle');
  if(toggle) {
    toggle.addEventListener('click',() => {
      document.querySelectorAll('.hero-window-frame').forEach(frame=>frame.style.removeProperty('transform'));
    });
  }
})();