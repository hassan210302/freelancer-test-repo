const VAT_CODES_KEY = 'r-vat-codes';
const VAT_CODES_TS_KEY = 'r-vat-codes-ts';

window.getVatCodes = async function() {
    const cached = getCachedData(VAT_CODES_KEY, VAT_CODES_TS_KEY);
    if (cached) {
        return cached;
    }
    
    const response = await fetch('/api/vat-codes');
    const data = await response.json();
    setCachedData(VAT_CODES_KEY, VAT_CODES_TS_KEY, data);
    return data;
};
