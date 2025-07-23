const ACCOUNTS_KEY = 'r-accounts';
const ACCOUNTS_TS_KEY = 'r-accounts-ts';

window.getAccounts = async function() {
    const cached = getCachedData(ACCOUNTS_KEY, ACCOUNTS_TS_KEY);
    if (cached) {
        return cached;
    }
    
    const response = await fetch('/api/accounts');
    const data = await response.json();
    setCachedData(ACCOUNTS_KEY, ACCOUNTS_TS_KEY, data);
    return data;
};
