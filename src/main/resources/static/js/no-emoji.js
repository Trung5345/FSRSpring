// no-emoji.js - Remove emojis from the whole site (DOM + dynamic updates)

(function () {
    'use strict';

    function buildEmojiRegex() {
        // Prefer Unicode property escapes when available (modern browsers)
        try {
            // Extended_Pictographic covers most emoji; Regional_Indicator covers flags.
            // Also remove VS16 (FE0F) and ZWJ (200D) used in emoji sequences.
            return new RegExp('[\\p{Extended_Pictographic}\\p{Regional_Indicator}\\uFE0F\\u200D]+', 'gu');
        } catch (_) {
            // Fallback range-based regex (still requires the /u flag)
            return /[\u{1F1E6}-\u{1F1FF}\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}\uFE0F\u200D]+/gu;
        }
    }

    const EMOJI_RE = buildEmojiRegex();

    function stripEmojis(value) {
        if (value == null) return value;
        return String(value).replace(EMOJI_RE, '');
    }

    function sanitizeTextNode(textNode) {
        const before = textNode.nodeValue;
        const after = stripEmojis(before);
        if (before !== after) textNode.nodeValue = after;
    }

    function sanitizeElementAttributes(el) {
        // Keep this minimal; only common user-visible attributes.
        const attrs = ['placeholder', 'title', 'aria-label'];
        for (const name of attrs) {
            const v = el.getAttribute && el.getAttribute(name);
            if (v) {
                const after = stripEmojis(v);
                if (after !== v) el.setAttribute(name, after);
            }
        }
    }

    function sanitizeSubtree(root) {
        if (!root) return;

        if (root.nodeType === Node.TEXT_NODE) {
            sanitizeTextNode(root);
            return;
        }

        if (root.nodeType !== Node.ELEMENT_NODE && root.nodeType !== Node.DOCUMENT_FRAGMENT_NODE) return;

        const walker = document.createTreeWalker(
            root,
            NodeFilter.SHOW_TEXT,
            {
                acceptNode: (node) => {
                    const parent = node.parentNode;
                    if (!parent || parent.nodeType !== Node.ELEMENT_NODE) return NodeFilter.FILTER_ACCEPT;

                    const tag = parent.tagName;
                    // Don’t touch scripts/styles; skip textarea content.
                    if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'TEXTAREA') return NodeFilter.FILTER_REJECT;

                    return NodeFilter.FILTER_ACCEPT;
                }
            }
        );

        let textNode;
        // eslint-disable-next-line no-cond-assign
        while (textNode = walker.nextNode()) {
            sanitizeTextNode(textNode);
        }

        if (root.nodeType === Node.ELEMENT_NODE) {
            sanitizeElementAttributes(root);
            // sanitize attributes for all descendants too
            const descendants = root.querySelectorAll ? root.querySelectorAll('[placeholder],[title],[aria-label]') : [];
            for (const el of descendants) sanitizeElementAttributes(el);
        }
    }

    function start() {
        sanitizeSubtree(document.body);

        const observer = new MutationObserver((mutations) => {
            for (const m of mutations) {
                if (m.type === 'characterData') {
                    sanitizeTextNode(m.target);
                    continue;
                }

                if (m.addedNodes && m.addedNodes.length) {
                    for (const n of m.addedNodes) sanitizeSubtree(n);
                }

                // Attributes we care about
                if (m.type === 'attributes' && m.target && m.target.nodeType === Node.ELEMENT_NODE) {
                    sanitizeElementAttributes(m.target);
                }
            }
        });

        observer.observe(document.body, {
            subtree: true,
            childList: true,
            characterData: true,
            attributes: true,
            attributeFilter: ['placeholder', 'title', 'aria-label']
        });

        // Optional: expose helper for app code if needed
        window.stripEmojis = stripEmojis;
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
