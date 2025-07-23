// HTMX Configuration
htmx.config.globalViewTransitions = true;

// Show loading states
document.addEventListener('htmx:beforeRequest', function (evt) {
    const target = evt.target;
    if (target.tagName === 'WA-BUTTON') {
        target.loading = true;
    }
});

document.addEventListener('htmx:afterRequest', function (evt) {
    const target = evt.target;
    if (target.tagName === 'WA-BUTTON') {
        target.loading = false;
    }
});

// Auto-hide success alerts after 5 seconds
setTimeout(function () {
    const alerts = document.querySelectorAll('wa-alert[variant="success"]');
    alerts.forEach(function (alert) {
        alert.style.transition = 'opacity 0.5s';
        alert.style.opacity = '0';
        setTimeout(() => alert.remove(), 500);
    });
}, 5000);

// Handle navigation
document.addEventListener('DOMContentLoaded', function () {
    // Close drawer on navigation link click (mobile)
    const navigationLinks = document.querySelectorAll("[slot='navigation'] a[href]:not([href='#'])");
    navigationLinks.forEach(link => {
        link.addEventListener('click', function () {
            // Close mobile navigation drawer
            const page = document.querySelector('wa-page');
            if (page && page.getAttribute('view') === 'mobile') {
                page.setAttribute('drawer', 'closed');
            }
        });
    });

    // Handle section anchors
    const sectionAnchors = document.querySelectorAll("[slot*='navigation'] a[href*='#']");
    sectionAnchors.forEach(sectionAnchor => sectionAnchor.setAttribute('data-drawer', 'close'));

    document.addEventListener("keydown", handleShortcutEvent);
});

// Handle aside visibility based on content
document.addEventListener('DOMContentLoaded', function () {
    const asideContent = document.getElementById('aside-content');
    const page = document.querySelector('wa-page');

    function updateAsideVisibility() {
        if (!asideContent || !page) return;

        const hasContent = asideContent.textContent.trim().length > 0 || asideContent.children.length > 0;

        if (hasContent) {
            page.setAttribute('data-aside-visible', 'true');
        } else {
            page.setAttribute('data-aside-visible', 'false');
        }
    }

    // Check on load
    updateAsideVisibility();

    // Check after HTMX requests
    document.addEventListener('htmx:afterSettle', updateAsideVisibility);
});

// Handle clickable callouts
document.addEventListener('click', function(e) {
    const callout = e.target.closest('wa-callout[data-link]');
    if (callout) {
        const link = callout.getAttribute('data-link');
        if (link) {
            window.location.href = link;
        }
    }
});

function handleShortcutEvent(event) {
    if (shortcutAction && shortcutAction.length > 0) {
        const combo = normalizeKeyCombination(event);

        const matchedShortcut = shortcutAction.find(
            s => s.keyCombination.toLowerCase() === combo
        );

        if (matchedShortcut) {
            event.preventDefault();
            const action = shortcutMap[matchedShortcut.actionId];
            if (action) {
                action();
            } else {
                console.warn("No handler for actionId:", matchedShortcut.actionId);
            }
        }
    }
}

function normalizeKeyCombination(event) {
    const keys = [];
    if (event.ctrlKey) keys.push("ctrl");
    if (event.altKey) keys.push("alt");
    if (event.shiftKey) keys.push("shift");
    const key = event.key.toLowerCase();
    keys.push(key);
    return keys.join("+");
}

document.addEventListener('htmx:configRequest', e => {
    const spinner = document.getElementById('loading-indicator');
    if (spinner) {
        spinner.style.display = 'inline-block';
    }
});

document.addEventListener('htmx:afterRequest', e => {
    const spinner = document.getElementById('loading-indicator');
    if (spinner) {
        spinner.style.display = 'none';
    }
});
