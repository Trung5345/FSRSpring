// router.js
// A lightweight PJAX router to prevent the entire page (especially the sidebar) from reloading.

let isNavigating = false;

window.onAppLoad = function(callback) {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', callback);
    } else {
        // If script is loaded dynamically after initial load
        setTimeout(callback, 0);
    }
    document.addEventListener('app:routed', callback);
};

document.addEventListener('click', async (e) => {
    const link = e.target.closest('a');
    if (!link || !link.href) return;
    
    const url = new URL(link.href);
    if (url.origin !== window.location.origin) return; 
    
    // Bypass constraints
    if (link.target === '_blank' || link.hasAttribute('data-no-router')) return;
    if (url.pathname.includes('/api/') || url.pathname.includes('/oauth2/')) return;
    
    if (url.pathname === window.location.pathname) {
        e.preventDefault();
        return;
    }

    e.preventDefault();
    if (isNavigating) return;
    isNavigating = true;
    
    // Add a simple top progress bar
    showProgressBar();

    await navigateTo(url.href, true);
    
    hideProgressBar();
    isNavigating = false;
});

window.addEventListener('popstate', () => {
    navigateTo(window.location.href, false);
});

async function navigateTo(url, pushState = true) {
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error('Network error');
        const html = await response.text();
        
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');

        document.title = doc.title;

        // Replace main and header
        const main = document.querySelector('main');
        const newMain = doc.querySelector('main');
        if (main && newMain) main.innerHTML = newMain.innerHTML;

        const header = document.querySelector('header');
        const newHeader = doc.querySelector('header');
        if (header && newHeader) {
            // Only update the page title in the header to preserve streak/xp state
            const oldTitle = header.querySelector('div.font-headline-md');
            const newTitle = newHeader.querySelector('div.font-headline-md');
            if (oldTitle && newTitle) {
                oldTitle.textContent = newTitle.textContent;
            }
        }

        // Replace any modals (elements in body but outside nav/header/main/script)
        replaceModals(doc.body);

        // Update active class on nav
        updateSidebar(url);syncStyles(doc);

        if (pushState) {
            window.history.pushState(null, '', url);
        }

        // Handle Scripts (load new scripts)
        await loadNewScripts(doc);

        // Inform application that routing is complete
        document.dispatchEvent(new Event('app:routed'));
        if (window.tailwind && window.tailwind.refresh) {
            window.tailwind.refresh();
        }
        window.scrollTo(0, 0);

    } catch (error) {
        console.error('Router error:', error);
        window.location.href = url; // Fallback
    }
}

function replaceModals(newBody) {
    const ignoredTags = ['nav', 'header', 'main', 'script'];
    
    // Remove old modals
    Array.from(document.body.children).forEach(child => {
        if (!ignoredTags.includes(child.tagName.toLowerCase())) {
            child.remove();
        }
    });

    // Insert new modals
    Array.from(newBody.children).forEach(child => {
        if (!ignoredTags.includes(child.tagName.toLowerCase())) {
            document.body.insertBefore(child.cloneNode(true), document.body.querySelector('script'));
        }
    });
}

function updateSidebar(url) {
    const path = new URL(url).pathname;
    document.querySelectorAll('nav ul a').forEach(a => {
        const linkPath = new URL(a.href).pathname;
        if (linkPath === path) {
            a.className = "flex items-center gap-4 px-4 py-3 bg-primary-fixed text-primary border-2 border-primary rounded-xl font-label-lg text-label-lg uppercase tracking-wider font-bold";
            const icon = a.querySelector('.material-symbols-outlined');
            if (icon) icon.classList.add('icon-filled');
        } else {
            a.className = "flex items-center gap-4 px-4 py-3 text-on-surface-variant hover:bg-surface-container rounded-xl transition-colors font-label-lg text-label-lg uppercase tracking-wider font-bold";
            const icon = a.querySelector('.material-symbols-outlined');
            if (icon) icon.classList.remove('icon-filled');
        }
    });
    // Update profile link active state
    const profileLink = document.querySelector('nav #userMenuContainer a[href="/profile"]');
    if (profileLink) {
        if (path === '/profile') {
            profileLink.className = 'flex items-center gap-3 px-4 py-3 bg-primary-fixed text-primary border-2 border-primary rounded-xl transition-colors w-full text-left font-label-lg group';
        } else {
            profileLink.className = 'flex items-center gap-3 px-4 py-3 bg-surface-container-lowest text-on-surface hover:bg-surface-container border-2 border-transparent hover:border-outline-variant rounded-xl transition-colors w-full text-left font-label-lg group';
        }
    }
}

async function loadNewScripts(doc) {
    const existingScripts = Array.from(document.querySelectorAll('script')).map(s => s.src || s.innerHTML);
    const scriptPromises = [];

    doc.querySelectorAll('script').forEach(script => {
        const isTailwind = script.src && script.src.includes('tailwindcss');
        const isTailwindConfig = script.innerHTML && script.innerHTML.includes('tailwind.config');
        const scriptContent = script.src || script.innerHTML;
        
        if (!isTailwind && !isTailwindConfig && !existingScripts.includes(scriptContent)) {
            const newScript = document.createElement('script');
            if (script.src) {
                newScript.src = script.src;
                scriptPromises.push(new Promise((resolve) => {
                    newScript.onload = resolve;
                    newScript.onerror = resolve; // Continue on error
                    document.body.appendChild(newScript);
                }));
            } else {
                newScript.innerHTML = script.innerHTML;
                document.body.appendChild(newScript);
            }
        }
    });

    await Promise.all(scriptPromises);
}

// Simple progress bar
let progressBar;
function showProgressBar() {
    if (!progressBar) {
        progressBar = document.createElement('div');
        progressBar.className = 'fixed top-0 left-0 h-1 bg-primary z-[9999] transition-all duration-300';
        progressBar.style.width = '0%';
        document.body.appendChild(progressBar);
    }
    progressBar.style.width = '30%';
    progressBar.style.opacity = '1';
}
function hideProgressBar() {
    if (progressBar) {
        progressBar.style.width = '100%';
        setTimeout(() => {
            progressBar.style.opacity = '0';
            setTimeout(() => {
                progressBar.style.width = '0%';
            }, 300);
        }, 300);
    }
}

async function loadCurrentUser() {
    try {
        const res = await fetch('/api/user/me');
        if (res.ok) {
            const user = await res.json();
            const nameEl = document.getElementById('userName');
            const emailEl = document.getElementById('userEmail');
            const avatarEl = document.getElementById('userAvatar');
            const initialEl = document.getElementById('userInitial');

            if (nameEl) nameEl.textContent = user.name || 'Student';
            if (emailEl) emailEl.textContent = user.email || '';
            
            if (avatarEl && user.avatarUrl) {
                avatarEl.src = user.avatarUrl;
                avatarEl.classList.remove('hidden');
                if (initialEl) initialEl.classList.add('hidden');
            }
        }
    } catch (e) {
        console.error('Failed to load user info', e);
    }
}

// Auto-run on every page load and PJAX route
window.onAppLoad(() => {
    loadCurrentUser();
});

async function loadUserMenu() {
    const container = document.getElementById('userMenuContainer');
    if (!container) return;
    container.innerHTML = `
        <div class="flex items-center gap-3 px-4 py-3 text-on-surface font-label-lg">
          <div class="w-8 h-8 rounded-full bg-primary-fixed flex items-center justify-center border-2 border-primary overflow-hidden">
            <span class="material-symbols-outlined icon-filled text-primary text-sm block">person</span>
          </div>
          <div class="flex-1 min-w-0 font-bold text-on-surface uppercase tracking-wider">Loading...</div>
        </div>
    `;
    try {
        const res = await fetch('/api/user/me');
        if (res.ok) {
            const user = await res.json();
            const path = window.location.pathname;
            const isProfile = path === '/profile';
            const linkClass = isProfile
                ? 'flex items-center gap-3 px-4 py-3 bg-primary-fixed text-primary border-2 border-primary rounded-xl transition-colors w-full text-left font-label-lg group'
                : 'flex items-center gap-3 px-4 py-3 bg-surface-container-lowest text-on-surface hover:bg-surface-container border-2 border-transparent hover:border-outline-variant rounded-xl transition-colors w-full text-left font-label-lg group';
            const avatarHtml = user.avatarUrl
                ? `<img src="${user.avatarUrl}" alt="Avatar" class="w-full h-full object-cover">`
                : `<span class="material-symbols-outlined icon-filled text-primary text-sm block">person</span>`;
            container.innerHTML = `
                <a href="/profile" class="${linkClass}">
                  <div class="w-8 h-8 rounded-full bg-primary-fixed flex items-center justify-center border-2 border-primary overflow-hidden">
                    ${avatarHtml}
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="truncate font-bold text-on-surface leading-tight">${user.name || 'Student'}</div>
                    <div class="truncate text-xs text-on-surface-variant font-normal leading-tight">${user.email || ''}</div>
                  </div>
                </a>
                <button onclick="window.location.href='/logout'" class="flex items-center justify-center gap-2 px-4 py-2 text-error hover:bg-error-container rounded-xl transition-colors font-label-lg font-bold w-full uppercase tracking-wider mt-2 border-2 border-transparent hover:border-error">
                  <span class="material-symbols-outlined">logout</span>Logout
                </button>
            `;
        } else {
            container.innerHTML = `
                <a href="/oauth2/authorization/google" class="flex items-center gap-3 px-4 py-3 bg-primary text-on-primary hover:opacity-90 rounded-xl transition-colors w-full text-left font-label-lg border-2 border-primary">
                  <div class="w-8 h-8 rounded-full bg-on-primary flex items-center justify-center overflow-hidden">
                    <span class="material-symbols-outlined text-primary text-sm block">account_circle</span>
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="truncate font-bold uppercase tracking-wider">Login</div>
                  </div>
                </a>
            `;
        }
    } catch (e) {
        console.error('Failed to load user menu', e);
    }
}

window.onAppLoad(loadUserMenu);

// Sync styles between pages
function syncStyles(newDoc) {
    // Remove old pjax css/styles
    document.querySelectorAll('[data-pjax-style]').forEach(el => el.remove());
    
    // Apply new styles and links
    newDoc.querySelectorAll('head style, head link[rel="stylesheet"]').forEach(el => {
        if (el.tagName.toLowerCase() === 'link') {
            const existingLink = document.querySelector(`head link[href="${el.href}"]`);
            if (existingLink) return; // Keep existing to avoid flicker
        } else if (el.tagName.toLowerCase() === 'style') {
            // Check if exact style exists to avoid duplicating inline styles if possible
            const existingStyle = Array.from(document.querySelectorAll('head style:not([data-pjax-style])'))
                .find(s => s.innerHTML === el.innerHTML);
            if (existingStyle) return;
        }

        const newEl = document.createElement(el.tagName);
        if (el.tagName.toLowerCase() === 'style') {
            newEl.innerHTML = el.innerHTML;
        } else {
            newEl.href = el.href;
            newEl.rel = el.rel;
        }
        newEl.setAttribute('data-pjax-style', 'true');
        document.head.appendChild(newEl);
    });
}
