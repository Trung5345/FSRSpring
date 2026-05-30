const fs = require('fs');
const file = 'src/main/resources/static/js/router.js';
let content = fs.readFileSync(file, 'utf8');

// 1. Fix loadNewScripts to ignore tailwind.config
content = content.replace(
    "const isTailwind = script.src && script.src.includes('tailwindcss');",
    "const isTailwind = script.src && script.src.includes('tailwindcss');\n        const isTailwindConfig = script.innerHTML && script.innerHTML.includes('tailwind.config');"
);
content = content.replace(
    "if (!isTailwind && !existingScripts.includes(scriptContent)) {",
    "if (!isTailwind && !isTailwindConfig && !existingScripts.includes(scriptContent)) {"
);

// 2. Fix syncStyles to also handle links safely
const oldSyncStyles = `function syncStyles(newDoc) {
    // Remove old pjax styles
    document.querySelectorAll('style[data-pjax-style]').forEach(el => el.remove());
    
    // Apply new styles
    const styles = newDoc.querySelectorAll('head style');
    styles.forEach(style => {
        const newStyle = document.createElement('style');
        newStyle.innerHTML = style.innerHTML;
        newStyle.setAttribute('data-pjax-style', 'true');
        document.head.appendChild(newStyle);
    });
}`;

const newSyncStyles = `function syncStyles(newDoc) {
    // Remove old pjax css/styles
    document.querySelectorAll('[data-pjax-style]').forEach(el => el.remove());
    
    // Apply new styles and links
    newDoc.querySelectorAll('head style, head link[rel="stylesheet"]').forEach(el => {
        if (el.tagName.toLowerCase() === 'link') {
            const existingLink = document.querySelector(\`head link[href="\${el.href}"]\`);
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
}`;

content = content.replace(oldSyncStyles, newSyncStyles);

fs.writeFileSync(file, content);
console.log('Patched!');
