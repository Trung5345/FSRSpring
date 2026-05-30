function replaceModals(newBody) {
    const ignoredTags = ['nav', 'header', 'main', 'script'];
    Array.from(document.body.children).forEach(child => {
        if (!ignoredTags.includes(child.tagName.toLowerCase())) {
            console.log("Removing:", child.tagName);
        }
    });
}
