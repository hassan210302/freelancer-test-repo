document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('customer-search-search-input');
    const dropdown = document.getElementById('customer-search-dropdown');
    const container = input?.closest('div');
    if (!input || !dropdown || !container) return;
    let lastSelectedCustomer = { id: '', name: '' };
    let lastDispatchedCustomer = { id: '', name: '' };
    const dropdownHasResults = () => dropdown.querySelectorAll('.dropdown-item').length > 0;

    const dispatchSelection = (id, name) => {
        if (id === lastDispatchedCustomer.id && name === lastDispatchedCustomer.name) return;
        input.dispatchEvent(new CustomEvent('customerSelected', {
            detail: { id, name },
            bubbles: true,
        }));
        lastDispatchedCustomer = { id, name };
    };

    const handleDropdownClose = () => {
        dropdown.style.display = 'none';
        const currentInput = input?.value?.trim();

        if (!lastSelectedCustomer.id && !dropdownHasResults()) {
            input.value = '';
            dispatchSelection('', '');
            lastSelectedCustomer = { id: '', name: '' };
            return;
        }

        if (currentInput !== lastSelectedCustomer.name) {
            if (lastSelectedCustomer.id) {
                input.value = lastSelectedCustomer.name;
                dispatchSelection(lastSelectedCustomer.id, lastSelectedCustomer.name);
            } else {
                input.value = '';
                dispatchSelection('', '');
                lastSelectedCustomer = { id: '', name: '' };
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
        if (!container.contains(e.target)) {
            handleDropdownClose();
        }
    });

    input.addEventListener('keydown', e => {
        if (e.key === 'Escape') {
            handleDropdownClose();
        }
    });

    dropdown.addEventListener('click', e => {
        const item = e.target.closest('.dropdown-item');
        if (!item) return;
        const id = item.dataset.customerId;
        const name = item.dataset.customerName;
        input.value = name;
        lastSelectedCustomer = { id, name };
        dispatchSelection(id, name);
        dropdown.style.display = 'none';
    });

    input.addEventListener('input', () => {
        if (input.value.trim() === '') {
            lastSelectedCustomer = { id: '', name: '' };
            dispatchSelection('', '');
        }
    });
});
