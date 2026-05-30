const fs = require('fs');
const file = 'src/main/resources/static/js/router.js';
let content = fs.readFileSync(file, 'utf8');

content = content.replace(
    'a.className = "flex items-center gap-4 px-4 py-3 bg-primary-fixed text-primary border-2 border-primary rounded-xl font-label-lg text-label-lg uppercase tracking-wider";',
    'a.className = "flex items-center gap-4 px-4 py-3 bg-primary-fixed text-primary border-2 border-primary rounded-xl font-label-lg text-label-lg uppercase tracking-wider font-bold";'
);
content = content.replace(
    'a.className = "flex items-center gap-4 px-4 py-3 text-on-surface-variant hover:bg-surface-container rounded-xl transition-colors font-label-lg text-label-lg uppercase tracking-wider";',
    'a.className = "flex items-center gap-4 px-4 py-3 text-on-surface-variant hover:bg-surface-container rounded-xl transition-colors font-label-lg text-label-lg uppercase tracking-wider font-bold";'
);

fs.writeFileSync(file, content);
console.log('Sidebar Patched!');
