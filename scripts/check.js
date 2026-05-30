const jsjs = require('jsdom');
const { JSDOM } = jsjs;
const dom = new JSDOM(`<!DOCTYPE html><html lang="en"><head><script src="https://cdn.tailwindcss.com"></script><script>tailwind.config = {theme:{extend:{fontFamily:{"test":["Lexend"]}}}}</script></head><body><h1 class="font-test">Hello</h1></body></html>`, { runScripts: "dangerously", resources: "usable" });
dom.window.addEventListener('load', () => { setTimeout(() => console.log(dom.window.document.head.innerHTML), 1000); });
