const { JSDOM } = require("jsdom");
const html = `<!DOCTYPE html><html><head>
<script src="https://cdn.tailwindcss.com"></script>
<script>tailwind.config = { theme: { extend: {} } }</script>
</head><body><div class="bg-red-500">test</div></body></html>`;
const dom = new JSDOM(html, { runScripts: "dangerously", resources: "usable" });
dom.window.addEventListener("error", (event) => console.error("DOM Error:", event.error));
setTimeout(() => {
  const styles = dom.window.document.querySelectorAll('style');
  console.log("Styles count:", styles.length);
  styles.forEach(s => console.log('Style:', s.innerHTML.substring(0, 100)));
}, 2000);
