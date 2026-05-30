const { JSDOM } = require("jsdom");
const fs = require("fs");
const html = fs.readFileSync("src/main/resources/static/index.html", "utf8");
const dom = new JSDOM(html, { runScripts: "dangerously", resources: "usable" });
dom.window.addEventListener("error", (event) => {
  console.error("DOM Error:", event.error);
});
setTimeout(() => {
  const styles = dom.window.document.querySelectorAll('style');
  console.log("Styles count:", styles.length);
  styles.forEach(s => console.log('Style ID:', s.id, s.innerHTML.substring(0, 50)));
}, 3000);
