// Documentation App Shell for Afsm
(() => {
  const guideData = window.afsmGuideData || {};

  (() => {
      const root = document.documentElement;
      const body = document.body;
      const menuButton = document.querySelector('#menu-button');
      const sidebar = document.querySelector('#sidebar');
      const scrim = document.querySelector('#mobile-scrim');
      const searchInput = document.querySelector('#docs-search');
      const searchResults = document.querySelector('#search-results');
      const languageButtons = document.querySelectorAll('[data-set-language]');
      const navLinks = document.querySelectorAll('[data-nav-link]');
      const tocLinks = document.querySelectorAll('[data-toc-link]');
      const observedSections = document.querySelectorAll('#installation, #quickstart, #concepts, #android-integration, #api-reference, #guides, #examples');
      const exampleButtons = document.querySelectorAll('[data-open-example]');
      const exampleLab = document.querySelector('#example-lab');
      const traceTitle = document.querySelector('#trace-title');
      const traceSummary = document.querySelector('#trace-summary');
      const traceGuideLink = document.querySelector('#trace-guide-link');
      const traceForm = document.querySelector('#trace-form');
      const tracePhase = document.querySelector('#trace-phase');
      const traceData = document.querySelector('#trace-data');
      const traceList = document.querySelector('#trace-list');
      const traceProgress = document.querySelector('#trace-progress');
      const traceResetButton = document.querySelector('#trace-reset');

      const copyLabels = {
        en: { copy: 'Copy', copied: 'Copied' },
        ko: { copy: '복사', copied: '복사됨' },
      };

            const searchItems = [
        { en: 'Overview', ko: '개요', groupEn: 'Getting started', groupKo: '시작하기', href: '#overview', keywords: 'intro status compatibility 시작 상태 호환성' },
        { en: 'Installation', ko: '설치', groupEn: 'Getting started', groupKo: '시작하기', href: '#installation', keywords: 'maven local gradle dependency repository 설치 의존성' },
        { en: '5-minute quickstart', ko: '5분 Quickstart', groupEn: 'Getting started', groupKo: '시작하기', href: '#quickstart', keywords: 'draft machine first tutorial 머신 예제' },
        { en: 'State, Event, Command', ko: 'State, Event, Command', groupEn: 'Fundamentals', groupKo: '기본 개념', href: '#concepts', keywords: 'phase data decision transitioned handled ignored invalid 개념' },
        { en: 'Android integration', ko: 'Android 연동', groupEn: 'Fundamentals', groupKo: '기본 개념', href: '#android-integration', keywords: 'viewmodel compose stateflow command handler 연동' },
        { en: 'API quick reference', ko: 'API 빠른 참조', groupEn: 'Reference', groupKo: '레퍼런스', href: '#api-reference', keywords: 'afsmstate reducer machine host dsl reference api' },
        { en: 'Getting started tutorial', ko: '시작하기 튜토리얼', groupEn: 'Getting started', groupKo: '시작하기', href: '#/guide/getting-started', keywords: 'draft tutorial guide 시작 튜토리얼' },
        { en: 'Modeling rules', ko: '모델링 규칙', groupEn: 'Guide', groupKo: '가이드', href: '#/guide/modeling-rules', keywords: 'phase data events command stale results 모델링' },
        { en: 'Testing guide', ko: '테스트 가이드', groupEn: 'Guide', groupKo: '가이드', href: '#/guide/testing-guide', keywords: 'junit transition viewmodel test assertion 테스트' },
        { en: 'Graph generation', ko: '그래프 생성', groupEn: 'Guide', groupKo: '가이드', href: '#/guide/graph-generation', keywords: 'mermaid mmd ksp graph topology 그래프' },
        { en: 'Restoration and UI policy', ko: '복원과 UI 정책', groupEn: 'Guide', groupKo: '가이드', href: '#/guide/restoration-command-ui-policy', keywords: 'savedstatehandle restore navigation ui 복원' },
        { en: 'Sample Shop guide', ko: 'Sample Shop 가이드', groupEn: 'Guide', groupKo: '가이드', href: '#/guide/sample-shop-afsm-guide', keywords: 'sample shop app architecture 가이드' },
        { en: 'Full public API', ko: '전체 공개 API', groupEn: 'Reference', groupKo: '레퍼런스', href: '#/guide/afsm-public-api', keywords: 'signature overload config runtime public api' },
        { en: 'Release readiness', ko: '릴리스 준비 상태', groupEn: 'Reference', groupKo: '레퍼런스', href: '#/guide/release-readiness', keywords: 'compatibility baseline verification release 릴리스' },
        { en: 'Auth walkthrough', ko: 'Auth 화면 가이드', groupEn: 'Walkthroughs', groupKo: '예제 실습', href: '#/guide/auth-walkthrough', keywords: 'auth login register validation walkthrough' },
        { en: 'Checkout walkthrough', ko: 'Checkout 결제 화면 가이드', groupEn: 'Walkthroughs', groupKo: '예제 실습', href: '#/guide/checkout-walkthrough', keywords: 'checkout payment retry restoration walkthrough' },
        { en: 'Product Editor walkthrough', ko: 'Product Editor 화면 가이드', groupEn: 'Walkthroughs', groupKo: '예제 실습', href: '#/guide/product-editor-walkthrough', keywords: 'editor upload cancel review publish walkthrough' },
        { en: 'Examples', ko: '예제', groupEn: 'Learning path', groupKo: '학습 순서', href: '#examples', keywords: 'draft auth checkout product editor sample 예제' },
      ];


      const getLanguage = () => root.dataset.language || 'en';
      const scrollBehavior = () => matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth';
      const themeButton = document.querySelector('#theme-toggle');
      const systemTheme = matchMedia('(prefers-color-scheme: dark)');
      let explicitTheme = false;
      try { explicitTheme = ['light', 'dark'].includes(localStorage.getItem('afsm-docs-theme')); } catch (_) {}
      const applyTheme = theme => {
        root.dataset.theme = theme;
        themeButton.setAttribute('aria-pressed', String(theme === 'dark'));
        document.querySelector('meta[name="theme-color"]').content = theme === 'dark' ? '#111916' : '#ffffff';
      };
      applyTheme(root.dataset.theme || (systemTheme.matches ? 'dark' : 'light'));
      themeButton.addEventListener('click', () => {
        explicitTheme = true;
        applyTheme(root.dataset.theme === 'dark' ? 'light' : 'dark');
        try { localStorage.setItem('afsm-docs-theme', root.dataset.theme); } catch (_) {}
      });
      systemTheme.addEventListener('change', event => {
        if (!explicitTheme) applyTheme(event.matches ? 'dark' : 'light');
      });

      const updateLocalizedAttributes = language => {
        document.querySelectorAll('[data-aria-en]').forEach(element => {
          element.setAttribute('aria-label', language === 'ko' ? element.dataset.ariaKo : element.dataset.ariaEn);
        });
        document.querySelectorAll('[data-placeholder-en]').forEach(element => {
          element.setAttribute('placeholder', language === 'ko' ? element.dataset.placeholderKo : element.dataset.placeholderEn);
        });
      };

      const setLanguage = language => {
        const previousLanguage = root.dataset.language;
        root.dataset.language = language;
        root.lang = language;
        document.title = language === 'ko' ? 'Afsm 시작하기 · Afsm Docs' : 'Getting started · Afsm Docs';
        document.querySelector('meta[name="description"]').content = language === 'ko'
          ? 'Afsm 공식 문서: 설치, Quickstart, Android 연동, API 참조, 테스트, 그래프, 예제를 한국어와 영어로 제공합니다.'
          : 'Official Afsm documentation: installation, quickstart, Android integration, API reference, testing, graphs, and examples in English and Korean.';
        languageButtons.forEach(button => {
          button.setAttribute('aria-pressed', String(button.dataset.setLanguage === language));
        });
        updateLocalizedAttributes(language);
        renderSearch(searchInput.value);
        if (activeGuideKey) {
          renderGuide(activeGuideKey);
        }
        if (previousLanguage && previousLanguage !== language && !exampleLab.hidden) {
          resetExampleLab();
        }
        try {
          localStorage.setItem('afsm-docs-language', language);
        } catch (_) {
          // Local-file viewing can deny storage; the current session still works.
        }
      };

      const openMenu = () => {
        body.classList.add('nav-open');
        menuButton.setAttribute('aria-expanded', 'true');
      };

      const closeMenu = () => {
        body.classList.remove('nav-open');
        menuButton.setAttribute('aria-expanded', 'false');
      };

      const closeSearch = () => {
        searchResults.classList.remove('is-open');
        searchInput.setAttribute('aria-expanded', 'false');
        searchResults.replaceChildren();
      };

      function renderSearch(query) {
        const language = getLanguage();
        const normalized = query.trim().toLowerCase();
        searchResults.replaceChildren();
        if (!normalized) {
          searchResults.classList.remove('is-open');
        searchInput.setAttribute('aria-expanded', 'false');
          return;
        }

        const tokens = normalized.split(/\s+/);
        const matches = searchItems.map(item => {
          const guide = guideData[item.href.replace('#/guide/', '')];
          const content = guide ? (guide[language] || guide.en || '') : '';
          const title = `${item.en} ${item.ko} ${item.keywords}`.toLowerCase();
          const haystack = `${title} ${content.toLowerCase()}`;
          const score = tokens.every(token => haystack.includes(token))
            ? 1 + tokens.filter(token => title.includes(token)).length * 5 : 0;
          const index = content.toLowerCase().indexOf(tokens[0]);
          const excerpt = index >= 0 ? content.slice(Math.max(0, index - 45), index + 125).replace(/[`#*\n]/g, ' ').trim() : '';
          return { ...item, score, excerpt };
        }).filter(item => item.score > 0).sort((a, b) => b.score - a.score).slice(0, 8);
        document.querySelector('#search-status').textContent = language === 'ko'
          ? `${matches.length}개 검색 결과` : `${matches.length} search results`;

        if (!matches.length) {
          const empty = document.createElement('li');
          empty.className = 'search-empty';
          empty.textContent = language === 'ko' ? '일치하는 문서가 없습니다.' : 'No matching documentation.';
          searchResults.append(empty);
        } else {
          matches.forEach(item => {
            const listItem = document.createElement('li');
            const link = document.createElement('a');
            const title = document.createElement('span');
            const group = document.createElement('small');
            link.href = item.href;
            title.textContent = language === 'ko' ? item.ko : item.en;
            group.textContent = language === 'ko' ? item.groupKo : item.groupEn;
            link.append(title, group);
            if (item.excerpt) {
              const excerpt = document.createElement('p');
              excerpt.className = 'search-excerpt';
              excerpt.textContent = item.excerpt;
              link.append(excerpt);
            }
            link.addEventListener('click', () => {
              closeSearch();
              closeMenu();
            });
            listItem.append(link);
            searchResults.append(listItem);
          });
        }
        searchResults.classList.add('is-open');
        searchInput.setAttribute('aria-expanded', 'true');
      }

      menuButton.addEventListener('click', () => {
        body.classList.contains('nav-open') ? closeMenu() : openMenu();
      });
      scrim.addEventListener('click', closeMenu);
      sidebar.querySelectorAll('a').forEach(link => link.addEventListener('click', closeMenu));

      languageButtons.forEach(button => {
        button.addEventListener('click', () => setLanguage(button.dataset.setLanguage));
      });

      searchInput.addEventListener('input', () => renderSearch(searchInput.value));
      searchInput.addEventListener('focus', () => renderSearch(searchInput.value));
      document.addEventListener('click', event => {
        if (!event.target.closest('.search-wrap')) closeSearch();
      });
      document.addEventListener('keydown', event => {
        const target = event.target;
        const isTyping = target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement || target.isContentEditable;
        if ((event.key === '/' && !isTyping) || ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k')) {
          event.preventDefault();
          searchInput.focus();
        }
        if (searchResults.classList.contains('is-open') && event.target.closest('.search-wrap')) {
          const links = [...searchResults.querySelectorAll('a')];
          const current = links.indexOf(document.activeElement);
          if ((event.key === 'ArrowDown' || event.key === 'ArrowUp') && links.length) {
            event.preventDefault();
            const next = current < 0 ? (event.key === 'ArrowDown' ? 0 : links.length - 1)
              : (current + (event.key === 'ArrowDown' ? 1 : -1) + links.length) % links.length;
            links[next].focus();
          }
          if (event.key === 'Enter' && document.activeElement === searchInput && links.length) {
            event.preventDefault();
            links[0].click();
          }
        }
        if (event.key === 'Escape') {
          const searching = event.target.closest('.search-wrap');
          closeSearch();
          closeMenu();
          if (searching) searchInput.focus();
        }
      });

      document.querySelectorAll('.copy-button').forEach(button => {
        button.addEventListener('click', async () => {
          const language = getLanguage();
          const target = document.querySelector(`#${button.dataset.copyTarget}`);
          try {
            await navigator.clipboard.writeText(target.textContent);
            button.querySelector(`[data-lang="${language}"]`).textContent = copyLabels[language].copied;
            window.setTimeout(() => {
              button.querySelector(`[data-lang="${language}"]`).textContent = copyLabels[language].copy;
            }, 1200);
          } catch (_) {
            const selection = window.getSelection();
            const range = document.createRange();
            range.selectNodeContents(target);
            selection.removeAllRanges();
            selection.addRange(range);
          }
        });
      });

  let activeGuideKey = null;

      const hubArticle = document.querySelector('#hub-article');
      const guideArticle = document.querySelector('#guide-article');
      const guideBreadcrumbCategory = document.querySelector('#guide-breadcrumb-category');
      const guideBreadcrumbTitle = document.querySelector('#guide-breadcrumb-title');
      const guideTitle = document.querySelector('#guide-title');
      const guideLead = document.querySelector('#guide-lead');
      const guideGithubLink = document.querySelector('#guide-github-link');
      const guideBody = document.querySelector('#guide-body');
      const guidePagination = document.querySelector('#guide-pagination');
      const onThisPageToc = document.querySelector('.on-this-page');
      const tocList = document.querySelector('.on-this-page .toc-list');
      const hubTocHtml = tocList ? tocList.innerHTML : '';

      function escapeHtml(str) {
        return str.replace(/&/g, '&amp;')
                  .replace(/</g, '&lt;')
                  .replace(/>/g, '&gt;')
                  .replace(/"/g, '&quot;')
                  .replace(/'/g, '&#039;');
      }

      function inlineMarkdown(str) {
        let s = str;
        s = s.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        s = s.replace(/\*(.*?)\*/g, '<em>$1</em>');
        s = s.replace(/`([^`]+)`/g, '<code>$1</code>');
        s = s.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (match, label, href) => {
          if (href.endsWith('.md') && !href.startsWith('http')) {
            const guideKey = href.replace(/\.md$/, '').replace(/\.ko$/, '').replace(/^.*\//, '');
            return `<a href="#/guide/${guideKey}">${label}</a>`;
          }
          if (href.startsWith('http')) {
            return `<a href="${href}" target="_blank" rel="noopener noreferrer">${label} ↗</a>`;
          }
          return `<a href="${href}">${label}</a>`;
        });
        return s;
      }

      function renderMarkdownToHtml(mdText) {
        if (!mdText) return '';
        let text = mdText.replace(/\r\n/g, '\n');
        text = text.replace(/^#\s+[^\n]+\n+/, '');

        const codeBlocks = [];
        text = text.replace(/```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, (match, lang, code) => {
          const placeholder = `%%CODE_BLOCK_${codeBlocks.length}%%`;
          codeBlocks.push({ lang: lang || 'text', code });
          return placeholder;
        });

        const tables = [];
        text = text.replace(/((?:\|[^\n]+\|\n?)+)/g, (match, tableContent) => {
          const lines = tableContent.trim().split('\n');
          if (lines.length >= 2 && lines[1].includes('|---')) {
            const placeholder = `%%TABLE_${tables.length}%%`;
            tables.push(lines);
            return placeholder;
          }
          return match;
        });

        text = text.replace(/^(#{2,4})\s+(.+)$/gm, (match, hashes, headingText) => {
          const level = hashes.length;
          const cleanText = headingText.replace(/\[([^\]]+)\]\([^)]+\)/g, '$1').replace(/`([^`]+)`/g, '$1');
          const slug = cleanText.toLowerCase().replace(/[^a-z0-9가-힣]+/g, '-').replace(/(^-|-$)/g, '');
          return `<h${level} id="${slug}">${inlineMarkdown(headingText)}<a class="anchor-link" href="#/guide/${activeGuideKey}#${slug}" aria-label="${escapeHtml(cleanText)}"></a></h${level}>`;
        });

        text = text.replace(/^>\s*\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]\s*\n((?:>.*(?:\n|$))*)/gim, (match, type, content) => {
          const alertType = type.toLowerCase();
          const cleanContent = content.replace(/^>\s?/gm, '').trim();
          const title = type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
          return `<div class="callout callout-${alertType}"><strong>${title}</strong><span>${inlineMarkdown(cleanContent)}</span></div>`;
        });

        text = text.replace(/((?:^>.*(?:\n|$))+)/gm, (match) => {
          const cleanContent = match.replace(/^>\s?/gm, '').trim();
          return `<blockquote><p>${inlineMarkdown(cleanContent)}</p></blockquote>`;
        });

        text = text.replace(/^---$/gm, '<hr class="doc-divider">');

        text = text.replace(/((?:^(?:-|\*|\d+\.)\s+.+(?:\n|$))+)/gm, (match) => {
          const lines = match.trim().split('\n');
          const isOrdered = /^\d+\./.test(lines[0]);
          const tag = isOrdered ? 'ol' : 'ul';
          const items = lines.map(line => {
            const itemContent = line.replace(/^(?:-|\*|\d+\.)\s+/, '');
            return `<li>${inlineMarkdown(itemContent)}</li>`;
          }).join('');
          return `<${tag}>${items}</${tag}>`;
        });

        const blocks = text.split(/\n\s*\n/);
        text = blocks.map(block => {
          const trimmed = block.trim();
          if (!trimmed) return '';
          if (trimmed.startsWith('<h') || trimmed.startsWith('<div') || trimmed.startsWith('<blockquote') ||
              trimmed.startsWith('<ul') || trimmed.startsWith('<ol') || trimmed.startsWith('<hr') ||
              trimmed.startsWith('%%CODE_BLOCK_') || trimmed.startsWith('%%TABLE_')) {
            return trimmed;
          }
          return `<p>${inlineMarkdown(trimmed)}</p>`;
        }).join('\n\n');

        text = text.replace(/%%CODE_BLOCK_(\d+)%%/g, (match, idx) => {
          const item = codeBlocks[Number(idx)];
          const escaped = escapeHtml(item.code);
          const codeId = `code-block-${Math.random().toString(36).substr(2, 9)}`;
          return `<div class="code-block">
            <div class="code-head">
              <span>${escapeHtml(item.lang)}</span>
              <button class="copy-button" type="button" data-copy-target="${codeId}">
                <span data-lang="en">Copy</span><span data-lang="ko">복사</span>
              </button>
            </div>
            <pre><code id="${codeId}">${escaped}</code></pre>
          </div>`;
        });

        text = text.replace(/%%TABLE_(\d+)%%/g, (match, idx) => {
          const lines = tables[Number(idx)];
          const headerCols = lines[0].split('|').slice(1, -1).map(c => c.trim());
          const rowLines = lines.slice(2);
          const thead = `<thead><tr>${headerCols.map(c => `<th>${inlineMarkdown(c)}</th>`).join('')}</tr></thead>`;
          const tbody = `<tbody>${rowLines.map(row => {
            const cols = row.split('|').slice(1, -1).map(c => c.trim());
            return `<tr>${cols.map(c => `<td>${inlineMarkdown(c)}</td>`).join('')}</tr>`;
          }).join('')}</tbody>`;
          return `<div class="table-wrap"><table class="doc-table">${thead}${tbody}</table></div>`;
        });

        return text;
      }

      function updateGuideToc() {
        if (!tocList || !guideBody) return;
        const headings = guideBody.querySelectorAll('h2, h3');
        tocList.replaceChildren();
        if (!headings.length) {
          const li = document.createElement('li');
          li.textContent = getLanguage() === 'ko' ? '목차 없음' : 'No sections';
          tocList.append(li);
          return;
        }
        headings.forEach(h => {
          const li = document.createElement('li');
          const a = document.createElement('a');
          a.href = `#/guide/${activeGuideKey}#${h.id}`;
          a.textContent = h.textContent.replace('#', '').trim();
          if (h.tagName === 'H3') {
            li.style.paddingLeft = '14px';
            li.style.fontSize = '0.78rem';
          }
          a.addEventListener('click', (e) => {
            e.preventDefault();
            h.scrollIntoView({ behavior: scrollBehavior() });
            history.replaceState(null, '', `#/guide/${activeGuideKey}#${h.id}`);
          });
          li.append(a);
          tocList.append(li);
        });
      }

      function renderGuide(guideKey, targetAnchor) {
        const guide = guideData[guideKey];
        if (!guide) {
          renderHub('overview');
          return;
        }
        activeGuideKey = guideKey;
        const lang = getLanguage();

        hubArticle.hidden = true;
        guideArticle.hidden = false;

        guideBreadcrumbCategory.textContent = guide.category[lang] || guide.category.en;
        guideBreadcrumbTitle.textContent = guide.title[lang] || guide.title.en;
        guideTitle.textContent = guide.title[lang] || guide.title.en;
        document.title = `${guideTitle.textContent} · Afsm Docs`;
        guideLead.textContent = guide.lead[lang] || guide.lead.en;
        guideGithubLink.href = `https://github.com/kez-lab/afsm/blob/main/docs/${lang === 'ko' ? guide.github.replace('.md', '.ko.md') : guide.github}`;
        guideGithubLink.querySelector('span').textContent = lang === 'ko' ? 'GitHub에서 보기 ↗' : 'View on GitHub ↗';

        const rawMarkdown = (lang === 'ko' && guide.ko) ? guide.ko : guide.en;
        guideBody.innerHTML = renderMarkdownToHtml(rawMarkdown);

        guideBody.querySelectorAll('.copy-button').forEach(button => {
          button.addEventListener('click', async () => {
            const currentLang = getLanguage();
            const target = document.querySelector(`#${button.dataset.copyTarget}`);
            if (!target) return;
            try {
              await navigator.clipboard.writeText(target.textContent);
              const enSpan = button.querySelector('[data-lang="en"]');
              const koSpan = button.querySelector('[data-lang="ko"]');
              if (enSpan) enSpan.textContent = copyLabels.en.copied;
              if (koSpan) koSpan.textContent = copyLabels.ko.copied;
              window.setTimeout(() => {
                if (enSpan) enSpan.textContent = copyLabels.en.copy;
                if (koSpan) koSpan.textContent = copyLabels.ko.copy;
              }, 1200);
            } catch (_) {
              const selection = window.getSelection();
              const range = document.createRange();
              range.selectNodeContents(target);
              selection.removeAllRanges();
              selection.addRange(range);
            }
          });
        });

        guidePagination.replaceChildren();
        if (guide.prev && guideData[guide.prev]) {
          const prevGuide = guideData[guide.prev];
          const prevLink = document.createElement('a');
          prevLink.href = `#/guide/${guide.prev}`;
          prevLink.innerHTML = `<small>${lang === 'ko' ? '← 이전' : '← Previous'}</small><strong>${prevGuide.title[lang] || prevGuide.title.en}</strong>`;
          guidePagination.append(prevLink);
        } else {
          const overviewLink = document.createElement('a');
          overviewLink.href = '#overview';
          overviewLink.innerHTML = `<small>${lang === 'ko' ? '← 처음으로' : '← Overview'}</small><strong>${lang === 'ko' ? '개요로 돌아가기' : 'Back to Overview'}</strong>`;
          guidePagination.append(overviewLink);
        }

        if (guide.next && guideData[guide.next]) {
          const nextGuide = guideData[guide.next];
          const nextLink = document.createElement('a');
          nextLink.href = `#/guide/${guide.next}`;
          nextLink.innerHTML = `<small>${lang === 'ko' ? '다음 →' : 'Next →'}</small><strong>${nextGuide.title[lang] || nextGuide.title.en}</strong>`;
          guidePagination.append(nextLink);
        }

        updateGuideToc();

        navLinks.forEach(link => {
          const href = link.getAttribute('href');
          const isActive = href === `#/guide/${guideKey}`;
          link.classList.toggle('is-active', isActive);
          isActive ? link.setAttribute('aria-current', 'location') : link.removeAttribute('aria-current');
        });

        if (targetAnchor) {
          const targetEl = document.getElementById(targetAnchor);
          if (targetEl) {
            targetEl.scrollIntoView({ behavior: scrollBehavior() });
            return;
          }
        }
        window.scrollTo({ top: 0, behavior: 'instant' });
      }

      function renderHub(sectionId) {
        activeGuideKey = null;
        document.title = getLanguage() === 'ko' ? 'Afsm 시작하기 · Afsm Docs' : 'Getting started · Afsm Docs';
        guideArticle.hidden = true;
        hubArticle.hidden = false;

        if (tocList && hubTocHtml) {
          tocList.innerHTML = hubTocHtml;
          tocList.querySelectorAll('[data-toc-link]').forEach(link => {
            link.addEventListener('click', (e) => {
              const targetHref = link.getAttribute('href');
              if (targetHref && targetHref.startsWith('#')) {
                const target = document.querySelector(targetHref);
                if (target) {
                  e.preventDefault();
                  target.scrollIntoView({ behavior: scrollBehavior() });
                  history.replaceState(null, '', targetHref);
                }
              }
            });
          });
        }

        navLinks.forEach(link => {
          const href = link.getAttribute('href');
          const isActive = href === `#${sectionId}`;
          link.classList.toggle('is-active', isActive);
          isActive ? link.setAttribute('aria-current', 'location') : link.removeAttribute('aria-current');
        });

        if (sectionId && sectionId !== 'overview') {
          const target = document.getElementById(sectionId);
          if (target) {
            target.scrollIntoView({ behavior: scrollBehavior() });
            return;
          }
        }
        if (sectionId === 'overview') {
          window.scrollTo({ top: 0, behavior: 'instant' });
        }
      }

      function handleRoute() {
        const hash = window.location.hash || '#overview';
        if (hash.startsWith('#/guide/')) {
          const parts = hash.slice('#/guide/'.length).split('#');
          const guideKey = parts[0];
          let anchor = parts[1] || null;
          try { if (anchor) anchor = decodeURIComponent(anchor); } catch (_) { /* Malformed URLs fall back to the guide. */ }
          renderGuide(guideKey, anchor);
        } else {
          const sectionId = hash.replace(/^#/, '');
          renderHub(sectionId || 'overview');
        }
      }

      window.addEventListener('hashchange', handleRoute);

  // Trace lab bridge
  function selectExample(key, shouldScroll) {
    if (window.afsmTraceLab && window.afsmTraceLab.selectExample) {
      window.afsmTraceLab.selectExample(key, shouldScroll);
    }
  }

  function resetExampleLab() {
    if (window.afsmTraceLab && window.afsmTraceLab.resetExampleLab) {
      window.afsmTraceLab.resetExampleLab();
    }
  }

  if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver(entries => {
          const visible = entries
            .filter(entry => entry.isIntersecting)
            .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0];
          if (!visible || activeGuideKey) return;
          const id = visible.target.id;
          navLinks.forEach(link => {
            const active = link.getAttribute('href') === `#${id}`;
            link.classList.toggle('is-active', active);
            active ? link.setAttribute('aria-current', 'location') : link.removeAttribute('aria-current');
          });
          document.querySelectorAll('[data-toc-link]').forEach(link => link.classList.toggle('is-active', link.getAttribute('href') === `#${id}`));
        }, { rootMargin: '-20% 0px -65% 0px', threshold: 0 });
        observedSections.forEach(section => observer.observe(section));
      }

      let savedLanguage = null;
      try {
        savedLanguage = localStorage.getItem('afsm-docs-language');
      } catch (_) {
        // Ignore unavailable storage.
      }
      const initialLanguage = savedLanguage === 'ko' || savedLanguage === 'en'
        ? savedLanguage
        : (navigator.language.toLowerCase().startsWith('ko') ? 'ko' : 'en');
      setLanguage(initialLanguage);
      handleRoute();
      selectExample('draft', false);
    })();
})();
