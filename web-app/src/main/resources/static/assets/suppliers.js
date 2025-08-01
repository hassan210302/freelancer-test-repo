const SUPPLIERS_KEY = 'r-suppliers';
const SUPPLIERS_TS_KEY = 'r-suppliers-ts';

window.getSuppliers = async function() {
    const cached = getCachedData(SUPPLIERS_KEY, SUPPLIERS_TS_KEY);
    if (cached) {
        return cached;
    }
    
    const response = await fetch('/api/suppliers');
    const data = await response.json();
    setCachedData(SUPPLIERS_KEY, SUPPLIERS_TS_KEY, data);
    return data;
};
