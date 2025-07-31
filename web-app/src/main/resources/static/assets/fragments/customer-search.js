document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('customer-search-search-input');
    const dropdown = document.getElementById('customer-search-dropdown');
    const container = input?.closest('div');
    if (!input || !dropdown || !container) return;

    let lastSelectedCustomer = { id: '', name: '' };
    let lastDispatchedCustomer = { id: '', name: '' };

    const dropdownHasResults = () => dropdown.querySelector('.dropdown-item') !== null;

    const dispatchSelection = (id, name) => {
        if (id === lastDispatchedCustomer.id && name === lastDispatchedCustomer.name) return;
        lastDispatchedCustomer = { id, name };
        input.dispatchEvent(new CustomEvent('customerSelected', {
            detail: { id, name },
            bubbles: true
        }));
    };

    const handleDropdownClose = () => {
        dropdown.style.display = 'none';
        const currentInput = input.value?.trim();

        if (!lastSelectedCustomer.id && !dropdownHasResults()) {
            input.value = '';
            lastSelectedCustomer = { id: '', name: '' };
            dispatchSelection('', '');
            return;
        }

        if (currentInput !== lastSelectedCustomer.name) {
            if (lastSelectedCustomer.id) {
                input.value = lastSelectedCustomer.name;
                dispatchSelection(lastSelectedCustomer.id, lastSelectedCustomer.name);
            } else {
                input.value = '';
                lastSelectedCustomer = { id: '', name: '' };
                dispatchSelection('', '');
            }
            return;
        }

        dispatchSelection(lastSelectedCustomer.id, lastSelectedCustomer.name);
    };

    document.body.addEventListener('htmx:afterSwap', e => {
        if (e.target.id === 'customer-search-dropdown') {
            dropdown.style.display = 'block';
        }
    });

    document.addEventListener('click', e => {
        if (!container.contains(e.target)) handleDropdownClose();
    });

    input.addEventListener('keydown', e => {
        if (e.key === 'Escape') handleDropdownClose();
    });

    dropdown.addEventListener('click', e => {
        const item = e.target.closest('.dropdown-item');
        if (!item) return;
        const { customerId: id, customerName: name } = item.dataset;
        input.value = name;
        lastSelectedCustomer = { id, name };
        dropdown.style.display = 'none';
        dispatchSelection(id, name);
    });

    input.addEventListener('input', () => {
        if (input.value.trim() === '') {
            lastSelectedCustomer = { id: '', name: '' };
            dispatchSelection('', '');
        }
    });
});