const { JSDOM } = require("jsdom");
JSDOM.fromURL("https://play.tailwindcss.com/", { runScripts: "dangerously", resources: "usable" }).then(dom => {
  setTimeout(() => {
    const styles = dom.window.document.querySelectorAll('style');
    styles.forEach(s => console.log('Style parent:', s.parentNode.tagName, 'ID:', s.id));
  }, 2000);
});
