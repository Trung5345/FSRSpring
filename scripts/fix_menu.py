import os
import glob
import re

html_files = glob.glob('src/main/resources/static/*.html')

for file in html_files:
    with open(file, 'r') as f:
        content = f.read()
    
    # Remove the bad script completely
    content = re.sub(r'<script>\s*async function loadUserMenu\(\).*?</script>', '', content, flags=re.DOTALL)
    
    # Check if there is an empty userMenuContainer
    # Replace it with static HTML that app.js can populate
    
    static_html = """  <!-- User Profile Menu -->
  <div class="mt-auto flex flex-col pt-6 border-t-2 border-outline-variant" id="userMenuContainer">
    <a href="/profile" class="flex items-center gap-3 px-4 py-3 text-on-surface hover:bg-surface-container rounded-xl transition-colors w-full text-left font-label-lg group border-2 border-transparent hover:border-outline-variant">
        <div class="w-8 h-8 rounded-full bg-primary-fixed flex items-center justify-center border-2 border-primary overflow-hidden">
          <img id="userAvatar" src="" alt="Avatar" class="w-full h-full object-cover hidden">
          <span id="userInitial" class="material-symbols-outlined icon-filled text-primary text-sm block">person</span>
        </div>
        <div class="flex-1 min-w-0">
          <div id="userName" class="truncate font-bold text-on-surface leading-tight">Student</div>
          <div id="userEmail" class="truncate text-xs text-on-surface-variant font-normal leading-tight">...</div>
        </div>
    </a>
    <button onclick="window.location.href='/logout'" class="flex items-center justify-center gap-2 px-4 py-2 text-error hover:bg-error-container rounded-xl transition-colors font-label-lg font-bold w-full uppercase tracking-wider mt-2 border-2 border-transparent hover:border-error">
        <span class="material-symbols-outlined">logout</span>Logout
    </button>
  </div>"""

    content = re.sub(r' *<!-- User Profile Menu -->\s*<div class="mt-auto flex flex-col pt-6 border-t-2 border-outline-variant" id="userMenuContainer">\s*<!-- Loaded dynamically -->\s*</div>', static_html, content, flags=re.DOTALL)
    
    with open(file, 'w') as f:
        f.write(content)

