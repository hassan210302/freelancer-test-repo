const CACHE_WEEK_MS = 7 * 24 * 60 * 60 * 1000;

function isCacheValid(timestamp) {
    return Date.now() - timestamp < CACHE_WEEK_MS;
}

function getCachedData(key, timestampKey) {
    const cached = localStorage.getItem(key);
    const ts = parseInt(localStorage.getItem(timestampKey) || '0', 10);
    if (cached && isCacheValid(ts)) {
        try {
            return JSON.parse(cached);
        } catch (_) {
            localStorage.removeItem(key);
            localStorage.removeItem(timestampKey);
        }
    }
    return null;
}

function setCachedData(key, timestampKey, data) {
    localStorage.setItem(key, JSON.stringify(data));
    localStorage.setItem(timestampKey, Date.now().toString());
    const event = new CustomEvent('vat-codes-cached', {
        detail: {
            key,
            timestampKey,
            data
        }
    });
    window.dispatchEvent(event);
} 